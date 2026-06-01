package com.ygc.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structured JSON logging utility.
 *
 * Output format:
 *   event=methodName() {"key":"value", ...}
 *
 * Example:
 *   event=submitPayment() {"membershipId":1,"monthNumber":2,"member":"user@ygc.com","status":"START"}
 *   event=submitPayment() {"membershipId":1,"monthNumber":2,"daysLate":3,"fine":60,"status":"FINE_APPLIED"}
 *   event=submitPayment() {"membershipId":1,"status":"SUCCESS"}
 *   event=submitPayment() {"membershipId":1,"error":"Membership not found","status":"FAILED"}
 */
@Component
@Slf4j
public class LoggingUtil {

    // ─────────────────────────────────────────────────────────────────
    // Core structured JSON builder
    // ─────────────────────────────────────────────────────────────────

    /**
     * Build the log line: event=methodName() {"k1":"v1","k2":"v2"}
     */
    private String build(String event, Map<String, Object> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("event=").append(event).append("() {");
        boolean first = true;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val == null) {
                sb.append("null");
            } else if (val instanceof Number || val instanceof Boolean) {
                sb.append(val);
            } else {
                // Escape quotes inside string values
                sb.append("\"").append(val.toString().replace("\"", "'")).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /** Convenience: single key-value */
    private String build(String event, String k1, Object v1) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        return build(event, m);
    }

    // ─────────────────────────────────────────────────────────────────
    // Transaction lifecycle
    // ─────────────────────────────────────────────────────────────────

