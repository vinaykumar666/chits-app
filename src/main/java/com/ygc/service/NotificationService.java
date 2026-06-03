package com.ygc.service;

import com.ygc.model.Notification;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final LoggingUtil loggingUtil;

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // ── SSE subscription ────────────────────────────────────────────────────
    public SseEmitter subscribe(String userEmail) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        emitters.computeIfAbsent(userEmail, k -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> removeEmitter(userEmail, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        loggingUtil.event("subscribe", "SSE_CONNECTED", "user", userEmail, "activeConnections", countConnections());
        return emitter;
    }

    private void removeEmitter(String userEmail, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userEmail);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emitters.remove(userEmail);
        }
    }

    private long countConnections() {
        return emitters.values().stream().mapToLong(List::size).sum();
    }

    // ── Core push ───────────────────────────────────────────────────────────
    public void push(Notification notification) {
        loggingUtil.event("push", "NOTIFICATION_PUSH",
                "type", notification.getType(),
                "target", notification.getTargetUser() != null ? notification.getTargetUser() : "BROADCAST",
                "chit", notification.getChitName());
        if (notification.getTargetUser() != null) {
            pushToUser(notification.getTargetUser(), notification);
        } else {
            emitters.forEach((user, list) -> pushToUser(user, notification));
        }
    }

    private void pushToUser(String userEmail, Notification notification) {
        List<SseEmitter> list = emitters.get(userEmail);
        if (list == null || list.isEmpty()) return;
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .id(notification.getId())
                        .name(notification.getType().name())
                        .data(notification.toJson()));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
        if (list.isEmpty()) emitters.remove(userEmail);
    }

    // ── Factory methods ─────────────────────────────────────────────────────

    public void notifyChitRegistrationApproved(String userEmail, String userName, String chitName) {
        push(new Notification(Notification.Type.CHIT_REGISTRATION_APPROVED,
                "✅ Chit Registration Approved",
                "Your registration for '" + chitName + "' has been approved. Welcome aboard!",
                userEmail, chitName));
    }

    public void notifyChitRegistrationRejected(String userEmail, String userName, String chitName, String reason) {
        push(new Notification(Notification.Type.CHIT_REGISTRATION_REJECTED,
                "❌ Chit Registration Rejected",
                "Your registration for '" + chitName + "' was rejected. Reason: " + reason,
                userEmail, chitName));
    }

    /** Backward compat overload */
    public void notifyChitRegistrationRejected(String userEmail, String userName, String chitName) {
        notifyChitRegistrationRejected(userEmail, userName, chitName, "Please contact admin for details.");
    }

    public void notifyPaymentApproved(String userEmail, String chitName, String amount) {
        push(new Notification(Notification.Type.PAYMENT_APPROVED,
                "✅ Payment Approved",
                "Your payment of ₹" + amount + " for '" + chitName + "' has been approved.",
                userEmail, chitName));
    }

    public void notifyPaymentRejected(String userEmail, String chitName, String amount, String reason) {
        push(new Notification(Notification.Type.PAYMENT_REJECTED,
                "❌ Payment Rejected",
                "Your payment of ₹" + amount + " for '" + chitName + "' was rejected. Reason: " + reason,
                userEmail, chitName));
    }

    public void notifySettlementApproved(String userEmail, String chitName, String amount) {
        push(new Notification(Notification.Type.SETTLEMENT_APPROVED,
                "✅ Settlement Approved",
                "Your settlement for '" + chitName + "' of ₹" + amount + " has been approved.",
                userEmail, chitName));
    }

    public void notifySettlementRejected(String userEmail, String chitName, String reason) {
        push(new Notification(Notification.Type.SETTLEMENT_REJECTED,
                "❌ Settlement Rejected",
                "Your settlement request for '" + chitName + "' was rejected. Reason: " + reason,
                userEmail, chitName));
    }

    public void notifyEarlyExitSubmitted(String adminEmail, String memberName, String chitName) {
        push(new Notification(Notification.Type.EARLY_EXIT_SUBMITTED,
                "⚠ Early Exit Request",
                memberName + " has submitted an early exit request for '" + chitName + "'.",
                adminEmail, chitName));
    }

    public void notifyAnnouncement(String title, String message) {
        // Broadcast to all connected users
        push(new Notification(Notification.Type.ANNOUNCEMENT, "📢 " + title, message, null, null));
    }

    public void notifyUserUpdated(String userEmail, String detail) {
        push(new Notification(Notification.Type.USER_UPDATED,
                "ℹ Profile Updated",
                "Your account details have been updated by admin. " + detail,
                userEmail, null));
    }

    public void notifyChitUpdated(String userEmail, String chitName, String detail) {
        push(new Notification(Notification.Type.CHIT_UPDATED,
                "ℹ Chit Updated",
                "Details for chit '" + chitName + "' have been updated. " + detail,
                userEmail, chitName));
    }

    public void notifyBidWindowOpen(String userEmail, String chitName, int monthNumber) {
        push(new Notification(Notification.Type.BID_WINDOW_OPEN,
                "🔔 Bid Window Open",
                "Bid window for Month " + monthNumber + " of '" + chitName + "' is now open!",
                userEmail, chitName));
    }

    public void notifyBidSubmitted(String userEmail, String chitName, String bidAmount) {
        push(new Notification(Notification.Type.BID_SUBMITTED,
                "Bid Submitted",
                "Your bid of ₹" + bidAmount + " for '" + chitName + "' has been received.",
                userEmail, chitName));
    }

    public void notifyBidWinnerAnnounced(String userEmail, String chitName, int monthNumber, boolean isWinner) {
        String msg = isWinner
                ? "🎉 You won the auction for Month " + monthNumber + " of '" + chitName + "'!"
                : "The winner for Month " + monthNumber + " of '" + chitName + "' has been announced.";
        push(new Notification(Notification.Type.BID_WINNER_ANNOUNCED,
                isWinner ? "🏆 You Won the Auction!" : "Auction Winner Announced",
                msg, userEmail, chitName));
    }

    public void notifyPaymentReminder(String userEmail, String chitName, String dueDate, String amount) {
        push(new Notification(Notification.Type.PAYMENT_REMINDER,
                "💰 Payment Reminder",
                "Payment of ₹" + amount + " for '" + chitName + "' is due on " + dueDate + ".",
                userEmail, chitName));
    }

    public void notifyPaymentDueAlert(String userEmail, String chitName, int daysOverdue) {
        push(new Notification(Notification.Type.PAYMENT_DUE_ALERT,
                "⚠ Overdue Payment",
                "Payment for '" + chitName + "' is " + daysOverdue + " day(s) overdue. Fine: ₹20/day.",
                userEmail, chitName));
    }

    public void notifyChitMaturity(String userEmail, String chitName) {
        push(new Notification(Notification.Type.CHIT_MATURITY,
                "🎊 Chit Matured",
                "Your chit '" + chitName + "' has completed all cycles. Final settlement processing.",
                userEmail, chitName));
    }

    public void notifyAgreementApproved(String userEmail, String chitName, String agreementNumber) {
        push(new Notification(Notification.Type.AGREEMENT_APPROVED,
                "✅ Agreement Approved",
                "Your chit agreement (" + agreementNumber + ") for '" + chitName + "' is approved. PDF emailed.",
                userEmail, chitName));
    }
}
