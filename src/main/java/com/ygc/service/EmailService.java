package com.ygc.service;

import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

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
        try {
            loggingUtil.externalServiceCall("JavaMailSender", "sendHtmlEmail", "EmailService");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("[YGC Internal] " + subject);
            helper.setText(htmlBody, true);
            helper.setFrom("noreply@ygcinternal.com");
            mailSender.send(message);
            loggingUtil.debug("HTML email sent to: " + to, "EmailService");
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
            loggingUtil.debug("Sending settlement confirmation to: " + email, "EmailService.sendSettlementConfirmation");

            String body = "Dear " + name + ",\n\n" +
                    "Your settlement for chit '" + chitName + "' has been processed.\n" +
                    "Settlement Amount: ₹" + amount + "\n\n" +
                    "Contact: +91 8919508889\n" +
                    "Regards,\nYGC Internal Team";
            sendEmail(email, "Settlement Confirmation - " + chitName, body);
            loggingUtil.info("Settlement confirmation email queued", "EmailService.sendSettlementConfirmation");
        } catch (Exception e) {
            loggingUtil.error("Error sending settlement email", "EmailService.sendSettlementConfirmation", e);
        }
    }
}
