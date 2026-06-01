package com.ygc.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * In-memory notification payload pushed to the client via SSE.
 * Not persisted to DB – notifications are ephemeral push events.
 */
public class Notification {

    public enum Type {
        CHIT_REGISTRATION_APPROVED,
        CHIT_REGISTRATION_REJECTED,
        BID_WINDOW_OPEN,
        BID_SUBMITTED,
        BID_WINNER_ANNOUNCED,
        PAYMENT_REMINDER,
        PAYMENT_DUE_ALERT,
        CHIT_MATURITY,
        AGREEMENT_APPROVED
    }

    private final String id;
    private final Type type;
    private final String title;
    private final String message;
    private final String targetUser;   // email – null means broadcast to all
    private final String chitName;
    private final LocalDateTime createdAt;

    public Notification(Type type, String title, String message,
                        String targetUser, String chitName) {
        this.id = java.util.UUID.randomUUID().toString();
        this.type = type;
        this.title = title;
        this.message = message;
        this.targetUser = targetUser;
        this.chitName = chitName;
        this.createdAt = LocalDateTime.now();
    }

    /** Serialise to a minimal JSON string for SSE data field. */
    public String toJson() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        return "{" +
               "\"id\":\"" + id + "\"," +
               "\"type\":\"" + type.name() + "\"," +
               "\"title\":\"" + esc(title) + "\"," +
               "\"message\":\"" + esc(message) + "\"," +
               "\"chitName\":\"" + esc(chitName != null ? chitName : "") + "\"," +
               "\"createdAt\":\"" + createdAt.format(fmt) + "\"" +
               "}";
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ---- Getters ----
    public String getId()          { return id; }
    public Type getType()          { return type; }
    public String getTitle()       { return title; }
    public String getMessage()     { return message; }
    public String getTargetUser()  { return targetUser; }
    public String getChitName()    { return chitName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
