package com.ygc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ygc.model.*;
import com.ygc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates saving a complete chit snapshot to chit_history and
 * optionally attaching the analysis PDF path.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChitHistoryService {

    private final ChitHistoryRepository chitHistoryRepository;
    private final ChitMembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final AuctionRepository auctionRepository;
    private final CommissionLedgerRepository commissionLedgerRepository;
    private final SettlementRepository settlementRepository;
    private final ReportExportService reportExportService;

    private static final ObjectMapper MAPPER = buildMapper();

    private static ObjectMapper buildMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        m.enable(SerializationFeature.INDENT_OUTPUT);
        return m;
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Build a full JSON snapshot of the chit, generate an analysis PDF,
     * persist both to chit_history, and return the saved entity.
     *
     * Call this BEFORE deleting or hard-closing a chit.
     */
    @Transactional
    public ChitHistory archiveChit(Chit chit, String finalStatus, String reason, User closedBy) {
        ChitHistory history = new ChitHistory();
        history.setOriginalChitId(chit.getId());
        history.setChitName(chit.getName());
        history.setFinalStatus(finalStatus);
        history.setClosingReason(reason);
        history.setClosedByEmail(closedBy.getEmail());
        history.setClosedByName(closedBy.getFullName());
        history.setClosedAt(LocalDateTime.now());

        // ── Build JSON snapshot ──────────────────────────────────────────
        String json = buildSnapshot(chit);
        history.setCompleteDataJson(json);

        // ── Generate Analysis PDF ────────────────────────────────────────
        try {
            byte[] pdfBytes = reportExportService.generateChitAnalysisPdf(chit, json);
            String pdfPath = savePdfToTemp(pdfBytes, chit.getName(), chit.getId());
            history.setAnalysisPdfPath(pdfPath);
        } catch (Exception e) {
            log.warn("Analysis PDF generation failed for chit {} — continuing without PDF: {}",
                    chit.getId(), e.getMessage());
        }

        return chitHistoryRepository.save(history);
    }

    /** Returns the byte[] of the analysis PDF for a saved history entry. */
    public byte[] getPdfBytesForHistory(Long historyId) throws Exception {
        ChitHistory h = chitHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("History entry not found: " + historyId));
        if (h.getAnalysisPdfPath() == null) {
            // Regenerate on the fly from stored JSON
            return reportExportService.generateChitAnalysisPdfFromJson(h.getChitName(), h.getCompleteDataJson());
        }
        java.nio.file.Path path = java.nio.file.Paths.get(h.getAnalysisPdfPath());
        if (java.nio.file.Files.exists(path)) {
            return java.nio.file.Files.readAllBytes(path);
        }
        // File missing — regenerate
        return reportExportService.generateChitAnalysisPdfFromJson(h.getChitName(), h.getCompleteDataJson());
    }

    /** All entries, newest first. */
    public List<ChitHistory> findAll() {
        return chitHistoryRepository.findAllByOrderByClosedAtDesc();
    }

    public Optional<ChitHistory> findById(Long id) {
        return chitHistoryRepository.findById(id);
    }

    // ── JSON Snapshot Builder ──────────────────────────────────────────────

    private String buildSnapshot(Chit chit) {
        try {
            List<ChitMembership> memberships = membershipRepository.findByChit(chit);

            // Payments across all memberships
            List<Map<String, Object>> payments = new ArrayList<>();
            BigDecimal totalCollected = BigDecimal.ZERO;
            BigDecimal totalLateFines  = BigDecimal.ZERO;
            for (ChitMembership m : memberships) {
                for (Payment p : paymentRepository.findByMembership(m)) {
                    Map<String, Object> pm = new LinkedHashMap<>();
                    pm.put("paymentId",   p.getId());
                    pm.put("memberName",  m.getUser().getFullName());
                    pm.put("memberEmail", m.getUser().getEmail());
                    pm.put("monthNumber", p.getMonthNumber());
                    pm.put("amount",      p.getAmount());
                    pm.put("lateFine",    p.getLateFine());
                    pm.put("totalAmount", p.getTotalAmount());
                    pm.put("status",      p.getStatus().name());
                    pm.put("dueDate",     p.getDueDate() != null ? p.getDueDate().toString() : null);
                    pm.put("paidDate",    p.getPaidDate() != null ? p.getPaidDate().toString() : null);
                    pm.put("adminRemarks", p.getAdminRemarks());
                    payments.add(pm);
                    if (p.getStatus() == Payment.PaymentStatus.APPROVED) {
                        totalCollected = totalCollected.add(p.getTotalAmount() != null ? p.getTotalAmount() : p.getAmount());
                        totalLateFines = totalLateFines.add(p.getLateFine() != null ? p.getLateFine() : BigDecimal.ZERO);
                    }
                }
            }

            // Auctions
            List<Auction> auctions = auctionRepository.findByChit(chit);
            List<Map<String, Object>> auctionList = auctions.stream().map(a -> {
                Map<String, Object> am = new LinkedHashMap<>();
                am.put("auctionId",         a.getId());
                am.put("monthNumber",       a.getMonthNumber());
                am.put("auctionDate",       a.getAuctionDate() != null ? a.getAuctionDate().toString() : null);
                am.put("status",            a.getStatus().name());
                am.put("winnerName",        a.getWinner() != null ? a.getWinner().getFullName() : null);
                am.put("winnerEmail",       a.getWinner() != null ? a.getWinner().getEmail() : null);
                am.put("winningBidAmount",  a.getWinningBidAmount());
                am.put("lumpSumPayout",     a.getLumpSumPayout());
                am.put("adminCommission",   a.getAdminCommission());
                am.put("payoutReleased",    a.isPayoutReleased());
                return am;
            }).collect(Collectors.toList());

            // Commissions
            List<Map<String, Object>> commissions = commissionLedgerRepository
                    .findAll().stream()
                    .filter(c -> c.getChit().getId().equals(chit.getId()))
                    .map(c -> {
                        Map<String, Object> cm = new LinkedHashMap<>();
                        cm.put("commissionAmount",     c.getCommissionAmount());
                        cm.put("commissionPercentage", c.getCommissionPercentage());
                        cm.put("source",               c.getSource());
                        cm.put("month",                c.getMonth() != null ? c.getMonth().toString() : null);
                        return cm;
                    }).collect(Collectors.toList());

            BigDecimal totalCommission = commissions.stream()
                    .map(c -> (BigDecimal) c.get("commissionAmount"))
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Settlements
            List<Map<String, Object>> settlementList = new ArrayList<>();
            for (ChitMembership m : memberships) {
                for (Settlement s : settlementRepository.findAll().stream()
                        .filter(x -> x.getMembership().getId().equals(m.getId()))
                        .collect(Collectors.toList())) {
                    Map<String, Object> sm = new LinkedHashMap<>();
                    sm.put("memberName",            m.getUser().getFullName());
                    sm.put("memberEmail",           m.getUser().getEmail());
                    sm.put("type",                  s.getType() != null ? s.getType().name() : null);
                    sm.put("status",                s.getStatus().name());
                    sm.put("totalPaidAmount",       s.getTotalPaidAmount());
                    sm.put("pendingDues",           s.getPendingDues());
                    sm.put("lateFines",             s.getLateFines());
                    sm.put("deductionAmount",       s.getDeductionAmount());
                    sm.put("finalSettlementAmount", s.getFinalSettlementAmount());
                    sm.put("adminRemarks",          s.getAdminRemarks());
                    sm.put("requestedAt",           s.getRequestedAt() != null ? s.getRequestedAt().toString() : null);
                    sm.put("processedAt",           s.getProcessedAt() != null ? s.getProcessedAt().toString() : null);
                    settlementList.add(sm);
                }
            }

            // Member summaries
            List<Map<String, Object>> memberList = memberships.stream().map(m -> {
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("userId",          m.getUser().getId());
                mm.put("fullName",        m.getUser().getFullName());
                mm.put("email",           m.getUser().getEmail());
                mm.put("phone",           m.getUser().getPhone());
                mm.put("status",          m.getStatus().name());
                mm.put("hasWonAuction",   m.isHasWonAuction());
                mm.put("agreementNumber", m.getAgreementNumber());
                mm.put("joinedAt",        m.getJoinedAt() != null ? m.getJoinedAt().toString() : null);
                mm.put("rejectionReason", m.getRejectionReason());
                return mm;
            }).collect(Collectors.toList());

            // Chit details
            Map<String, Object> chitMap = new LinkedHashMap<>();
            chitMap.put("id",                       chit.getId());
            chitMap.put("name",                     chit.getName());
            chitMap.put("description",              chit.getDescription());
            chitMap.put("monthlyAmount",            chit.getMonthlyAmount());
            chitMap.put("totalMembers",             chit.getTotalMembers());
            chitMap.put("durationMonths",           chit.getDurationMonths());
            chitMap.put("totalChitValue",           chit.getTotalChitValue());
            chitMap.put("adminCommissionPercentage",chit.getAdminCommissionPercentage());
            chitMap.put("minBidAmount",             chit.getMinBidAmount());
            chitMap.put("maxBidAmount",             chit.getMaxBidAmount());
            chitMap.put("startDate",                chit.getStartDate() != null ? chit.getStartDate().toString() : null);
            chitMap.put("endDate",                  chit.getEndDate() != null ? chit.getEndDate().toString() : null);
            chitMap.put("status",                   chit.getStatus().name());
            chitMap.put("closingReason",            chit.getClosingReason());
            chitMap.put("createdAt",                chit.getCreatedAt() != null ? chit.getCreatedAt().toString() : null);
            chitMap.put("createdBy",                chit.getCreatedBy() != null ? chit.getCreatedBy().getFullName() : null);

            // Summary
            long completedPayments = payments.stream().filter(p -> "APPROVED".equals(p.get("status"))).count();
            long pendingPayments   = payments.stream().filter(p -> "PENDING".equals(p.get("status"))).count();
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("totalMembers",         memberships.size());
            summary.put("totalCollected",        totalCollected);
            summary.put("totalLateFines",        totalLateFines);
            summary.put("totalCommissionEarned", totalCommission);
            summary.put("totalAuctions",         auctions.size());
            summary.put("completedPayments",     completedPayments);
            summary.put("pendingPayments",       pendingPayments);
            summary.put("archivedAt",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));

            // Final envelope
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("chit",        chitMap);
            envelope.put("summary",     summary);
            envelope.put("members",     memberList);
            envelope.put("payments",    payments);
            envelope.put("auctions",    auctionList);
            envelope.put("commissions", commissions);
            envelope.put("settlements", settlementList);

            return MAPPER.writeValueAsString(envelope);
        } catch (Exception e) {
            log.error("Failed to build chit JSON snapshot for chit {}", chit.getId(), e);
            return "{\"error\":\"snapshot failed\",\"chitId\":" + chit.getId() + "}";
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String savePdfToTemp(byte[] bytes, String chitName, Long chitId) throws Exception {
        String safeName = chitName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String filename = "chit_analysis_" + safeName + "_" + chitId + "_"
                + System.currentTimeMillis() + ".pdf";
        java.nio.file.Path dir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "ygc-chit-history");
        java.nio.file.Files.createDirectories(dir);
        java.nio.file.Path file = dir.resolve(filename);
        java.nio.file.Files.write(file, bytes);
        return file.toAbsolutePath().toString();
    }
}
