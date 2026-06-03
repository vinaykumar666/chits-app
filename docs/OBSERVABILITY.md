# 100% Observability Implementation Guide

## Overview
This document outlines the comprehensive logging, exception handling, and observability implementation for the YGC Internal Chit Management System.

## Architecture

### 1. Logging Infrastructure

#### Logback Configuration (`logback-spring.xml`)
- **Console Appender**: Real-time log output to console
- **File Appender**: Rolling file logs with size-based rotation (10MB per file, 30-day retention)
- **Async Appender**: Non-blocking asynchronous logging for better performance
- **Error File Appender**: Separate error log file for quick error tracking

#### Log Levels
- **Root**: INFO
- **Application Code (com.ygc)**: DEBUG
- **Spring Framework**: DEBUG (web & security), INFO (other)
- **Hibernate**: DEBUG (SQL), TRACE (parameter binding)

#### Log Output
```
logs/
├── application.log          # Main application logs (rolling)
├── error.log                # Error logs only (rolling)
├── application.YYYY-MM-DD.N.log
└── error.YYYY-MM-DD.N.log
```

### 2. Custom Exception Handling

#### Exception Classes
Located in `com.ygc.exception` package:

1. **EntityNotFoundException**: Resource not found (404)
2. **AccessDeniedException**: Access denied (403)
3. **DuplicateResourceException**: Duplicate resource (409)
4. **ValidationException**: Validation failures (400)

#### Global Exception Handler (`GlobalExceptionHandler`)
- Centralized exception handling across all controllers
- Consistent error response format
- Logs all exceptions with full stack traces
- Returns structured JSON error responses

#### Error Response Format
```json
{
  "timestamp": "2026-06-02T10:30:00",
  "status": 400,
  "error": "Invalid Request",
  "message": "Detailed error message",
  "path": "/api/endpoint"
}
```

### 3. Logging Utility (`LoggingUtil`)

Provides consistent logging methods across the application:

#### Method Categories

**Transaction Logging**
```java
loggingUtil.transactionStart("methodName", "ServiceName");
loggingUtil.transactionComplete("methodName", "ServiceName");
loggingUtil.transactionFailed("methodName", "ServiceName", exception);
```

**Database Operations**
```java
loggingUtil.databaseOperation("INSERT/UPDATE/SELECT/DELETE", "EntityName", "Context");
```

**User Actions**
```java
loggingUtil.userAction("userEmail", "ACTION_NAME", "Context");
```

**Business Rules**
```java
loggingUtil.businessRuleViolation("RULE_NAME", "Context", "Details");
```

**External Services**
```java
loggingUtil.externalServiceCall("ServiceName", "Operation", "Context");
loggingUtil.externalServiceResponse("ServiceName", success, "Context");
```

**Validation**
```java
loggingUtil.validationError("fieldName", "Error message", "Context");
```

**Performance**
```java
loggingUtil.performanceMetric("Operation", executionTimeMs, "Context");
```

**Security**
```java
loggingUtil.securityEvent("EVENT_TYPE", "Details", "Context");
```

### 4. Request/Response Logging Filter

#### RequestResponseLoggingFilter
- Logs all HTTP requests and responses
- Generates unique request ID for request tracing
- Tracks request duration
- Logs performance metrics for slow requests (>1s)
- Adds request ID to response headers (`X-Request-ID`)

#### Log Entry Format
```
[REQUEST_ID] GET /api/endpoint
[REQUEST_ID] Query String: param=value
[REQUEST_ID] Content Type: application/json
[REQUEST_ID] --> Response Status: 200
[REQUEST_ID] Response Time: 250 ms
```

### 5. Service Layer Logging

All services include comprehensive logging:
- Method entry/exit
- Transaction start/complete/failed
- Database operations
- Business rule violations
- Error handling with exceptions
- User actions

**Covered Services**:
- UserService
- ChitService
- PaymentService
- AuctionService
- EmailService

### 6. Controller Layer Logging

All controllers include:
- Request parameter logging
- User action tracking
- Error logging
- Validation logging
- Security event logging

**Covered Controllers**:
- AuthController
- MemberController
- AdminController (can be extended)

## Log Examples

### Successful User Registration
```
2026-06-02 10:30:15.234 [main] DEBUG com.ygc.service.UserService - [UserService.registerUser] Registering user with email: user@example.com
2026-06-02 10:30:15.245 [main] DEBUG com.ygc.service.UserService - [UserService.registerUser] Database operation: INSERT on User
2026-06-02 10:30:15.456 [main] INFO  com.ygc.service.UserService - [USER_ACTION][UserService.registerUser] user@example.com performed action: REGISTRATION
```

### Payment Processing with Late Fine
```
2026-06-02 10:31:00.123 [main] DEBUG com.ygc.service.PaymentService - [PaymentService.submitPayment] Submitting payment for month: 1
2026-06-02 10:31:00.234 [main] INFO  com.ygc.service.PaymentService - [PaymentService.submitPayment] Payment is 5 days late. Fine: 100
2026-06-02 10:31:00.345 [main] DEBUG com.ygc.service.PaymentService - [DB_OPERATION][PaymentService.submitPayment] INSERT on Payment
2026-06-02 10:31:00.456 [main] INFO  com.ygc.service.PaymentService - [USER_ACTION][PaymentService.submitPayment] user@example.com performed action: SUBMIT_PAYMENT
```

