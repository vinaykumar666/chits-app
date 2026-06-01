package com.ygc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[YGC Internal] " + subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Email send failed (non-critical): " + e.getMessage());
        }
    }

    public void sendRegistrationConfirmation(String email, String name, String tempPassword) {
        String body = "Dear " + name + ",\n\n" +
                "Welcome to YGC Internal - Save Rupee, Rupee Will Save You In Future\n\n" +
                "Your account has been created.\n" +
                "Temporary Password: " + tempPassword + "\n" +
                "Please login and change your password immediately.\n\n" +
                "Contact: +91 8919508889\n" +
                "Regards,\nYGC Internal Team";
        sendEmail(email, "Registration Confirmation", body);
    }

    public void sendPaymentDueReminder(String email, String name, String chitName, String dueDate, String amount) {
        String body = "Dear " + name + ",\n\n" +
                "This is a reminder that your payment for chit '" + chitName + "' is due on " + dueDate + ".\n" +
                "Amount Due: \u20b9" + amount + "\n\n" +
                "Please make the payment to avoid late fines of \u20b920/day.\n\n" +
                "Contact: +91 8919508889\n" +
                "Regards,\nYGC Internal Team";
        sendEmail(email, "Payment Due Reminder - " + chitName, body);
    }

    public void sendPaymentApproval(String email, String name, String chitName, String amount, boolean approved) {
        String status = approved ? "Approved" : "Rejected";
        String body = "Dear " + name + ",\n\n" +
                "Your payment of \u20b9" + amount + " for chit '" + chitName + "' has been " + status + ".\n\n" +
                "Contact: +91 8919508889\n" +
                "Regards,\nYGC Internal Team";
        sendEmail(email, "Payment " + status + " - " + chitName, body);
    }

    public void sendAuctionAnnouncement(String email, String name, String chitName, String auctionDate, int monthNumber) {
        String body = "Dear " + name + ",\n\n" +
                "The auction for Month " + monthNumber + " of chit '" + chitName + "' is scheduled on " + auctionDate + ".\n\n" +
                "Please login to place your bid.\n\n" +
                "Contact: +91 8919508889\n" +
                "Regards,\nYGC Internal Team";
        sendEmail(email, "Auction Announcement - " + chitName + " Month " + monthNumber, body);
    }

    public void sendSettlementConfirmation(String email, String name, String chitName, String amount) {
        String body = "Dear " + name + ",\n\n" +
                "Your settlement for chit '" + chitName + "' has been processed.\n" +
                "Settlement Amount: \u20b9" + amount + "\n\n" +
                "Contact: +91 8919508889\n" +
                "Regards,\nYGC Internal Team";
        sendEmail(email, "Settlement Confirmation - " + chitName, body);
    }
}
