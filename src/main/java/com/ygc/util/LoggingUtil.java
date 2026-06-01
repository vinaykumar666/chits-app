package com.ygc.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utility class for consistent logging with exception handling across the application
 * Provides methods for different log levels with structured error handling
 */
@Component
@Slf4j
public class LoggingUtil {

    /**
     * Log debug information with context
     */
    public void debug(String message, String context) {
        log.debug("[{}] {}", context, message);
    }

    /**
     * Log info with context
     */
    public void info(String message, String context) {
        log.info("[{}] {}", context, message);
    }

    /**
     * Log warning with context
     */
    public void warn(String message, String context) {
        log.warn("[{}] {}", context, message);
    }

    /**
     * Log error with context and exception
     */
    public void error(String message, String context, Exception exception) {
        log.error("[{}] {}", context, message, exception);
    }

    /**
     * Log error with context only
     */
    public void error(String message, String context) {
        log.error("[{}] {}", context, message);
    }

    /**
     * Log exception with full stack trace
     */
    public void logException(Exception exception, String context, String operation) {
        log.error("[{}] Exception during {}: {}", context, operation, exception.getMessage(), exception);
    }

    /**
     * Log method entry
     */
    public void methodEntry(String methodName, String context) {
        log.debug("[{}] --> Entering method: {}", context, methodName);
    }

    /**
     * Log method exit
     */
    public void methodExit(String methodName, String context) {
        log.debug("[{}] <-- Exiting method: {}", context, methodName);
    }

    /**
     * Log method execution with result
     */
    public void methodExecuted(String methodName, String context, String result) {
        log.debug("[{}] Method: {} completed with result: {}", context, methodName, result);
    }

    /**
     * Log transaction information
     */
    public void transactionStart(String transactionName, String context) {
        log.info("[{}] --> Starting transaction: {}", context, transactionName);
    }

    /**
     * Log transaction completion
     */
    public void transactionComplete(String transactionName, String context) {
        log.info("[{}] <-- Transaction completed: {}", context, transactionName);
    }

    /**
     * Log transaction failure
     */
    public void transactionFailed(String transactionName, String context, Exception exception) {
        log.error("[{}] Transaction FAILED: {} - {}", context, transactionName, exception.getMessage(), exception);
    }

    /**
     * Log user action
     */
    public void userAction(String userId, String action, String context) {
        log.info("[USER_ACTION][{}] {} performed action: {}", context, userId, action);
    }

    /**
     * Log business rule violation
     */
    public void businessRuleViolation(String rule, String context, String details) {
        log.warn("[BUSINESS_RULE][{}] Violation: {} - {}", context, rule, details);
    }

    /**
     * Log performance metric
     */
    public void performanceMetric(String operationName, long executionTimeMs, String context) {
        log.debug("[PERFORMANCE][{}] Operation: {} completed in {} ms", context, operationName, executionTimeMs);
    }

    /**
     * Log database operation
     */
    public void databaseOperation(String operation, String entity, String context) {
        log.debug("[DB_OPERATION][{}] {} on entity: {}", context, operation, entity);
    }

    /**
     * Log security event
     */
    public void securityEvent(String event, String details, String context) {
        log.warn("[SECURITY][{}] Event: {} - {}", context, event, details);
    }

    /**
     * Log API call
     */
    public void apiCall(String method, String endpoint, String context) {
        log.info("[API_CALL][{}] {} {}", context, method, endpoint);
    }

    /**
     * Log API response
     */
    public void apiResponse(String endpoint, int statusCode, String context) {
        log.info("[API_RESPONSE][{}] {} - Status: {}", context, endpoint, statusCode);
    }

    /**
     * Log external service call
     */
    public void externalServiceCall(String serviceName, String operation, String context) {
        log.info("[EXTERNAL_SERVICE][{}] --> Calling {} - {}", context, serviceName, operation);
    }

    /**
     * Log external service response
     */
    public void externalServiceResponse(String serviceName, boolean success, String context) {
        String status = success ? "SUCCESS" : "FAILURE";
        log.info("[EXTERNAL_SERVICE][{}] <-- {} response: {}", context, serviceName, status);
    }

    /**
     * Log validation error
     */
    public void validationError(String field, String error, String context) {
        log.warn("[VALIDATION][{}] Field: {} - {}", context, field, error);
    }

    /**
     * Log cache operation
     */
    public void cacheOperation(String operation, String key, String context) {
        log.debug("[CACHE][{}] {} - Key: {}", context, operation, key);
    }
}

