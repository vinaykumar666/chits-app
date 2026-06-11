package com.ygc.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.ygc.model.*;
import com.ygc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService {

    private final ChitRepository chitRepository;
    private final PaymentRepository paymentRepository;
    private final CommissionLedgerRepository commissionLedgerRepository;
    private final SettlementRepository settlementRepository;

    // App brand colours
    private static final DeviceRgb NAVY       = new DeviceRgb(26, 26, 46);
    private static final DeviceRgb GOLD       = new DeviceRgb(240, 165, 0);
    private static final DeviceRgb GOLD_LIGHT = new DeviceRgb(255, 248, 220);
    private static final DeviceRgb BLUE       = new DeviceRgb(51, 102, 204);
    private static final DeviceRgb SUCCESS    = new DeviceRgb(25, 135, 84);
    private static final DeviceRgb DANGER     = new DeviceRgb(220, 53, 69);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(248, 249, 250);
    private static final DeviceRgb BORDER     = new DeviceRgb(222, 226, 230);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    // ── Chit Analysis PDF (full archive report) ───────────────────────────────
    //
    //  TWO entry-points:
    //    1. generateChitAnalysisPdf(Chit, json)   — called just before close/delete
    //    2. generateChitAnalysisPdfFromJson(...)   — regenerate from stored snapshot
    //
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Full chit analysis PDF.  Called by ChitHistoryService with a live Chit
     * object (before deletion) and the pre-built JSON snapshot string.
     */
    public byte[] generateChitAnalysisPdf(Chit chit, String snapshotJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = mapper.readValue(snapshotJson, java.util.Map.class);
            return buildChitAnalysisPdfFromMap(chit.getName(), data);
        } catch (Exception e) {
            log.error("Failed to generate chit analysis PDF for chit {}", chit.getId(), e);
            throw new RuntimeException("Chit analysis PDF generation failed", e);
        }
    }

    /**
     * Regenerate the analysis PDF purely from the stored JSON string
     * (used when the original PDF file is missing on disk).
     */
    public byte[] generateChitAnalysisPdfFromJson(String chitName, String snapshotJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = mapper.readValue(snapshotJson, java.util.Map.class);
            return buildChitAnalysisPdfFromMap(chitName, data);
        } catch (Exception e) {
            log.error("Failed to regenerate chit analysis PDF from JSON for chit '{}'", chitName, e);
            throw new RuntimeException("Chit analysis PDF regeneration failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private byte[] buildChitAnalysisPdfFromMap(String chitName,
                                               java.util.Map<String, Object> data) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            registerPageNumbers(pdf);
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(40, 40, 50, 40);

            java.util.Map<String, Object> chit     = (java.util.Map<String, Object>) data.getOrDefault("chit",     java.util.Collections.emptyMap());
            java.util.Map<String, Object> summary  = (java.util.Map<String, Object>) data.getOrDefault("summary",  java.util.Collections.emptyMap());
            List<java.util.Map<String,Object>> members     = (List<java.util.Map<String,Object>>) data.getOrDefault("members",     java.util.Collections.emptyList());
            List<java.util.Map<String,Object>> payments    = (List<java.util.Map<String,Object>>) data.getOrDefault("payments",    java.util.Collections.emptyList());
            List<java.util.Map<String,Object>> auctions    = (List<java.util.Map<String,Object>>) data.getOrDefault("auctions",    java.util.Collections.emptyList());
            List<java.util.Map<String,Object>> commissions = (List<java.util.Map<String,Object>>) data.getOrDefault("commissions", java.util.Collections.emptyList());
            List<java.util.Map<String,Object>> settlements = (List<java.util.Map<String,Object>>) data.getOrDefault("settlements", java.util.Collections.emptyList());

            // ── Header ─────────────────────────────────────────────────
            addHeader(doc, "Chit Analysis Report", "Complete Record for: " + chitName);
            addReportIntro(doc,
                    "This certified archive report summarises the full lifecycle of the chit group — "
                            + "members, payments, auctions, commissions, and settlements — "
                            + "generated from YGC Internal records at the time of closure.");

            // ── Meta info row ──────────────────────────────────────────
            String status    = str(chit.get("status"));
            String archivedAt = str(summary.get("archivedAt"));
            addMetaRow(doc, "Final Status", status, "Archived At", archivedAt);
            addMetaRow(doc,
                    "Start Date",  str(chit.get("startDate")),
                    "End Date",    str(chit.get("endDate")));

            String closingReason = str(chit.get("closingReason"));
            if (closingReason != null && !closingReason.isBlank()) {
                doc.add(new Paragraph("Closing Reason: " + closingReason)
                        .setFontSize(9).setFontColor(DANGER).setMarginBottom(8));
            }

            // ── Summary boxes ──────────────────────────────────────────
            addThreeSummaryBoxes(doc,
                    "Total Members",      str(summary.get("totalMembers")),  BLUE,
                    "Total Collected",    "₹" + str(summary.get("totalCollected")),  SUCCESS,
                    "Commission Earned",  "₹" + str(summary.get("totalCommissionEarned")),  GOLD);

            addThreeSummaryBoxes(doc,
                    "Total Auctions",     str(summary.get("totalAuctions")),  NAVY,
                    "Payments Approved",  str(summary.get("completedPayments")),  SUCCESS,
                    "Payments Pending",   str(summary.get("pendingPayments")),  DANGER);

            // ── Chit Configuration ─────────────────────────────────────
            sectionTitle(doc, "Chit Configuration");
            Table configTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                    .useAllAvailableWidth().setMarginBottom(16).setFontSize(9);
            addConfigRow(configTable, "Monthly Amount", "₹" + str(chit.get("monthlyAmount")),
                    "Duration",       str(chit.get("durationMonths")) + " months");
            addConfigRow(configTable, "Total Members",  str(chit.get("totalMembers")),
                    "Total Chit Value", "₹" + str(chit.get("totalChitValue")));
            addConfigRow(configTable, "Commission %",   str(chit.get("adminCommissionPercentage")) + "%",
                    "Created By",     str(chit.get("createdBy")));
            addConfigRow(configTable, "Min Bid",        "₹" + str(chit.get("minBidAmount")),
                    "Max Bid",        "₹" + str(chit.get("maxBidAmount")));
            doc.add(configTable);

            // ── Members ────────────────────────────────────────────────
            sectionTitle(doc, "Members (" + members.size() + ")");
            if (!members.isEmpty()) {
                float[] mCols = {30f, 150f, 170f, 80f, 60f, 60f, 120f};
                Table mt = createTable(mCols);
                addHeaderRow(mt, "#", "Full Name", "Email", "Status", "Won", "Joined", "Agreement No.");
                int idx = 1;
                for (java.util.Map<String, Object> m : members) {
                    addDataRow(mt, idx % 2 == 0,
                            String.valueOf(idx++),
                            str(m.get("fullName")),
                            str(m.get("email")),
                            str(m.get("status")),
                            Boolean.TRUE.equals(m.get("hasWonAuction")) ? "Yes" : "No",
                            safe(str(m.get("joinedAt")), 10),
                            str(m.get("agreementNumber")));
                }
                doc.add(mt);
            } else {
                doc.add(new Paragraph("No members recorded.").setFontSize(9).setFontColor(DANGER));
            }

            // ── Auctions ───────────────────────────────────────────────
            sectionTitle(doc, "Auctions (" + auctions.size() + ")");
            if (!auctions.isEmpty()) {
                float[] aCols = {40f, 60f, 80f, 130f, 90f, 90f, 90f, 60f};
                Table at = createTable(aCols);
                addHeaderRow(at, "#", "Month", "Date", "Winner", "Bid Amount", "Payout", "Commission", "Released");
                int idx = 1;
                for (java.util.Map<String, Object> a : auctions) {
                    addDataRow(at, idx % 2 == 0,
                            String.valueOf(idx++),
                            "M" + str(a.get("monthNumber")),
                            safe(str(a.get("auctionDate")), 10),
                            str(a.get("winnerName")),
                            "₹" + str(a.get("winningBidAmount")),
                            "₹" + str(a.get("lumpSumPayout")),
                            "₹" + str(a.get("adminCommission")),
                            Boolean.TRUE.equals(a.get("payoutReleased")) ? "Yes" : "No");
                }
                doc.add(at);
            } else {
                doc.add(new Paragraph("No auctions conducted.").setFontSize(9).setFontColor(DANGER));
            }

            // ── Payments ───────────────────────────────────────────────
            sectionTitle(doc, "Payments (" + payments.size() + ")");
            if (!payments.isEmpty()) {
                float[] pCols = {30f, 120f, 40f, 60f, 50f, 70f, 60f, 70f};
                Table pt = createTable(pCols);
                addHeaderRow(pt, "#", "Member", "Mo.", "Amount", "Fine", "Total", "Status", "Paid Date");
                int idx = 1;
                for (java.util.Map<String, Object> p : payments) {
                    String pStatus = str(p.get("status"));
                    addDataRow(pt, idx % 2 == 0,
                            String.valueOf(idx++),
                            str(p.get("memberName")),
                            "M" + str(p.get("monthNumber")),
                            "₹" + str(p.get("amount")),
                            nullToZero(str(p.get("lateFine"))),
                            "₹" + str(p.get("totalAmount")),
                            pStatus,
                            safe(str(p.get("paidDate")), 10));
                }
                doc.add(pt);
            } else {
                doc.add(new Paragraph("No payments recorded.").setFontSize(9).setFontColor(DANGER));
            }

            // ── Commissions ────────────────────────────────────────────
            sectionTitle(doc, "Commission Ledger (" + commissions.size() + " entries)");
            if (!commissions.isEmpty()) {
                float[] cCols = {40f, 100f, 100f, 200f, 100f};
                Table ct = createTable(cCols);
                addHeaderRow(ct, "#", "Amount", "Percentage", "Source", "Month");
                int idx = 1;
                for (java.util.Map<String, Object> c : commissions) {
                    addDataRow(ct, idx % 2 == 0,
                            String.valueOf(idx++),
                            "₹" + str(c.get("commissionAmount")),
                            str(c.get("commissionPercentage")) + "%",
                            str(c.get("source")),
                            safe(str(c.get("month")), 10));
                }
                doc.add(ct);
            } else {
                doc.add(new Paragraph("No commission entries.").setFontSize(9).setFontColor(DANGER));
            }

            // ── Settlements ────────────────────────────────────────────
            if (!settlements.isEmpty()) {
                sectionTitle(doc, "Settlements (" + settlements.size() + ")");
                float[] sCols = {30f, 120f, 60f, 70f, 70f, 50f, 80f, 60f};
                Table st = createTable(sCols);
                addHeaderRow(st, "#", "Member", "Type", "Total Paid", "Deduction", "Status", "Final Amount", "Processed");
                int idx = 1;
                for (java.util.Map<String, Object> s : settlements) {
                    addDataRow(st, idx % 2 == 0,
                            String.valueOf(idx++),
                            str(s.get("memberName")),
                            str(s.get("type")),
                            "₹" + str(s.get("totalPaidAmount")),
                            "₹" + str(s.get("deductionAmount")),
                            str(s.get("status")),
                            "₹" + str(s.get("finalSettlementAmount")),
                            safe(str(s.get("processedAt")), 10));
                }
                doc.add(st);
            }

            addDigitalSeal(doc, "Chit Analysis Report | YGC Internal | " + chitName);
            addFooter(doc);
            doc.close();
            return baos.toByteArray();
        }
    }

    // ── Small helpers used by chit analysis builder only ──────────────────

    private void sectionTitle(Document doc, String title) {
        doc.add(new Paragraph(title).setFontSize(12).setBold().setFontColor(NAVY)
                .setMarginTop(16).setMarginBottom(4));
        LineSeparator ls = new LineSeparator(new SolidLine(0.8f));
        ls.setStrokeColor(BLUE);
        doc.add(ls);
        doc.add(new Paragraph("").setMarginBottom(6));
    }

    private void addConfigRow(Table table, String k1, String v1, String k2, String v2) {
        table.addCell(styledCell(k1, 9, true));
        table.addCell(styledCell(v1 != null ? v1 : "-", 9, false));
        table.addCell(styledCell(k2, 9, true));
        table.addCell(styledCell(v2 != null ? v2 : "-", 9, false));
    }

    private String str(Object o) {
        if (o == null) return "-";
        String s = o.toString().trim();
        return s.isEmpty() ? "-" : s;
    }

    private String nullToZero(String s) {
        if (s == null || s.equals("-")) return "₹0";
        return "₹" + s;
    }

    /** Return only the first {@code len} chars of a string (e.g. date prefix). */
    private String safe(String s, int len) {
        if (s == null || s.equals("-")) return "-";
        return s.length() > len ? s.substring(0, len) : s;
    }

    // ── Commission Report PDF ────────────────────────────────────────────────
    public byte[] generateCommissionReport(List<CommissionLedger> ledger, BigDecimal totalCommission) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            registerPageNumbers(pdf);
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(40, 40, 50, 40);

            addHeader(doc, "Commission Report", "Admin Commission Ledger");
            addReportIntro(doc,
                    "Commission ledger showing admin earnings per chit cycle. "
                            + "Amounts are recorded when auctions close and payouts are processed.");
            addMetaRow(doc, "Generated", LocalDateTime.now().format(FMT), "Total Records", String.valueOf(ledger.size()));

            // Summary box
            addSummaryBox(doc, "Total Commission Earned", "₹" + totalCommission.toPlainString(), GOLD);

            // Table
            float[] cols = {60f, 200f, 100f, 100f, 100f, 90f};
            Table table = createTable(cols);
            addHeaderRow(table, "#", "Chit Name", "Commission %", "Amount", "Source", "Month");
            int i = 1;
            BigDecimal running = BigDecimal.ZERO;
            for (CommissionLedger l : ledger) {
                running = running.add(l.getCommissionAmount());
                boolean alt = i % 2 == 0;
                addDataRow(table, alt, String.valueOf(i++),
                        l.getChit().getName(),
                        l.getCommissionPercentage().toPlainString() + "%",
                        "₹" + l.getCommissionAmount().toPlainString(),
                        l.getSource() != null ? l.getSource() : "-",
                        l.getMonth() != null ? l.getMonth().format(DateTimeFormatter.ofPattern("MMM yyyy")) : "-");
            }
            doc.add(table);
            addDigitalSeal(doc, "Commission Report | YGC Internal");
            addFooter(doc);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate commission report PDF", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ── Payment Report PDF ───────────────────────────────────────────────────
    public byte[] generatePaymentReport(List<Payment> payments) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            registerPageNumbers(pdf);
            Document doc = new Document(pdf, PageSize.A4.rotate()); // landscape for more cols
            doc.setMargins(36, 36, 48, 36);

            addHeader(doc, "Payment Report", "All Payment Records");
            addReportIntro(doc,
                    "Consolidated payment register with verification status, fines, and admin remarks. "
                            + "Use this report for reconciliation and audit trails.");
            addMetaRow(doc, "Generated", LocalDateTime.now().format(FMT), "Total Payments", String.valueOf(payments.size()));

            long approved = payments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.APPROVED).count();
            long rejected = payments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.REJECTED).count();
            long pending  = payments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING).count();
            addThreeSummaryBoxes(doc,
                "Approved", String.valueOf(approved), SUCCESS,
                "Rejected", String.valueOf(rejected), DANGER,
                "Pending",  String.valueOf(pending), GOLD);

            float[] cols = {30f, 110f, 120f, 50f, 70f, 60f, 70f, 70f, 90f, 130f};
            Table table = createTable(cols);
            addHeaderRow(table, "#", "Member", "Chit", "Month", "Amount", "Fine", "Total", "Status", "Verified By", "Rejection Reason");
            int i = 1;
            for (Payment p : payments) {
                boolean alt = i % 2 == 0;
                addDataRow(table, alt, String.valueOf(i++),
                        p.getMembership().getUser().getFullName(),
                        p.getMembership().getChit().getName(),
                        "M" + p.getMonthNumber(),
                        "₹" + p.getAmount().toPlainString(),
                        p.getLateFine().compareTo(BigDecimal.ZERO) > 0 ? "₹" + p.getLateFine().toPlainString() : "-",
                        "₹" + p.getTotalAmount().toPlainString(),
                        p.getStatus().name(),
                        p.getVerifiedBy() != null ? p.getVerifiedBy().getFullName() : "-",
                        p.getRejectionReason() != null ? p.getRejectionReason() : "-");
            }
            doc.add(table);
            addDigitalSeal(doc, "Payment Report | YGC Internal");
            addFooter(doc);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate payment report PDF", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ── Settlement Report PDF ────────────────────────────────────────────────
    public byte[] generateSettlementReport(List<Settlement> settlements) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            registerPageNumbers(pdf);
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(40, 40, 50, 40);

            addHeader(doc, "Settlement Report", "Early Exit & Maturity Settlements");
            addReportIntro(doc,
                    "Settlement summary for members exiting early or at maturity, "
                            + "including deductions, final payout amounts, and processing status.");
            addMetaRow(doc, "Generated", LocalDateTime.now().format(FMT), "Total", String.valueOf(settlements.size()));

            float[] cols = {30f, 120f, 110f, 70f, 70f, 70f, 80f, 80f, 130f};
            Table table = createTable(cols);
            addHeaderRow(table, "#", "Member", "Chit", "Type", "Total Paid", "Deduction", "Final", "Status", "Remarks");
            int i = 1;
            for (Settlement s : settlements) {
                addDataRow(table, i % 2 == 0, String.valueOf(i++),
                        s.getMembership().getUser().getFullName(),
                        s.getMembership().getChit().getName(),
                        s.getType().name(),
                        "₹" + s.getTotalPaidAmount().toPlainString(),
                        s.getDeductionAmount() != null ? "₹" + s.getDeductionAmount().toPlainString() : "-",
                        "₹" + s.getFinalSettlementAmount().toPlainString(),
                        s.getStatus().name(),
                        s.getAdminRemarks() != null ? s.getAdminRemarks() : "-");
            }
            doc.add(table);
            addDigitalSeal(doc, "Settlement Report | YGC Internal");
            addFooter(doc);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate settlement report PDF", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ── PDF Building Helpers ─────────────────────────────────────────────────

    /** Load image bytes from classpath; returns null if not found so callers can fall back. */
    private byte[] loadImageBytes(String classpathPath) {
        try {
            InputStream is = new ClassPathResource(classpathPath).getInputStream();
            return is.readAllBytes();
        } catch (Exception e) {
            log.warn("Report image not found at {}: {}", classpathPath, e.getMessage());
            return null;
        }
    }

    private void addHeader(Document doc, String title, String subtitle) {
        // Top gradient header bar — logo left, branding right
        Table header = new Table(UnitValue.createPercentArray(new float[]{0.18f, 0.82f})).useAllAvailableWidth();
        header.setBorder(Border.NO_BORDER);

        // Logo cell
        Cell logoCell = new Cell().setBackgroundColor(NAVY).setPadding(12).setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE).setTextAlignment(TextAlignment.CENTER);
        byte[] logoBytes = loadImageBytes("static/images/ygc-logo.png");
        if (logoBytes != null) {
            try {
                Image logo = new Image(ImageDataFactory.create(logoBytes)).setWidth(70).setHeight(70);
                logoCell.add(logo);
            } catch (Exception e) {
                logoCell.add(new Paragraph("Y&G").setFontColor(GOLD).setFontSize(22).setBold());
            }
        } else {
            Table logoBadge = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
            Cell badge = new Cell().setBorder(Border.NO_BORDER).setPadding(4);
            badge.add(new Paragraph("Y&G").setFontColor(GOLD).setFontSize(20).setBold().setTextAlignment(TextAlignment.CENTER));
            badge.add(new Paragraph("CHITS").setFontColor(DeviceRgb.WHITE).setFontSize(8).setTextAlignment(TextAlignment.CENTER).setMarginTop(-2));
            logoBadge.addCell(badge);
            logoCell.add(logoBadge);
        }
        header.addCell(logoCell);

        // Brand text cell
        Cell brandCell = new Cell().setBackgroundColor(NAVY).setPadding(20).setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        brandCell.add(new Paragraph("Y & G CHITS").setFontColor(GOLD).setFontSize(20).setBold().setMarginBottom(2));
        brandCell.add(new Paragraph("Finance & Growth — YGC Internal").setFontColor(DeviceRgb.WHITE).setFontSize(13).setBold());
        brandCell.add(new Paragraph("Save Rupee, Rupee Will Save You In Future")
                .setFontColor(new DeviceRgb(180, 180, 200)).setFontSize(9).setMarginTop(2));
        header.addCell(brandCell);

        doc.add(header);

        // Gold accent bar
        Table accent = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
        accent.addCell(new Cell().setBackgroundColor(GOLD).setHeight(4).setBorder(Border.NO_BORDER));
        doc.add(accent);

        // Title section
        doc.add(new Paragraph(title).setFontSize(20).setBold().setFontColor(NAVY).setMarginTop(16).setMarginBottom(2));
        doc.add(new Paragraph(subtitle).setFontSize(11).setFontColor(new DeviceRgb(100, 100, 120)).setMarginBottom(12));

        // Horizontal rule
        LineSeparator ls = new LineSeparator(new SolidLine(1f));
        ls.setStrokeColor(GOLD);
        doc.add(ls);
        doc.add(new Paragraph("").setMarginBottom(8));
    }

    private void addReportIntro(Document doc, String text) {
        Table intro = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth().setMarginBottom(14);
        Cell cell = new Cell()
                .setBackgroundColor(GOLD_LIGHT)
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(GOLD, 1.5f))
                .setPadding(12);
        cell.add(new Paragraph("About this report")
                .setFontSize(9).setBold().setFontColor(NAVY).setMarginBottom(4));
        cell.add(new Paragraph(text)
                .setFontSize(9).setFontColor(new DeviceRgb(60, 60, 80)).setMultipliedLeading(1.3f));
        intro.addCell(cell);
        doc.add(intro);
    }

    private void registerPageNumbers(PdfDocument pdf) {
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new IEventHandler() {
            @Override
            public void handleEvent(Event event) {
                PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
                PdfDocument pdfDoc = docEvent.getDocument();
                PdfPage page = docEvent.getPage();
                int pageNumber = pdfDoc.getPageNumber(page);
                Rectangle pageSize = page.getPageSize();
                PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);
                try (Canvas c = new Canvas(canvas, pageSize)) {
                    c.showTextAligned(
                            new Paragraph("Page " + pageNumber)
                                    .setFontSize(8)
                                    .setFontColor(new DeviceRgb(140, 140, 155)),
                            pageSize.getWidth() / 2, 18, TextAlignment.CENTER);
                    c.showTextAligned(
                            new Paragraph("YGC Internal · Confidential")
                                    .setFontSize(7)
                                    .setFontColor(new DeviceRgb(180, 180, 190)),
                            pageSize.getWidth() - 40, 18, TextAlignment.RIGHT);
                } catch (Exception e) {
                    log.debug("Could not render page number: {}", e.getMessage());
                }
            }
        });
    }

    private void addMetaRow(Document doc, String k1, String v1, String k2, String v2) {
        Table meta = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth()
                .setMarginBottom(12);
        meta.addCell(styledCell(k1 + ": " + v1, 9, false));
        meta.addCell(styledCell(k2 + ": " + v2, 9, false));
        doc.add(meta);
    }

    private void addSummaryBox(Document doc, String label, String value, DeviceRgb color) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth().setMarginBottom(16);
        Cell c = new Cell().setBackgroundColor(GOLD_LIGHT)
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(color, 2))
                .setPadding(14).setTextAlignment(TextAlignment.CENTER);
        c.add(new Paragraph(value).setFontSize(24).setBold().setFontColor(color).setMarginBottom(2));
        c.add(new Paragraph(label).setFontSize(10).setFontColor(NAVY));
        t.addCell(c);
        doc.add(t);
    }

    private void addThreeSummaryBoxes(Document doc, String l1, String v1, DeviceRgb c1,
                                       String l2, String v2, DeviceRgb c2,
                                       String l3, String v3, DeviceRgb c3) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1})).useAllAvailableWidth().setMarginBottom(16);
        t.addCell(summaryCell(l1, v1, c1));
        t.addCell(summaryCell(l2, v2, c2));
        t.addCell(summaryCell(l3, v3, c3));
        doc.add(t);
    }

    private Cell summaryCell(String label, String value, DeviceRgb color) {
        Cell c = new Cell().setPadding(12).setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(color, 2));
        c.add(new Paragraph(value).setFontSize(20).setBold().setFontColor(color).setMarginBottom(2));
        c.add(new Paragraph(label).setFontSize(9).setFontColor(NAVY));
        return c;
    }

    private Table createTable(float[] cols) {
        return new Table(UnitValue.createPointArray(cols)).useAllAvailableWidth()
                .setMarginBottom(16).setFontSize(8);
    }

    private void addHeaderRow(Table table, String... headers) {
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(NAVY).setFontColor(DeviceRgb.WHITE)
                    .setPadding(7).setBold().setFontSize(8)
                    .setBorder(Border.NO_BORDER)
                    .add(new Paragraph(h)));
        }
    }

    private void addDataRow(Table table, boolean alt, String... vals) {
        DeviceRgb bg = alt ? LIGHT_GRAY : WHITE;
        for (String v : vals) {
            table.addCell(new Cell()
                    .setBackgroundColor(bg).setPadding(6)
                    .setBorderTop(new com.itextpdf.layout.borders.SolidBorder(BORDER, 0.5f))
                    .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER).setBorderBottom(Border.NO_BORDER)
                    .add(new Paragraph(v != null ? v : "-")));
        }
    }

    private Cell styledCell(String text, int size, boolean bold) {
        Cell c = new Cell().setBorder(Border.NO_BORDER).setPadding(3);
        Paragraph p = new Paragraph(text).setFontSize(size).setFontColor(new DeviceRgb(80, 80, 100));
        if (bold) p.setBold();
        c.add(p);
        return c;
    }

    private void addDigitalSeal(Document doc, String label) {
        doc.add(new Paragraph("").setMarginTop(20));
        Table sealTable = new Table(UnitValue.createPercentArray(new float[]{0.25f, 0.75f})).useAllAvailableWidth();
        sealTable.setBorder(new com.itextpdf.layout.borders.DashedBorder(GOLD, 1.5f));

        // Stamp image cell
        Cell stampCell = new Cell().setPadding(10).setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE).setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(GOLD_LIGHT);
        byte[] stampBytes = loadImageBytes("static/images/ygc-stamp.png");
        if (stampBytes != null) {
            try {
                Image stamp = new Image(ImageDataFactory.create(stampBytes)).setWidth(80).setHeight(80);
                stampCell.add(stamp);
            } catch (Exception e) {
                stampCell.add(new Paragraph("✦").setFontSize(36).setFontColor(GOLD));
            }
        } else {
            stampCell.add(new Paragraph("✦").setFontSize(36).setFontColor(GOLD));
        }
        sealTable.addCell(stampCell);

        // Certification text cell
        Cell textCell = new Cell().setPadding(14).setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(GOLD_LIGHT);
        textCell.add(new Paragraph("✦ DIGITALLY CERTIFIED ✦")
                .setFontSize(11).setBold().setFontColor(NAVY).setMarginBottom(4));
        textCell.add(new Paragraph("Y & G CHITS — Finance & Growth")
                .setFontSize(10).setBold().setFontColor(GOLD).setMarginBottom(2));
        textCell.add(new Paragraph(label)
                .setFontSize(9).setFontColor(new DeviceRgb(80, 80, 100)).setMarginBottom(2));
        textCell.add(new Paragraph("Generated: " + LocalDateTime.now().format(FMT) + " | YGC Internal System")
                .setFontSize(8).setFontColor(new DeviceRgb(120, 120, 140)));
        textCell.add(new Paragraph("CHITS FUND • CERTIFIED & SEALED")
                .setFontSize(7).setFontColor(GOLD).setMarginTop(4));
        sealTable.addCell(textCell);

        doc.add(sealTable);
    }

    private void addFooter(Document doc) {
        doc.add(new Paragraph("").setMarginTop(12));
        LineSeparator ls = new LineSeparator(new SolidLine(0.5f));
        ls.setStrokeColor(BORDER);
        doc.add(ls);
        Table footer = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth().setMarginTop(6);
        footer.addCell(new Cell().setBorder(Border.NO_BORDER).add(
                new Paragraph("Y & G Chits — Finance & Growth | Save Rupee, Rupee Will Save You In Future")
                        .setFontSize(8).setFontColor(new DeviceRgb(150, 150, 160))));
        footer.addCell(new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).add(
                new Paragraph("Contact: +91 8919508889")
                        .setFontSize(8).setFontColor(new DeviceRgb(150, 150, 160))));
        doc.add(footer);
    }

    // ── Admin: All Members Report PDF ────────────────────────────────────────
    public byte[] generateMembersReport(List<User> members) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            registerPageNumbers(pdf);
            Document doc = new Document(pdf, PageSize.A4.rotate());
            doc.setMargins(36, 36, 48, 36);

            addHeader(doc, "Members Report", "All Registered Members");
            addReportIntro(doc,
                    "Directory of all registered members with contact details, role, and account status.");
            addMetaRow(doc, "Generated", LocalDateTime.now().format(FMT), "Total Members", String.valueOf(members.size()));

            float[] cols = {30f, 160f, 200f, 80f, 80f, 80f, 130f};
            Table table = createTable(cols);
            addHeaderRow(table, "#", "Full Name", "Email", "Phone", "Role", "Status", "Joined");
            int i = 1;
            for (User u : members) {
                addDataRow(table, i % 2 == 0, String.valueOf(i++),
                        u.getFullName() != null ? u.getFullName() : "-",
                        u.getEmail() != null ? u.getEmail() : "-",
                        u.getPhone() != null ? u.getPhone() : "-",
                        u.getRole() != null ? u.getRole().name() : "-",
                        u.isActive() ? "Active" : "Inactive",
                        u.getCreatedAt() != null ? u.getCreatedAt().format(DATE_FMT) : "-");
            }
            doc.add(table);
            addDigitalSeal(doc, "Members Report | YGC Admin");
            addFooter(doc);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate members report PDF", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ── Admin: Chits List Report PDF ─────────────────────────────────────────
    public byte[] generateChitsReport(List<Chit> chits) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            registerPageNumbers(pdf);
            Document doc = new Document(pdf, PageSize.A4.rotate());
            doc.setMargins(36, 36, 48, 36);

            addHeader(doc, "Chit Groups Report", "All Chit Fund Groups");
            addReportIntro(doc,
                    "Overview of every chit fund group — monthly contribution, pool size, duration, and lifecycle status.");
            addMetaRow(doc, "Generated", LocalDateTime.now().format(FMT), "Total Chits", String.valueOf(chits.size()));

            long open      = chits.stream().filter(c -> c.getStatus() == Chit.ChitStatus.OPEN).count();
            long active    = chits.stream().filter(c -> c.getStatus() == Chit.ChitStatus.ACTIVE).count();
            long completed = chits.stream().filter(c -> c.getStatus() == Chit.ChitStatus.COMPLETED).count();
            addThreeSummaryBoxes(doc,
                    "Open", String.valueOf(open), SUCCESS,
                    "Active", String.valueOf(active), GOLD,
                    "Completed", String.valueOf(completed), BLUE);

            float[] cols = {30f, 140f, 80f, 80f, 70f, 70f, 70f, 80f, 80f};
            Table table = createTable(cols);
            addHeaderRow(table, "#", "Chit Name", "Monthly ₹", "Total ₹", "Members", "Duration", "Commission", "Start", "Status");
            int i = 1;
            for (Chit c : chits) {
                addDataRow(table, i % 2 == 0, String.valueOf(i++),
                        c.getName(),
                        "₹" + c.getMonthlyAmount().toPlainString(),
                        "₹" + (c.getTotalChitValue() != null ? c.getTotalChitValue().toPlainString() : "-"),
                        String.valueOf(c.getTotalMembers()),
                        c.getDurationMonths() + " mo",
                        c.getAdminCommissionPercentage().toPlainString() + "%",
                        c.getStartDate() != null ? c.getStartDate().toString() : "-",
                        c.getStatus().name());
            }
            doc.add(table);
            addDigitalSeal(doc, "Chit Groups Report | YGC Admin");
            addFooter(doc);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate chits report PDF", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ── Member: My Memberships & Summary PDF ─────────────────────────────────
    public byte[] generateMemberSummaryReport(User user, List<ChitMembership> memberships) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            registerPageNumbers(pdf);
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(40, 40, 50, 40);

            addHeader(doc, "My Chit Memberships", "Personal Summary — " + user.getFullName());
            addReportIntro(doc,
                    "Your personal membership portfolio across all chit groups, including monthly commitment and current status.");
            addMetaRow(doc, "Member", user.getEmail(), "Generated", LocalDateTime.now().format(FMT));

            long active = memberships.stream().filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE).count();
            long settled = memberships.stream().filter(m -> m.getStatus() == ChitMembership.MembershipStatus.SETTLED).count();
            addThreeSummaryBoxes(doc,
                    "Total", String.valueOf(memberships.size()), BLUE,
                    "Active", String.valueOf(active), SUCCESS,
                    "Settled", String.valueOf(settled), GOLD);

            float[] cols = {30f, 150f, 80f, 80f, 80f, 80f, 70f};
            Table table = createTable(cols);
            addHeaderRow(table, "#", "Chit Name", "Monthly ₹", "Total ₹", "Duration", "Status", "Joined");
            int i = 1;
            for (ChitMembership m : memberships) {
                addDataRow(table, i % 2 == 0, String.valueOf(i++),
                        m.getChit().getName(),
                        "₹" + m.getChit().getMonthlyAmount().toPlainString(),
                        "₹" + (m.getChit().getTotalChitValue() != null ? m.getChit().getTotalChitValue().toPlainString() : "-"),
                        m.getChit().getDurationMonths() + " months",
                        m.getStatus().name(),
                        m.getJoinedAt() != null ? m.getJoinedAt().format(DATE_FMT) : "-");
            }
            doc.add(table);
            addDigitalSeal(doc, "Member Summary | " + user.getFullName() + " | YGC Internal");
            addFooter(doc);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate member summary report for {}", user.getEmail(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ── Member: Payment History for a Specific Membership PDF ────────────────
    public byte[] generateMemberPaymentHistoryReport(User user, ChitMembership membership, List<Payment> payments) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            registerPageNumbers(pdf);
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(40, 40, 50, 40);

            addHeader(doc, "Payment History",
                    membership.getChit().getName() + " — " + user.getFullName());
            addReportIntro(doc,
                    "Month-by-month payment statement for this membership, including fines, verification status, and admin remarks.");
            addMetaRow(doc, "Member", user.getEmail(), "Generated", LocalDateTime.now().format(FMT));

            // Totals
            BigDecimal totalPaid = payments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.APPROVED)
                    .map(Payment::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long approved = payments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.APPROVED).count();
            long pending  = payments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING).count();

            addThreeSummaryBoxes(doc,
                    "Total Paid", "₹" + totalPaid.toPlainString(), SUCCESS,
                    "Approved", String.valueOf(approved), SUCCESS,
                    "Pending",  String.valueOf(pending), GOLD);

            float[] cols = {30f, 60f, 80f, 70f, 70f, 80f, 90f, 110f};
            Table table = createTable(cols);
            addHeaderRow(table, "#", "Month", "Amount", "Fine", "Total", "Status", "Paid Date", "Remarks");
            int i = 1;
            for (Payment p : payments) {
                addDataRow(table, i % 2 == 0, String.valueOf(i++),
                        "M" + p.getMonthNumber(),
                        "₹" + p.getAmount().toPlainString(),
                        p.getLateFine().compareTo(BigDecimal.ZERO) > 0 ? "₹" + p.getLateFine().toPlainString() : "-",
                        "₹" + p.getTotalAmount().toPlainString(),
                        p.getStatus().name(),
                        p.getPaidDate() != null ? p.getPaidDate().toString() : "-",
                        p.getAdminRemarks() != null ? p.getAdminRemarks() : "-");
            }
            doc.add(table);
            addDigitalSeal(doc, "Payment Statement | " + user.getFullName() + " | " + membership.getChit().getName());
            addFooter(doc);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate payment history report for membership {}", membership.getId(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }
}
