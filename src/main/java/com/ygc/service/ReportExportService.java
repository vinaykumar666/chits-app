package com.ygc.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.ygc.model.*;
import com.ygc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // ── Commission Report PDF ────────────────────────────────────────────────
    public byte[] generateCommissionReport(List<CommissionLedger> ledger, BigDecimal totalCommission) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(40, 40, 50, 40);

            addHeader(doc, "Commission Report", "Admin Commission Ledger");
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
            Document doc = new Document(pdf, PageSize.A4.rotate()); // landscape for more cols
            doc.setMargins(36, 36, 48, 36);

            addHeader(doc, "Payment Report", "All Payment Records");
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
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(40, 40, 50, 40);

            addHeader(doc, "Settlement Report", "Early Exit & Maturity Settlements");
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

    private void addHeader(Document doc, String title, String subtitle) {
        // Top gradient header bar
        Table header = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
        Cell headerCell = new Cell().setBackgroundColor(NAVY).setPadding(20).setBorder(Border.NO_BORDER);
        headerCell.add(new Paragraph("Y").setFontColor(GOLD).setFontSize(28).setBold().setMarginBottom(2));
        headerCell.add(new Paragraph("YGC Internal").setFontColor(DeviceRgb.WHITE).setFontSize(18).setBold());
        headerCell.add(new Paragraph("Save Rupee, Rupee Will Save You In Future")
                .setFontColor(new DeviceRgb(180, 180, 200)).setFontSize(9).setMarginTop(2));
        header.addCell(headerCell);
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
        Table seal = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
        Cell c = new Cell().setBorder(new com.itextpdf.layout.borders.DashedBorder(GOLD, 1.5f))
                .setPadding(14).setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(GOLD_LIGHT);
        c.add(new Paragraph("✦ DIGITALLY CERTIFIED ✦").setFontSize(11).setBold().setFontColor(NAVY).setMarginBottom(4));
        c.add(new Paragraph(label).setFontSize(9).setFontColor(new DeviceRgb(80, 80, 100)).setMarginBottom(2));
        c.add(new Paragraph("Generated: " + LocalDateTime.now().format(FMT) + " | YGC Internal System")
                .setFontSize(8).setFontColor(new DeviceRgb(120, 120, 140)));
        seal.addCell(c);
        doc.add(seal);
    }

    private void addFooter(Document doc) {
        doc.add(new Paragraph("").setMarginTop(12));
        LineSeparator ls = new LineSeparator(new SolidLine(0.5f));
        ls.setStrokeColor(BORDER);
        doc.add(ls);
        Table footer = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth().setMarginTop(6);
        footer.addCell(new Cell().setBorder(Border.NO_BORDER).add(
                new Paragraph("YGC Internal | Save Rupee, Rupee Will Save You In Future")
                        .setFontSize(8).setFontColor(new DeviceRgb(150, 150, 160))));
        footer.addCell(new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).add(
                new Paragraph("Contact: +91 8919508889")
                        .setFontSize(8).setFontColor(new DeviceRgb(150, 150, 160))));
        doc.add(footer);
    }
}
