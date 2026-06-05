package com.ygc.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.ygc.audit.AuditService;
import com.ygc.model.Chit;
import com.ygc.model.ChitMembership;
import com.ygc.model.User;
import com.ygc.repository.ChitMembershipRepository;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Handles the full digital agreement lifecycle:
 *  1. Member views and accepts agreement with mandatory checkboxes
 *  2. Join request submitted (PENDING)
 *  3. Admin approves → PDF generated, stored, emailed to customer & admin
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChitAgreementService {

    private final ChitMembershipRepository membershipRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final LoggingUtil loggingUtil;
    private final NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${ygc.mail.from:${spring.mail.username:admin@ygcinternal.com}}")
    private String adminEmail;

    private static final String AGREEMENT_DIR = "uploads/agreements/";

    /**
     * Records that a member has accepted the three mandatory agreement checkboxes.
     * Call this BEFORE creating the ChitMembership (at join-request time).
     */
    @Transactional
    public void recordAgreementAcceptance(ChitMembership membership,
                                          boolean agreementRead,
                                          boolean termsAccepted,
                                          boolean infoProcessingAuthorized) {
        if (!agreementRead || !termsAccepted || !infoProcessingAuthorized) {
            throw new IllegalArgumentException(
                    "All three agreement checkboxes must be checked before joining a chit.");
        }
        membership.setAgreementRead(true);
        membership.setAgreementAccepted(true);
        membership.setInfoProcessingAuthorized(true);
        membership.setTermsAccepted(true);
        membership.setAgreementAcceptedAt(LocalDateTime.now());
        membership.setAgreementNumber(generateAgreementNumber(membership));
        membershipRepository.save(membership);
        loggingUtil.info("Agreement accepted by " + membership.getUser().getEmail()
                + " for chit " + membership.getChit().getId(), "ChitAgreementService");
    }

    /**
     * Called when admin approves a membership.
     * Generates the signed PDF, stores it, and emails it to both customer and admin.
     */
    @Transactional
    public String generateAndDistributeAgreementPdf(ChitMembership membership, User admin) {
        loggingUtil.transactionStart("generateAndDistributeAgreementPdf", "ChitAgreementService");
        try {
            membership.setAgreementApprovedAt(LocalDateTime.now());
            if (membership.getAgreementNumber() == null) {
                membership.setAgreementNumber(generateAgreementNumber(membership));
            }

            String pdfPath = buildAgreementPdf(membership);
            membership.setAgreementPdfPath(pdfPath);
            membershipRepository.save(membership);

            // Email to customer
            sendAgreementEmail(membership.getUser().getEmail(),
                    membership.getUser().getFullName(), membership, pdfPath, false);

            // Email to admin
            sendAgreementEmail(adminEmail, "Admin", membership, pdfPath, true);

            auditService.log(admin, "AGREEMENT_PDF_GENERATED", "ChitMembership",
                    membership.getId(), "Agreement PDF generated and emailed: " + pdfPath);

            // Push real-time notification to the member
            notificationService.notifyAgreementApproved(
                    membership.getUser().getEmail(),
                    membership.getChit().getName(),
                    membership.getAgreementNumber());

            loggingUtil.transactionComplete("generateAndDistributeAgreementPdf", "ChitAgreementService");
            return pdfPath;
        } catch (Exception e) {
            loggingUtil.transactionFailed("generateAndDistributeAgreementPdf", "ChitAgreementService", e);
            throw new RuntimeException("Failed to generate agreement PDF: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String generateAgreementNumber(ChitMembership membership) {
        return "AGR-" + membership.getChit().getId()
                + "-" + membership.getUser().getId()
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String buildAgreementPdf(ChitMembership membership) throws Exception {
        new File(AGREEMENT_DIR).mkdirs();
        String filename = "agreement_" + membership.getAgreementNumber() + ".pdf";
        String filepath = AGREEMENT_DIR + filename;

        Chit chit = membership.getChit();
        User customer = membership.getUser();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy");

        PdfWriter writer = new PdfWriter(new FileOutputStream(filepath));
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);
        doc.setMargins(45, 50, 50, 50);

        DeviceRgb NAVY     = new DeviceRgb(15, 15, 26);
        DeviceRgb GOLD     = new DeviceRgb(240, 165, 0);
        DeviceRgb GOLD_BG  = new DeviceRgb(255, 248, 225);
        DeviceRgb BLUE     = new DeviceRgb(51, 102, 204);
        DeviceRgb GREEN    = new DeviceRgb(16, 185, 129);
        DeviceRgb GRAY     = new DeviceRgb(120, 120, 140);
        DeviceRgb LGRAY    = new DeviceRgb(245, 246, 250);
        DeviceRgb WHITE    = new DeviceRgb(255, 255, 255);

        // ══════════════ HEADER with logo placeholder ══════════════
        Table headerTbl = new Table(new float[]{120, 320}).setWidth(UnitValue.createPercentValue(100));

        // Logo cell — circular brand mark
        Cell logoCell = new Cell()
                .add(new Paragraph("YGC")
                        .setFontSize(24).setBold().setFontColor(GOLD)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("EST. 2024")
                        .setFontSize(7).setFontColor(GRAY)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(NAVY).setBorder(new SolidBorder(GOLD, 2))
                .setPadding(12).setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
        headerTbl.addCell(logoCell);

        // Title cell
        Cell titleCell = new Cell()
                .add(new Paragraph("YGC INTERNAL").setFontSize(22).setBold().setFontColor(NAVY))
                .add(new Paragraph("CHIT FUND PARTICIPATION AGREEMENT")
                        .setFontSize(13).setBold().setFontColor(BLUE).setMarginTop(2))
                .add(new Paragraph("Save Rupee, Rupee Will Save You In Future")
                        .setFontSize(9).setFontColor(GRAY).setItalic().setMarginTop(2))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setPaddingLeft(15).setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
        headerTbl.addCell(titleCell);
        doc.add(headerTbl);

        // Gold divider
        Table divider = new Table(1).setWidth(UnitValue.createPercentValue(100)).setMarginTop(8).setMarginBottom(8);
        divider.addCell(new Cell().setHeight(3).setBackgroundColor(GOLD)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        doc.add(divider);

        // Agreement number + date badge
        Table metaRow = new Table(new float[]{250, 250}).setWidth(UnitValue.createPercentValue(100));
        metaRow.addCell(new Cell().add(new Paragraph("Agreement No: " + membership.getAgreementNumber())
                .setFontSize(10).setBold().setFontColor(NAVY))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        metaRow.addCell(new Cell().add(new Paragraph("Date: " + LocalDateTime.now().format(dateFmt))
                .setFontSize(10).setFontColor(GRAY).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        doc.add(metaRow);
        doc.add(new Paragraph("\n").setFontSize(4));

        // ══════════════ CUSTOMER DETAILS ══════════════
        addSectionHeader(doc, "§ CUSTOMER DETAILS", NAVY);
        Table ct = new Table(new float[]{180, 320}).setWidth(UnitValue.createPercentValue(100));
        addRow(ct, "Full Name", customer.getFullName());
        addRow(ct, "Email", customer.getEmail());
        addRow(ct, "Phone", customer.getPhone() != null ? customer.getPhone() : "—");
        addRow(ct, "Address", customer.getAddress() != null ? customer.getAddress() : "—");
        addRow(ct, "Membership ID", "MEM-" + membership.getId());
        doc.add(ct);
        doc.add(new Paragraph("\n").setFontSize(4));

        // ══════════════ CHIT DETAILS ══════════════
        addSectionHeader(doc, "§ CHIT FUND DETAILS", NAVY);
        Table cht = new Table(new float[]{180, 320}).setWidth(UnitValue.createPercentValue(100));
        addRow(cht, "Chit Name", chit.getName());
        addRow(cht, "Monthly Contribution", "₹" + chit.getMonthlyAmount());
        addRow(cht, "Total Chit Value", "₹" + chit.getTotalChitValue());
        addRow(cht, "Duration", chit.getDurationMonths() + " months");
        addRow(cht, "Members", String.valueOf(chit.getTotalMembers()));
        addRow(cht, "Commission Rate", chit.getAdminCommissionPercentage() + "%");
        addRow(cht, "Start Date", chit.getStartDate() != null ? chit.getStartDate().toString() : "—");
        addRow(cht, "End Date", chit.getEndDate() != null ? chit.getEndDate().toString() : "—");
        doc.add(cht);
        doc.add(new Paragraph("\n").setFontSize(4));

        // ══════════════ TERMS ══════════════
        addSectionHeader(doc, "§ TERMS AND CONDITIONS", NAVY);
        String[] terms = {
            "1. Monthly contributions of ₹" + chit.getMonthlyAmount() + " must be paid on/before the due date.",
            "2. Late fine: ₹20/day charged for payments after the due date.",
            "3. Auctions: Lowest bidder wins. Winner receives (bid amount – " + chit.getAdminCommissionPercentage() + "% commission).",
            "4. Once won, the member cannot bid in subsequent auctions of the same chit.",
            "5. Non-winners earn dividends from the discount (chit value – winning bid), distributed proportionally.",
            "6. Personal information processed solely for chit operations as per the Information Technology Act, 2000.",
            "7. Early exit subject to a " + "2% deduction and admin approval.",
            "8. Disputes resolved per the Chit Funds Act, 1982 and applicable state regulations.",
            "9. This is a legally binding digital agreement upon acceptance."
        };
        for (String term : terms) doc.add(new Paragraph(term).setFontSize(9).setMarginBottom(3).setFontColor(new DeviceRgb(60, 60, 80)));

        doc.add(new Paragraph("\n").setFontSize(6));

        // ══════════════ REQUEST & APPROVAL STAMPS ══════════════
        addSectionHeader(doc, "§ DIGITAL ACCEPTANCE & APPROVAL", NAVY);

        Table stampTbl = new Table(new float[]{250, 250}).setWidth(UnitValue.createPercentValue(100));

        // Request stamp (left)
        Cell requestStamp = new Cell()
                .add(new Paragraph("MEMBER REQUEST").setFontSize(11).setBold().setFontColor(BLUE).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("────────────────").setFontSize(8).setFontColor(GRAY).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Agreement Read: " + (membership.isAgreementRead() ? "✓ YES" : "✗ NO")).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Terms Accepted: " + (membership.isAgreementAccepted() ? "✓ YES" : "✗ NO")).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Info Auth: " + (membership.isInfoProcessingAuthorized() ? "✓ YES" : "✗ NO")).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("────────────────").setFontSize(8).setFontColor(GRAY).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Requested: " + (membership.getAgreementAcceptedAt() != null ? membership.getAgreementAcceptedAt().format(dtf) : "—"))
                        .setFontSize(8).setFontColor(GRAY).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("By: " + customer.getFullName()).setFontSize(8).setFontColor(GRAY).setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(BLUE, 1.5f)).setBackgroundColor(new DeviceRgb(240, 245, 255))
                .setPadding(12);
        stampTbl.addCell(requestStamp);

        // Approval stamp (right)
        Cell approvalStamp = new Cell()
                .add(new Paragraph("★ APPROVED ★").setFontSize(13).setBold().setFontColor(GREEN).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("────────────────").setFontSize(8).setFontColor(GRAY).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("DIGITALLY APPROVED").setFontSize(9).setBold().setFontColor(new DeviceRgb(5,100,60)).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("YGC Internal Admin").setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("────────────────").setFontSize(8).setFontColor(GRAY).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Approved: " + (membership.getAgreementApprovedAt() != null ? membership.getAgreementApprovedAt().format(dtf) : "—"))
                        .setFontSize(8).setFontColor(GRAY).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Seal: YGC-" + membership.getAgreementNumber())
                        .setFontSize(7).setFontColor(GRAY).setItalic().setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(GREEN, 2f)).setBackgroundColor(new DeviceRgb(236, 253, 245))
                .setPadding(12);
        stampTbl.addCell(approvalStamp);
        doc.add(stampTbl);

        // ══════════════ DIGITAL SEAL ══════════════
        doc.add(new Paragraph("\n").setFontSize(6));
        Table sealTable = new Table(1).setWidth(UnitValue.createPercentValue(40))
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        Cell sealCell = new Cell()
                .add(new Paragraph("⬡ YGC DIGITAL SEAL ⬡").setFontSize(10).setBold().setFontColor(GOLD).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Ref: " + membership.getAgreementNumber()).setFontSize(8).setFontColor(GRAY).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Verified • Authentic • Binding").setFontSize(7).setFontColor(GRAY).setItalic().setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(GOLD_BG).setBorder(new SolidBorder(GOLD, 2))
                .setPadding(10);
        sealTable.addCell(sealCell);
        doc.add(sealTable);

        // ══════════════ FOOTER ══════════════
        doc.add(new Paragraph("\n"));
        Table footDiv = new Table(1).setWidth(UnitValue.createPercentValue(100));
        footDiv.addCell(new Cell().setHeight(2).setBackgroundColor(NAVY)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        doc.add(footDiv);
        doc.add(new Paragraph(
                "This is a digitally generated and sealed agreement. No physical signature required.\n"
                + "Agreement No: " + membership.getAgreementNumber()
                + "  |  Generated: " + LocalDateTime.now().format(dtf)
                + "\nYGC Internal  |  Contact: +91 8919508889  |  Save Rupee, Rupee Will Save You In Future")
                .setFontSize(8).setFontColor(GRAY).setTextAlignment(TextAlignment.CENTER).setMarginTop(6));

        doc.close();
        loggingUtil.info("Agreement PDF created: " + filepath, "ChitAgreementService");
        return filepath;
    }

    private void addSectionHeader(Document doc, String title, DeviceRgb color) {
        Paragraph p = new Paragraph(title)
                .setFontSize(12).setBold()
                .setFontColor(color)
                .setMarginBottom(6);
        doc.add(p);
    }

    private void addRow(Table table, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFontSize(10).setBold())
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setBackgroundColor(new DeviceRgb(248, 249, 250))
                .setPadding(6));
        table.addCell(new Cell()
                .add(new Paragraph(value != null ? value : "—").setFontSize(10))
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setPadding(6));
    }

    private void sendAgreementEmail(String to, String recipientName,
                                     ChitMembership membership, String pdfPath, boolean isAdmin) {
        String subject = (isAdmin ? "[Admin Copy] " : "") + "Chit Agreement — "
                + membership.getChit().getName() + " | " + membership.getAgreementNumber();

        String heading = isAdmin
                ? "New Membership Approved"
                : "Your Membership Is Approved!";
        String intro = isAdmin
                ? "A new chit membership has been approved. Agreement details below."
                : "Congratulations <strong>" + membership.getUser().getFullName()
                  + "</strong>! Your membership in <strong>" + membership.getChit().getName()
                  + "</strong> has been approved by the admin.";

        String htmlBody = "<!DOCTYPE html><html><head><meta charset='utf-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'></head>"
            + "<body style='margin:0;padding:0;background:#f4f4f7;font-family:Helvetica,Arial,sans-serif'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f4f7;padding:24px 0'>"
            + "<tr><td align='center'><table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%'>"

            // Header with gold gradient
            + "<tr><td style='background:linear-gradient(135deg,#0f0f1a,#1a1a35);padding:28px 32px;border-radius:16px 16px 0 0;text-align:center'>"
            + "<div style='font-size:28px;font-weight:900;color:#f0a500;letter-spacing:1px'>YGC INTERNAL</div>"
            + "<div style='font-size:12px;color:rgba(255,255,255,.45);margin-top:4px;letter-spacing:2px'>SAVE RUPEE, RUPEE WILL SAVE YOU IN FUTURE</div>"
            + "</td></tr>"

            // Body
            + "<tr><td style='background:#ffffff;padding:32px 32px 24px;border-left:1px solid #e8e8e8;border-right:1px solid #e8e8e8'>"
            + "<h2 style='margin:0 0 16px;color:#0f0f1a;font-size:20px'>" + heading + "</h2>"
            + "<p style='color:#555;font-size:14px;line-height:1.7;margin:0 0 20px'>" + intro + "</p>"

            // Agreement details card
            + "<table width='100%' cellpadding='12' cellspacing='0' style='background:#f8f9fe;border-radius:10px;border:1px solid #eef1f8;margin-bottom:20px'>"
            + "<tr><td style='color:#888;font-size:11px;text-transform:uppercase;letter-spacing:1px;padding-bottom:0'>Agreement Number</td></tr>"
            + "<tr><td style='font-size:18px;font-weight:700;color:#0f0f1a;padding-top:2px'>" + membership.getAgreementNumber() + "</td></tr>"
            + "</table>"

            + "<table width='100%' cellpadding='8' cellspacing='0' style='font-size:13px;color:#333'>"
            + "<tr><td style='color:#888;width:140px'>Customer</td><td style='font-weight:600'>" + membership.getUser().getFullName() + "</td></tr>"
            + "<tr><td style='color:#888'>Email</td><td>" + membership.getUser().getEmail() + "</td></tr>"
            + "<tr><td style='color:#888'>Chit Group</td><td style='font-weight:600'>" + membership.getChit().getName() + "</td></tr>"
            + "<tr><td style='color:#888'>Monthly Amount</td><td style='font-weight:700;color:#f0a500'>₹" + membership.getChit().getMonthlyAmount() + "</td></tr>"
            + "<tr><td style='color:#888'>Total Value</td><td>₹" + membership.getChit().getTotalChitValue() + "</td></tr>"
            + "<tr><td style='color:#888'>Duration</td><td>" + membership.getChit().getDurationMonths() + " months</td></tr>"
            + "<tr><td style='color:#888'>Commission</td><td>" + membership.getChit().getAdminCommissionPercentage() + "%</td></tr>"
            + "<tr><td style='color:#888'>Accepted At</td><td>" + (membership.getAgreementAcceptedAt() != null
                ? membership.getAgreementAcceptedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "—") + "</td></tr>"
            + "<tr><td style='color:#888'>Approved At</td><td>" + (membership.getAgreementApprovedAt() != null
                ? membership.getAgreementApprovedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "—") + "</td></tr>"
            + "</table>"

            + "<div style='margin-top:20px;padding:14px;background:#fff8e1;border-radius:8px;border-left:4px solid #f0a500;font-size:13px;color:#856404'>"
            + "<strong>📎 Agreement PDF Attached</strong> — The signed agreement (No: " + membership.getAgreementNumber() + ") is attached to this email."
            + "</div>"
            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#0f0f1a;padding:20px 32px;border-radius:0 0 16px 16px;text-align:center'>"
            + "<div style='color:#f0a500;font-weight:700;font-size:14px'>YGC Internal</div>"
            + "<div style='color:rgba(255,255,255,.35);font-size:12px;margin-top:4px'>Contact: +91 8919508889</div>"
            + "<div style='color:rgba(255,255,255,.2);font-size:11px;margin-top:8px'>This is an automated email. Please do not reply.</div>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";

        String attachmentName = "Agreement_" + membership.getAgreementNumber() + ".pdf";
        emailService.sendHtmlEmailWithAttachment(to, subject, htmlBody, pdfPath, attachmentName);
        loggingUtil.info("Agreement email sent to " + to + " with PDF: " + attachmentName, "ChitAgreementService");
    }
}
