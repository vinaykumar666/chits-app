package com.ygc.controller;

import com.ygc.model.User;
import com.ygc.repository.*;
import com.ygc.service.ChitService;
import com.ygc.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Admin PDF report endpoints.
 *
 * FIX: All methods are @Transactional(readOnly=true) so that Hibernate
 * can eagerly load lazy associations within a single session — preventing
 * LazyInitializationException and the N+1 hang on large datasets.
 * Repositories use JOIN FETCH queries to load everything in one SQL round-trip.
 */
@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class ReportController {

    private final ReportExportService reportExportService;
    private final CommissionLedgerRepository commissionLedgerRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final ChitService chitService;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final java.math.BigDecimal ZERO = java.math.BigDecimal.ZERO;

    @GetMapping("/commission/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> commissionPdf() {
        log.debug("Generating commission report PDF");
        // FIX: use findAllForReport() — single JOIN FETCH query, no N+1
        var ledger = commissionLedgerRepository.findAllForReport();
        var total  = ledger.stream().map(l -> l.getCommissionAmount()).reduce(ZERO, java.math.BigDecimal::add);
        byte[] pdf = reportExportService.generateCommissionReport(ledger, total);
        return pdfResponse(pdf, "YGC_Commission_" + ts() + ".pdf");
    }

    @GetMapping("/payments/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> paymentsPdf() {
        log.debug("Generating payments report PDF");
        // FIX: use findAllForReport() — single JOIN FETCH query, no N+1
        byte[] pdf = reportExportService.generatePaymentReport(paymentRepository.findAllForReport());
        return pdfResponse(pdf, "YGC_Payments_" + ts() + ".pdf");
    }

    @GetMapping("/settlements/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> settlementsPdf() {
        log.debug("Generating settlements report PDF");
        // FIX: use findAllForReport() — single JOIN FETCH query, no N+1
        byte[] pdf = reportExportService.generateSettlementReport(settlementRepository.findAllForReport());
        return pdfResponse(pdf, "YGC_Settlements_" + ts() + ".pdf");
    }

    @GetMapping("/members/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> membersPdf() {
        log.debug("Generating members report PDF");
        var members = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.MEMBER)
                .collect(Collectors.toList());
        byte[] pdf = reportExportService.generateMembersReport(members);
        return pdfResponse(pdf, "YGC_Members_" + ts() + ".pdf");
    }

    @GetMapping("/chits/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> chitsPdf() {
        log.debug("Generating chits report PDF");
        byte[] pdf = reportExportService.generateChitsReport(chitService.getAllChits());
        return pdfResponse(pdf, "YGC_Chits_" + ts() + ".pdf");
    }

    private String ts() {
        return LocalDateTime.now().format(TS);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdf);
    }
}