    public void transactionStart(String method, String clazz) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("class", clazz);
        m.put("status", "START");
        log.info(build(method, m));
    }

    public void transactionComplete(String method, String clazz) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("class", clazz);
        m.put("status", "SUCCESS");
        log.info(build(method, m));
    }

    public void transactionFailed(String method, String clazz, Exception ex) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("class", clazz);
        m.put("error", ex.getMessage());
        m.put("status", "FAILED");
        log.error(build(method, m), ex);
    }

    // ─────────────────────────────────────────────────────────────────
    // Method-level structured logging
    // ─────────────────────────────────────────────────────────────────

    /**
     * Log method entry with input values.
     * Usage: loggingUtil.entry("submitPayment", "membershipId", membershipId, "monthNumber", monthNumber);
     * Supports up to 5 key-value pairs (varargs in pairs).
     */
    public void entry(String method, Object... kvPairs) {
        Map<String, Object> m = toMap("START", kvPairs);
        log.debug(build(method, m));
    }

    /**
     * Log method success with result values.
     * Usage: loggingUtil.success("submitPayment", "paymentId", saved.getId());
     */
    public void success(String method, Object... kvPairs) {
        Map<String, Object> m = toMap("SUCCESS", kvPairs);
        log.info(build(method, m));
    }

    /**
     * Log method failure with error.
     * Usage: loggingUtil.failure("submitPayment", ex, "membershipId", membershipId);
     */
    public void failure(String method, Exception ex, Object... kvPairs) {
        Map<String, Object> m = toMap("FAILED", kvPairs);
        m.put("error", ex.getMessage());
        log.error(build(method, m), ex);
    }

    /**
     * Log a named business event mid-method (fine calculation, email sent, etc).
     * Usage: loggingUtil.event("submitPayment", "LATE_FINE_APPLIED", "daysLate", 3, "fine", 60);
     */
    public void event(String method, String eventName, Object... kvPairs) {
        Map<String, Object> m = toMap(eventName, kvPairs);
        log.info(build(method, m));
    }

    // ─────────────────────────────────────────────────────────────────
    // Specialised structured log methods
    // ─────────────────────────────────────────────────────────────────

    /** DB operation: event=createChit() {"db_op":"INSERT","entity":"Chit","status":"DB"} */
    public void db(String method, String operation, String entity) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("db_op", operation);
        m.put("entity", entity);
        m.put("status", "DB");
        log.debug(build(method, m));
    }

    /** User action: event=dashboard() {"user":"admin@ygc.com","action":"DASHBOARD_ACCESS","status":"AUDIT"} */
    public void audit(String method, String user, String action) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("user", user);
        m.put("action", action);
        m.put("status", "AUDIT");
        log.info(build(method, m));
    }

    /** Security event: event=jwtFilter() {"event":"ACCESS_DENIED","detail":"...","status":"SECURITY"} */
    public void security(String method, String secEvent, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("event", secEvent);
        m.put("detail", detail);
        m.put("status", "SECURITY");
        log.warn(build(method, m));
    }

    /** Business rule violation: event=placeBid() {"rule":"BID_BELOW_MINIMUM","bid":100,"min":500,"status":"RULE_VIOLATION"} */
    public void ruleViolation(String method, String rule, Object... kvPairs) {
        Map<String, Object> m = toMap("RULE_VIOLATION", kvPairs);
        m.put("rule", rule);
        log.warn(build(method, m));
    }

    /** External service call: event=sendEmail() {"service":"JavaMailSender","op":"sendHtmlEmail","status":"EXT_CALL"} */
    public void externalCall(String method, String service, String operation) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("service", service);
        m.put("op", operation);
        m.put("status", "EXT_CALL");
        log.info(build(method, m));
    }

    /** Validation error: event=changePassword() {"field":"confirmPassword","error":"Passwords do not match","status":"VALIDATION_ERROR"} */
    public void validationError(String method, String field, String error) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("field", field);
        m.put("error", error);
        m.put("status", "VALIDATION_ERROR");
        log.warn(build(method, m));
    }

    /** Performance metric: event=getAllChits() {"durationMs":42,"status":"PERF"} */
    public void perf(String method, long durationMs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("durationMs", durationMs);
        m.put("status", "PERF");
        log.debug(build(method, m));
    }

    // ─────────────────────────────────────────────────────────────────
    // Legacy bridge methods — existing call sites still compile
    // ─────────────────────────────────────────────────────────────────

    public void debug(String message, String context) {
        log.debug("event=debug() {\"ctx\":\"{}\",\"msg\":\"{}\"}", context, message);
    }

    public void info(String message, String context) {
        log.info("event=info() {\"ctx\":\"{}\",\"msg\":\"{}\"}", context, message);
    }

    public void warn(String message, String context) {
        log.warn("event=warn() {\"ctx\":\"{}\",\"msg\":\"{}\"}", context, message);
    }

    public void error(String message, String context, Exception ex) {
        log.error("event=error() {\"ctx\":\"{}\",\"msg\":\"{}\"}", context, message, ex);
    }

    public void error(String message, String context) {
        log.error("event=error() {\"ctx\":\"{}\",\"msg\":\"{}\"}", context, message);
    }

    public void logException(Exception ex, String context, String operation) {
        log.error("event=exception() {\"ctx\":\"{}\",\"op\":\"{}\",\"error\":\"{}\"}", context, operation, ex.getMessage(), ex);
    }

    public void methodEntry(String method, String context) {
        log.debug("event={}() {\"ctx\":\"{}\",\"status\":\"START\"}", method, context);
    }

    public void methodExit(String method, String context) {
        log.debug("event={}() {\"ctx\":\"{}\",\"status\":\"EXIT\"}", method, context);
    }

    public void methodExecuted(String method, String context, String result) {
        log.debug("event={}() {\"ctx\":\"{}\",\"result\":\"{}\",\"status\":\"DONE\"}", method, context, result);
    }

    public void userAction(String user, String action, String context) {
        log.info("event=userAction() {\"ctx\":\"{}\",\"user\":\"{}\",\"action\":\"{}\",\"status\":\"AUDIT\"}", context, user, action);
    }

    public void businessRuleViolation(String rule, String context, String details) {
        log.warn("event=ruleViolation() {\"ctx\":\"{}\",\"rule\":\"{}\",\"detail\":\"{}\",\"status\":\"RULE_VIOLATION\"}", context, rule, details);
    }

    public void performanceMetric(String op, long ms, String context) {
        log.debug("event={}() {\"ctx\":\"{}\",\"durationMs\":{},\"status\":\"PERF\"}", op, context, ms);
    }

    public void databaseOperation(String op, String entity, String context) {
        log.debug("event=dbOp() {\"ctx\":\"{}\",\"op\":\"{}\",\"entity\":\"{}\",\"status\":\"DB\"}", context, op, entity);
    }

    public void securityEvent(String secEvent, String details, String context) {
        log.warn("event=security() {\"ctx\":\"{}\",\"event\":\"{}\",\"detail\":\"{}\",\"status\":\"SECURITY\"}", context, secEvent, details);
    }

    public void apiCall(String method, String endpoint, String context) {
        log.info("event=apiCall() {\"ctx\":\"{}\",\"method\":\"{}\",\"endpoint\":\"{}\",\"status\":\"REQUEST\"}", context, method, endpoint);
    }

    public void apiResponse(String endpoint, int status, String context) {
        log.info("event=apiResponse() {\"ctx\":\"{}\",\"endpoint\":\"{}\",\"httpStatus\":{},\"status\":\"RESPONSE\"}", context, endpoint, status);
    }

    public void externalServiceCall(String service, String op, String context) {
        log.info("event=extCall() {\"ctx\":\"{}\",\"service\":\"{}\",\"op\":\"{}\",\"status\":\"EXT_CALL\"}", context, service, op);
    }

    public void externalServiceResponse(String service, boolean success, String context) {
        log.info("event=extResponse() {\"ctx\":\"{}\",\"service\":\"{}\",\"success\":{},\"status\":\"EXT_RESPONSE\"}", context, service, success);
    }

    public void cacheOperation(String op, String key, String context) {
        log.debug("event=cache() {\"ctx\":\"{}\",\"op\":\"{}\",\"key\":\"{}\",\"status\":\"CACHE\"}", context, op, key);
    }

    // ─────────────────────────────────────────────────────────────────
    // Internal helper
    // ─────────────────────────────────────────────────────────────────

    /**
     * Convert vararg k/v pairs into an ordered map with a leading "status" key.
     * kvPairs must be (String key, Object value) pairs — odd length is ignored.
     */
    private Map<String, Object> toMap(String status, Object... kvPairs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status);
        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            m.put(String.valueOf(kvPairs[i]), kvPairs[i + 1]);
        }
        return m;
    }
}
