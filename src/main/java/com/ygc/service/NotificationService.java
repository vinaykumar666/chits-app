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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Real-time push notification service using Server-Sent Events (SSE).
 *
 * Each authenticated user subscribes with their email as key.
 * Notifications can be:
 *   - targeted  (targetUser = email)  → delivered only to that user
 *   - broadcast (targetUser = null)   → delivered to all connected users
 *
 * No extra infra (no WebSocket upgrade, no message broker) is required;
 * SSE works over plain HTTP/1.1 with long-polling fallback in browsers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final LoggingUtil loggingUtil;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    /** user-email → list of active SSE emitters (user may have multiple tabs) */
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────
    // SSE subscription management
    // ─────────────────────────────────────────────────────────────────

    /**
     * Called by {@link com.ygc.controller.NotificationController} when a client
     * opens the SSE stream.
     */
    public SseEmitter subscribe(String userEmail) {
        // Keep stream open for long-lived Render connections.
        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(userEmail, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> removeEmitter(userEmail, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        scheduleHeartbeat(userEmail, emitter);

        loggingUtil.event("subscribe", "SSE_CONNECTED", "user", userEmail,
                "activeConnections", countConnections());
        return emitter;
    }

    private void scheduleHeartbeat(String userEmail, SseEmitter emitter) {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (!isEmitterActive(userEmail, emitter)) return;
            try {
                emitter.send(SseEmitter.event().name("PING").data("keepalive"));
            } catch (IOException e) {
                removeEmitter(userEmail, emitter);
                loggingUtil.warn("SSE heartbeat failed for: " + userEmail, "NotificationService");
            }
        }, 20, 20, TimeUnit.SECONDS);
    }

    private boolean isEmitterActive(String userEmail, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userEmail);
        return list != null && list.contains(emitter);
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

    // ─────────────────────────────────────────────────────────────────
    // Push helpers
    // ─────────────────────────────────────────────────────────────────

    /** Push a notification to a specific user (or broadcast if targetUser is null). */
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
                loggingUtil.warn("Dead SSE emitter removed for: " + userEmail, "NotificationService");
            }
        }
        list.removeAll(dead);
        if (list.isEmpty()) emitters.remove(userEmail);
    }

    // ─────────────────────────────────────────────────────────────────
    // Notification factory methods (called from services/scheduled jobs)
    // ─────────────────────────────────────────────────────────────────

    public void notifyChitRegistrationApproved(String userEmail, String userName, String chitName) {
        push(new Notification(
                Notification.Type.CHIT_REGISTRATION_APPROVED,
                "Chit Registration Approved",
                "Your registration for chit '" + chitName + "' has been approved. Welcome aboard!",
                userEmail, chitName));
    }

    public void notifyChitRegistrationRejected(String userEmail, String userName, String chitName) {
        push(new Notification(
                Notification.Type.CHIT_REGISTRATION_REJECTED,
                "Chit Registration Rejected",
                "Your registration request for chit '" + chitName + "' was not approved. Please contact admin.",
                userEmail, chitName));
    }

    public void notifyBidWindowOpen(String userEmail, String chitName, int monthNumber) {
        push(new Notification(
                Notification.Type.BID_WINDOW_OPEN,
                "Bid Window Open",
                "The bid window for Month " + monthNumber + " of chit '" + chitName + "' is now open. Login to place your bid!",
                userEmail, chitName));
    }

    public void notifyBidSubmitted(String userEmail, String chitName, String bidAmount) {
        push(new Notification(
                Notification.Type.BID_SUBMITTED,
                "Bid Submitted",
                "Your bid of ₹" + bidAmount + " for chit '" + chitName + "' has been received.",
                userEmail, chitName));
    }

    public void notifyBidWinnerAnnounced(String userEmail, String chitName, int monthNumber, boolean isWinner) {
        String msg = isWinner
                ? "Congratulations! You won the auction for Month " + monthNumber + " of chit '" + chitName + "'."
                : "The winner for Month " + monthNumber + " of chit '" + chitName + "' has been announced.";
        push(new Notification(
                Notification.Type.BID_WINNER_ANNOUNCED,
                isWinner ? "You Won the Auction!" : "Auction Winner Announced",
                msg, userEmail, chitName));
    }

    public void notifyPaymentReminder(String userEmail, String chitName, String dueDate, String amount) {
        push(new Notification(
                Notification.Type.PAYMENT_REMINDER,
                "Monthly Payment Reminder",
                "Payment of ₹" + amount + " for chit '" + chitName + "' is due on " + dueDate + ".",
                userEmail, chitName));
    }

    public void notifyPaymentDueAlert(String userEmail, String chitName, int daysOverdue) {
        push(new Notification(
                Notification.Type.PAYMENT_DUE_ALERT,
                "⚠ Overdue Payment Alert",
                "Your payment for chit '" + chitName + "' is " + daysOverdue + " day(s) overdue. A fine of ₹20/day is being charged.",
                userEmail, chitName));
    }

    public void notifyChitMaturity(String userEmail, String chitName) {
        push(new Notification(
                Notification.Type.CHIT_MATURITY,
                "Chit Fund Matured",
                "Your chit '" + chitName + "' has completed all cycles. Final settlement is being processed.",
                userEmail, chitName));
    }

    public void notifyAgreementApproved(String userEmail, String chitName, String agreementNumber) {
        push(new Notification(
                Notification.Type.AGREEMENT_APPROVED,
                "Agreement Approved",
                "Your chit agreement (" + agreementNumber + ") for '" + chitName + "' has been approved. The signed PDF has been emailed to you.",
                userEmail, chitName));
    }
}
