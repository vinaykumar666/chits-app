package com.ygc.service;

import com.ygc.model.ChitMembership;
import com.ygc.model.Payment;
import com.ygc.repository.ChitMembershipRepository;
import com.ygc.repository.PaymentRepository;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled jobs for proactive payment push notifications.
 *
 *  - 5 days before due: PAYMENT_REMINDER (monthly reminder)
 *  - On / after due date with no approved payment: PAYMENT_DUE_ALERT (overdue)
 *
 * Cron times:
 *  remindUpcoming  — runs daily at 08:00
 *  alertOverdue    — runs daily at 09:00
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReminderScheduler {

    private final ChitMembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final LoggingUtil loggingUtil;

    /**
     * Upcoming payment reminder — 5 days before due date.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void remindUpcomingPayments() {
        loggingUtil.event("remindUpcomingPayments", "SCHEDULER_START", "job", "PAYMENT_REMINDER");

        LocalDate reminderTarget = LocalDate.now().plusDays(5);

        List<Payment> upcoming = paymentRepository.findByDueDateAndStatus(
                reminderTarget, Payment.PaymentStatus.PENDING);

        for (Payment p : upcoming) {
            ChitMembership membership = p.getMembership();
            if (membership.getStatus() != ChitMembership.MembershipStatus.ACTIVE) continue;

            String email    = membership.getUser().getEmail();
            String name     = membership.getUser().getFullName();
            String chitName = membership.getChit().getName();
            String dueDate  = reminderTarget.toString();
            String amount   = p.getAmount().toPlainString();

            // Email reminder
            emailService.sendPaymentDueReminder(email, name, chitName, dueDate, amount);

            // Push notification
            notificationService.notifyPaymentReminder(email, chitName, dueDate, amount);

            loggingUtil.event("remindUpcomingPayments", "REMINDER_SENT",
                    "user", email, "chit", chitName, "dueDate", dueDate);
        }

        loggingUtil.event("remindUpcomingPayments", "SCHEDULER_DONE",
                "remindersCount", upcoming.size());
    }

    /**
     * Overdue payment alert — fires for payments past their due date with no approval.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void alertOverduePayments() {
        loggingUtil.event("alertOverduePayments", "SCHEDULER_START", "job", "PAYMENT_DUE_ALERT");

        LocalDate today = LocalDate.now();

        List<Payment> overdue = paymentRepository.findByStatusAndDueDateBefore(
                Payment.PaymentStatus.PENDING, today);

        for (Payment p : overdue) {
            ChitMembership membership = p.getMembership();
            if (membership.getStatus() != ChitMembership.MembershipStatus.ACTIVE) continue;

            String email    = membership.getUser().getEmail();
            String chitName = membership.getChit().getName();
            int daysOverdue = (int) (today.toEpochDay() - p.getDueDate().toEpochDay());

            notificationService.notifyPaymentDueAlert(email, chitName, daysOverdue);

            loggingUtil.event("alertOverduePayments", "OVERDUE_ALERT_SENT",
                    "user", email, "chit", chitName, "daysOverdue", daysOverdue);
        }

        loggingUtil.event("alertOverduePayments", "SCHEDULER_DONE",
                "alertsCount", overdue.size());
    }
}