### Business Rule Violation
```
2026-06-02 10:32:00.123 [main] WARN  com.ygc.service.ChitService - [BUSINESS_RULE][ChitService.requestJoin] Violation: CHIT_FULL - Current: 50, Total: 50
```

### Exception Handling
```
2026-06-02 10:33:00.123 [main] ERROR com.ygc.exception.GlobalExceptionHandler - [GLOBAL_HANDLER] Exception during Request Processing: User not found: 999
java.lang.RuntimeException: User not found: 999
  at com.ygc.service.UserService.findById(UserService.java:120)
  at com.ygc.controller.MemberController.getProfile(MemberController.java:45)
  ...
```

## Monitoring & Analysis

### Log File Locations
- **Main Logs**: `logs/application.log`
- **Error Logs**: `logs/error.log`
- **Archived**: `logs/application.YYYY-MM-DD.N.log`

### Log Rotation Policy
- File size: 10MB per file
- Time-based: Daily rotation
- Retention: 30 days
- Total size cap: 1GB

### Log Parsing & Analysis
Use standard Unix tools:

```bash
# View recent errors
tail -f logs/error.log

# Search for specific user actions
grep "USER_ACTION" logs/application.log | grep "user@example.com"

# Find all database operations
grep "DB_OPERATION" logs/application.log

# Track performance metrics
grep "PERFORMANCE" logs/application.log

# Monitor business rule violations
grep "BUSINESS_RULE" logs/application.log

# View request tracing
grep "REQUEST_ID" logs/application.log
```

## Best Practices

### 1. Always Log Context
```java
// Good
loggingUtil.info("Payment received", "PaymentService.processPayment");

// Bad
log.info("Payment received");
```

### 2. Log at Appropriate Levels
- **DEBUG**: Detailed method flow, parameter values
- **INFO**: Transaction start/complete, user actions, business events
- **WARN**: Business rule violations, recoverable errors
- **ERROR**: Exceptions, critical failures

### 3. Use Structured Context
```java
// Good - Clear context
loggingUtil.debug("Payment amount: " + amount + ", Chit: " + chitId, 
    "PaymentService.submitPayment");

// Better - Use audit service for critical actions
auditService.log(user, "SUBMIT_PAYMENT", "Payment", paymentId, details);
```

### 4. Handle Non-Critical Failures Gracefully
```java
try {
    emailService.sendEmail(email, subject, body);
} catch (Exception e) {
    loggingUtil.error("Failed to send email", "Context", e);
    // Continue - email is non-critical
}
```

### 5. Log Performance Metrics
```java
long startTime = System.currentTimeMillis();
// ... operation ...
long duration = System.currentTimeMillis() - startTime;
loggingUtil.performanceMetric("OperationName", duration, "Context");
```

## Integration with Monitoring Tools

### Render Deployment
- Logs are written to `logs/` directory
- Monitor via Render Dashboard → Logs
- Set up log aggregation for production

### Local Development
- Console logs provide real-time feedback
- File logs in `logs/` directory
- Use tail command to monitor: `tail -f logs/application.log`

### Production Monitoring
- Consider integrating with:
  - Datadog
  - New Relic
  - ELK Stack (Elasticsearch, Logstash, Kibana)
  - Splunk
  - CloudWatch (if using AWS)

## Configuration Customization

### Adjust Log Levels (application-prod.properties)
```properties
logging.level.com.ygc=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

### Disable/Enable Async Logging (logback-spring.xml)
```xml
<!-- Change discardingThreshold to drop logs on overflow -->
<discardingThreshold>20000</discardingThreshold>
```

### Adjust File Rotation (logback-spring.xml)
```xml
<!-- Change max file size -->
<maxFileSize>50MB</maxFileSize>

<!-- Change retention period -->
<maxHistory>90</maxHistory>

<!-- Change total size cap -->
<totalSizeCap>5GB</totalSizeCap>
```

## Troubleshooting

### Issue: Logs not appearing
**Solution**: 
- Check log level configuration in `application.properties`
- Ensure `logback-spring.xml` is in classpath
- Verify `logs/` directory exists and has write permissions

### Issue: Performance degradation due to logging
**Solution**:
- Use async appenders (already configured)
- Reduce log levels in production
- Increase async queue size in `logback-spring.xml`

### Issue: Log files consuming too much space
**Solution**:
- Reduce retention period in `logback-spring.xml`
- Reduce max file size
- Implement log cleanup script

## Conclusion

This comprehensive logging and observability implementation provides:
- ✅ **100% Observability**: Every operation is logged
- ✅ **Exception Tracking**: Full stack traces and context
- ✅ **Performance Monitoring**: Execution time tracking
- ✅ **Business Intelligence**: Business rule violations and user actions logged
- ✅ **Security**: All security events logged
- ✅ **Debugging**: Request tracing with unique IDs
- ✅ **Production Ready**: Async logging, file rotation, performance optimized

