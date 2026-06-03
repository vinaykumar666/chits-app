package com.ygc.service;

import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

import java.io.File;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final LoggingUtil loggingUtil;

    @Async
    public void sendEmail(String to, String subject, String body) {
        sendHtmlEmail(to, subject, getPlainHtmlTemplate(subject, body));
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        sendHtmlEmailWithAttachment(to, subject, htmlBody, null, null);
    }

    /**
     * Send an HTML email with an optional file attachment.
     *
     * @param attachmentPath  absolute path to the file to attach (null = no attachment)
     * @param attachmentName  display name for the attachment (null = use filename)
     */
    @Async
    public void sendHtmlEmailWithAttachment(String to, String subject, String htmlBody,
                                             String attachmentPath, String attachmentName) {
        try {
            loggingUtil.externalServiceCall("JavaMailSender", "sendHtmlEmailWithAttachment", "EmailService");
            MimeMessage message = mailSender.createMimeMessage();
            // multipart=true required for attachments
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("[YGC Internal] " + subject);
            helper.setText(htmlBody, true);
            helper.setFrom("noreply@ygcinternal.com");

            if (attachmentPath != null) {
                File file = new File(attachmentPath);
                if (file.exists() && file.isFile()) {
                    String name = (attachmentName != null) ? attachmentName : file.getName();
                    helper.addAttachment(name, file);
                    loggingUtil.debug("Attachment added: " + name, "EmailService.sendHtmlEmailWithAttachment");
                } else {
                    loggingUtil.warn("Attachment file not found, sending without: " + attachmentPath,
                            "EmailService.sendHtmlEmailWithAttachment");
                }
            }

            mailSender.send(message);
            loggingUtil.debug("Email sent to: " + to, "EmailService");
        } catch (Exception e) {
            loggingUtil.error("Failed to send email to: " + to, "EmailService", e);
        }
    }

    private String getPlainHtmlTemplate(String subject, String body) {
        return "<html><body style='font-family: Arial; margin: 20px;'>" +
                "<div style='background: linear-gradient(135deg, #3366cc 0%, #1a4d99 100%); color: white; padding: 20px; text-align: center; border-radius: 5px;'>" +
                "<h1 style='margin: 0;'>YGC Internal</h1>" +
                "</div>" +
                "<div style='padding: 20px; border: 1px solid #ddd; margin-top: 10px; border-radius: 5px;'>" +
                "<h2>" + subject + "</h2>" +
                "<p style='color: #333;'>" + body.replace("\n", "<br>") + "</p>" +
                "</div>" +
                "<div style='text-align: center; margin-top: 20px; color: #999; font-size: 12px;'>" +
                "<p>Save Rupee, Rupee Will Save You In Future</p>" +
                "</div>" +
                "</body></html>";
    }

    public void sendRegistrationConfirmation(String email, String name, String tempPassword) {
        try {
            loggingUtil.debug("Sending registration confirmation to: " + email, "EmailService.sendRegistrationConfirmation");

            String body = "Dear " + name + ",\n\n" +
                    "Welcome to YGC Internal - Save Rupee, Rupee Will Save You In Future\n\n" +
                    "Your account has been created.\n" +
                    "Temporary Password: " + tempPassword + "\n" +
                    "Please login and change your password immediately.\n\n" +
                    "Contact: +91 8919508889\n" +
                    "Regards,\nYGC Internal Team";
            sendEmail(email, "Registration Confirmation", body);
            loggingUtil.info("Registration confirmation email queued", "EmailService.sendRegistrationConfirmation");
        } catch (Exception e) {
            loggingUtil.error("Error sending registration email", "EmailService.sendRegistrationConfirmation", e);
        }
    }

    public void sendPaymentDueReminder(String email, String name, String chitName, String dueDate, String amount) {
        try {
            loggingUtil.debug("Sending payment due reminder to: " + email, "EmailService.sendPaymentDueReminder");

            String body = "Dear " + name + ",\n\n" +
                    "This is a reminder that your payment for chit '" + chitName + "' is due on " + dueDate + ".\n" +
                    "Amount Due: ₹" + amount + "\n\n" +
                    "Please make the payment to avoid late fines of ₹20/day.\n\n" +
                    "Contact: +91 8919508889\n" +
                    "Regards,\nYGC Internal Team";
            sendEmail(email, "Payment Due Reminder - " + chitName, body);
            loggingUtil.info("Payment due reminder queued", "EmailService.sendPaymentDueReminder");
        } catch (Exception e) {
            loggingUtil.error("Error sending payment reminder", "EmailService.sendPaymentDueReminder", e);
        }
    }

    public void sendPaymentApproval(String email, String name, String chitName, String amount, boolean approved) {
        try {
            loggingUtil.debug("Sending payment " + (approved ? "approval" : "rejection") + " to: " + email, "EmailService.sendPaymentApproval");

            String status = approved ? "Approved" : "Rejected";
            String body = "Dear " + name + ",\n\n" +
                    "Your payment of ₹" + amount + " for chit '" + chitName + "' has been " + status + ".\n\n" +
                    "Contact: +91 8919508889\n" +
                    "Regards,\nYGC Internal Team";
            sendEmail(email, "Payment " + status + " - " + chitName, body);
            loggingUtil.info("Payment " + status + " email queued", "EmailService.sendPaymentApproval");
        } catch (Exception e) {
            loggingUtil.error("Error sending payment approval email", "EmailService.sendPaymentApproval", e);
        }
    }

    public void sendAuctionAnnouncement(String email, String name, String chitName, String auctionDate, int monthNumber) {
        try {
            loggingUtil.debug("Sending auction announcement to: " + email, "EmailService.sendAuctionAnnouncement");

            String body = "Dear " + name + ",\n\n" +
                    "The auction for Month " + monthNumber + " of chit '" + chitName + "' is scheduled on " + auctionDate + ".\n\n" +
                    "Please login to place your bid.\n\n" +
                    "Contact: +91 8919508889\n" +
                    "Regards,\nYGC Internal Team";
            sendEmail(email, "Auction Announcement - " + chitName + " Month " + monthNumber, body);
            loggingUtil.info("Auction announcement queued", "EmailService.sendAuctionAnnouncement");
        } catch (Exception e) {
            loggingUtil.error("Error sending auction announcement", "EmailService.sendAuctionAnnouncement", e);
        }
    }

    public void sendSettlementConfirmation(String email, String name, String chitName, String amount) {
        try {
            String html = getStyledEmailTemplate("✅ Settlement Approved — " + chitName,
                "Dear " + name + ",",
                "Your settlement for chit <strong>" + chitName + "</strong> has been processed successfully.",
                new String[][]{{"Settlement Amount", "₹" + amount}, {"Status", "Approved"}},
                "success");
            sendHtmlEmail(email, "Settlement Approved - " + chitName, html);
        } catch (Exception e) {
            loggingUtil.error("Error sending settlement email", "EmailService.sendSettlementConfirmation", e);
        }
    }

    public void sendPaymentRejected(String email, String name, String chitName, String amount, String reason) {
        try {
            String html = getStyledEmailTemplate("❌ Payment Rejected — " + chitName,
                "Dear " + name + ",",
                "Your payment of <strong>₹" + amount + "</strong> for chit <strong>" + chitName + "</strong> has been <span style='color:#dc3545'>rejected</span>.",
                new String[][]{{"Rejection Reason", reason}, {"Action Required", "Please resubmit or contact admin"}},
                "danger");
            sendHtmlEmail(email, "Payment Rejected - " + chitName, html);
        } catch (Exception e) {
            loggingUtil.error("Error sending payment rejection email", "EmailService.sendPaymentRejected", e);
        }
    }

    public void sendMembershipRejected(String email, String name, String chitName, String reason) {
        try {
            String html = getStyledEmailTemplate("❌ Chit Registration Rejected — " + chitName,
                "Dear " + name + ",",
                "Your registration request for chit <strong>" + chitName + "</strong> has been <span style='color:#dc3545'>rejected</span>.",
                new String[][]{{"Rejection Reason", reason}, {"Next Steps", "Please contact admin at +91 8919508889"}},
                "danger");
            sendHtmlEmail(email, "Registration Rejected - " + chitName, html);
        } catch (Exception e) {
            loggingUtil.error("Error sending membership rejection email", "EmailService.sendMembershipRejected", e);
        }
    }

    public void sendSettlementRejected(String email, String name, String chitName, String reason) {
        try {
            String html = getStyledEmailTemplate("❌ Settlement Rejected — " + chitName,
                "Dear " + name + ",",
                "Your settlement request for chit <strong>" + chitName + "</strong> has been <span style='color:#dc3545'>rejected</span>.",
                new String[][]{{"Rejection Reason", reason}, {"Next Steps", "Please contact admin at +91 8919508889"}},
                "danger");
            sendHtmlEmail(email, "Settlement Rejected - " + chitName, html);
        } catch (Exception e) {
            loggingUtil.error("Error sending settlement rejection email", "EmailService.sendSettlementRejected", e);
        }
    }

    public void sendAnnouncement(String email, String name, String title, String message) {
        try {
            String html = getStyledEmailTemplate("📢 " + title,
                "Dear " + name + ",",
                message,
                null,
                "info");
            sendHtmlEmail(email, "Announcement: " + title, html);
        } catch (Exception e) {
            loggingUtil.error("Error sending announcement email", "EmailService.sendAnnouncement", e);
        }
    }

    public void sendUserUpdated(String email, String name, String detail) {
        try {
            String html = getStyledEmailTemplate("ℹ Account Updated",
                "Dear " + name + ",",
                "Your account details have been updated by admin.",
                new String[][]{{"Change", detail}},
                "info");
            sendHtmlEmail(email, "Account Details Updated", html);
        } catch (Exception e) {
            loggingUtil.error("Error sending user update email", "EmailService.sendUserUpdated", e);
        }
    }

    public void sendChitUpdated(String email, String name, String chitName, String detail) {
        try {
            String html = getStyledEmailTemplate("ℹ Chit Updated — " + chitName,
                "Dear " + name + ",",
                "Details for your chit <strong>" + chitName + "</strong> have been updated.",
                new String[][]{{"Update", detail}},
                "info");
            sendHtmlEmail(email, "Chit Updated - " + chitName, html);
        } catch (Exception e) {
            loggingUtil.error("Error sending chit update email", "EmailService.sendChitUpdated", e);
        }
    }

    // ── Beautiful branded email template ─────────────────────────────────────
    private String getStyledEmailTemplate(String heading, String greeting,
                                           String body, String[][] rows, String type) {
        String accentColor = switch (type) {
            case "success" -> "#198754";
            case "danger"  -> "#dc3545";
            case "warning" -> "#f0a500";
            default        -> "#3366cc";
        };
        String accentLight = switch (type) {
            case "success" -> "#d1e7dd";
            case "danger"  -> "#f8d7da";
            case "warning" -> "#fff3cd";
            default        -> "#cfe2ff";
        };

        StringBuilder rowsHtml = new StringBuilder();
        if (rows != null) {
            rowsHtml.append("<table style='width:100%;border-collapse:collapse;margin-top:16px'>");
            for (String[] row : rows) {
                rowsHtml.append("<tr>")
                    .append("<td style='padding:8px 12px;background:#f8f9fa;font-weight:600;color:#495057;border-radius:6px 0 0 6px;width:40%'>").append(row[0]).append("</td>")
                    .append("<td style='padding:8px 12px;background:").append(accentLight).append(";color:#212529;border-radius:0 6px 6px 0'>").append(row[1]).append("</td>")
                    .append("</tr><tr><td colspan='2' style='height:4px'></td></tr>");
            }
            rowsHtml.append("</table>");
        }

        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6fb;font-family:Arial,sans-serif'>" +
            "<div style='max-width:580px;margin:32px auto;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,.1)'>" +
            // Header
            "<div style='background:linear-gradient(135deg,#1a1a2e 0%,#3366cc 100%);padding:32px 32px 24px;text-align:center'>" +
            "<div style='display:inline-block;background:" + accentColor + ";color:#fff;width:56px;height:56px;border-radius:50%;font-size:28px;font-weight:900;line-height:56px;margin-bottom:12px'>Y</div>" +
            "<div style='color:#ffffff;font-size:22px;font-weight:700;letter-spacing:1px'>YGC Internal</div>" +
            "<div style='color:rgba(255,255,255,.7);font-size:12px;margin-top:4px'>Save Rupee, Rupee Will Save You In Future</div>" +
            "</div>" +
            // Accent bar
            "<div style='height:4px;background:" + accentColor + "'></div>" +
            // Body
            "<div style='padding:32px'>" +
            "<h2 style='color:#1a1a2e;font-size:20px;margin:0 0 8px'>" + heading + "</h2>" +
            "<p style='color:#495057;margin:0 0 16px'>" + greeting + "</p>" +
            "<p style='color:#212529;line-height:1.7;margin:0'>" + body + "</p>" +
            rowsHtml +
            "</div>" +
            // Footer
            "<div style='background:#f8f9fa;padding:20px 32px;border-top:1px solid #e9ecef;text-align:center'>" +
            "<p style='margin:0;color:#6c757d;font-size:12px'>Questions? Call us at <strong>+91 8919508889</strong></p>" +
            "<p style='margin:4px 0 0;color:#adb5bd;font-size:11px'>© YGC Internal — This is an automated message</p>" +
            "</div></div></body></html>";
    }
}
