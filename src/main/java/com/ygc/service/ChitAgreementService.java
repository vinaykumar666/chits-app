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

        PdfWriter writer = new PdfWriter(new FileOutputStream(filepath));
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);
        doc.setMargins(50, 50, 50, 50);

        DeviceRgb brandBlue = new DeviceRgb(51, 102, 204);
        DeviceRgb lightGray = new DeviceRgb(248, 249, 250);

        // ---- Header ----
        Paragraph header = new Paragraph("YGC INTERNAL")
                .setFontSize(26).setBold()
                .setFontColor(brandBlue)
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(header);

        Paragraph tagline = new Paragraph("Save Rupee, Rupee Will Save You In Future")
                .setFontSize(11).setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(tagline);

        Paragraph title = new Paragraph("CHIT FUND PARTICIPATION AGREEMENT")
                .setFontSize(16).setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(15).setMarginBottom(5);
        doc.add(title);

        Paragraph agreementNo = new Paragraph("Agreement No: " + membership.getAgreementNumber())
                .setFontSize(10).setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        doc.add(agreementNo);

        // Horizontal rule (simulated via an empty table with bottom border)
        Table hr = new Table(1).setWidth(UnitValue.createPercentValue(100));
        Cell hrCell = new Cell().setBorderTop(new SolidBorder(brandBlue, 2))
                .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 1))
                .setBorderLeft(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setBorderRight(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setHeight(4);
        hr.addCell(hrCell);
        doc.add(hr);
        doc.add(new Paragraph("\n"));

        // ---- Customer Details ----
        addSectionHeader(doc, "CUSTOMER DETAILS", brandBlue);
        Table customerTable = new Table(new float[]{200, 300}).setWidth(UnitValue.createPercentValue(100));
        addRow(customerTable, "Full Name", customer.getFullName());
        addRow(customerTable, "Email Address", customer.getEmail());
        addRow(customerTable, "Phone Number", customer.getPhone() != null ? customer.getPhone() : "—");
        addRow(customerTable, "Membership ID", String.valueOf(membership.getId()));
        doc.add(customerTable);
        doc.add(new Paragraph("\n"));

        // ---- Chit Details ----
        addSectionHeader(doc, "CHIT DETAILS", brandBlue);
        Table chitTable = new Table(new float[]{200, 300}).setWidth(UnitValue.createPercentValue(100));
        addRow(chitTable, "Chit Name", chit.getName());
        addRow(chitTable, "Monthly Contribution", "₹" + chit.getMonthlyAmount());
        addRow(chitTable, "Total Chit Value", "₹" + chit.getTotalChitValue());
        addRow(chitTable, "Duration", chit.getDurationMonths() + " months");
        addRow(chitTable, "Total Members", String.valueOf(chit.getTotalMembers()));
        addRow(chitTable, "Commission Rate", chit.getAdminCommissionPercentage() + "%");
        addRow(chitTable, "Start Date", chit.getStartDate().toString());
        addRow(chitTable, "End Date", chit.getEndDate() != null ? chit.getEndDate().toString() : "—");
        doc.add(chitTable);
        doc.add(new Paragraph("\n"));

        // ---- Terms & Conditions ----
        addSectionHeader(doc, "TERMS AND CONDITIONS", brandBlue);
        String[] terms = {
            "1. The subscriber agrees to pay monthly contributions of ₹" + chit.getMonthlyAmount()
                    + " on or before the due date each month.",
            "2. A late payment fine of ₹20 per day will be charged for payments received after the due date.",
            "3. The subscriber is eligible to participate in monthly auctions. The member who bids the lowest"
                    + " amount wins the chit prize.",
            "4. YGC Internal will deduct a commission of " + chit.getAdminCommissionPercentage()
                    + "% from the winning bid amount.",
            "5. Once a member wins an auction, they are not eligible to bid in subsequent auctions of the same chit.",
            "6. The subscriber authorises YGC Internal to process their personal information solely for chit operations.",
            "7. Early exit from the chit is subject to terms agreed with the administrator.",
            "8. All disputes shall be resolved as per the Chit Funds Act, 1982 and applicable regulations.",
            "9. This agreement is legally binding upon signing/digital acceptance."
        };
        for (String term : terms) {
            doc.add(new Paragraph(term).setFontSize(10).setMarginBottom(4));
        }
        doc.add(new Paragraph("\n"));

        // ---- Acceptance Details ----
        addSectionHeader(doc, "ACCEPTANCE RECORD", brandBlue);
        Table acceptTable = new Table(new float[]{200, 300}).setWidth(UnitValue.createPercentValue(100));
        addRow(acceptTable, "Agreement Read", membership.isAgreementRead() ? "✓ Yes" : "No");
        addRow(acceptTable, "Terms Accepted", membership.isAgreementAccepted() ? "✓ Yes" : "No");
        addRow(acceptTable, "Info Processing Auth.", membership.isInfoProcessingAuthorized() ? "✓ Yes" : "No");
        addRow(acceptTable, "Acceptance Timestamp",
                membership.getAgreementAcceptedAt() != null
                        ? membership.getAgreementAcceptedAt().format(dtf) : "—");
        addRow(acceptTable, "Approval Timestamp",
                membership.getAgreementApprovedAt() != null
                        ? membership.getAgreementApprovedAt().format(dtf) : "—");
        addRow(acceptTable, "Approved By", "YGC Internal Admin");
        doc.add(acceptTable);

        // ---- Footer ----
        doc.add(new Paragraph("\n\n"));
        Paragraph footer = new Paragraph(
                "This is a digitally generated agreement. Agreement No: " + membership.getAgreementNumber()
                + "\nGenerated on: " + LocalDateTime.now().format(dtf)
                + "\n\nYGC Internal | Contact: +91 8919508889 | Save Rupee, Rupee Will Save You In Future")
                .setFontSize(9).setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(footer);

        doc.close();
        loggingUtil.info("Agreement PDF created at: " + filepath, "ChitAgreementService");
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
