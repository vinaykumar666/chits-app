# 100% Observability - Quick Reference

## 🚀 Quick Start

### View Logs
```bash
# Main application logs
tail -f logs/application.log

# Error logs only  
tail -f logs/error.log

# Search for errors
grep ERROR logs/application.log

# Search for user actions
grep USER_ACTION logs/application.log
```

## 🔍 Logging Levels

| Level | Usage | File Output |
|-------|-------|------------|
| DEBUG | Detailed method flow, parameters | application.log |
| INFO | Transactions, user actions, events | application.log |
| WARN | Business rule violations | application.log |
| ERROR | Exceptions, critical failures | application.log + error.log |

## 📝 Common Log Patterns

### Request with ID
```
[REQUEST-UUID] GET /api/endpoint
[REQUEST-UUID] Response Status: 200
[REQUEST-UUID] Response Time: 250 ms
```

### User Action
```
[USER_ACTION][ServiceName] user@email.com performed action: ACTION_NAME
```

### Transaction
```
[ServiceName.method] --> Starting transaction: methodName
[ServiceName.method] <-- Transaction completed: methodName
[ServiceName.method] FAILED Transaction: methodName
```

### Database Operation
```
[DB_OPERATION][ServiceName.method] SELECT/INSERT/UPDATE/DELETE on Entity
```

### Business Rule Violation
```
[BUSINESS_RULE][ServiceName.method] Violation: RULE_NAME - Details
```

## 💡 Using LoggingUtil

```java
// In any @Service or @Component class
@RequiredArgsConstructor
public class MyService {
    private final LoggingUtil loggingUtil;
    
    public void myMethod() {
        // Transaction logging
        loggingUtil.transactionStart("myMethod", "MyService");
        try {
            loggingUtil.debug("Processing data", "MyService.myMethod");
            loggingUtil.databaseOperation("INSERT", "Entity", "MyService.myMethod");
            loggingUtil.userAction(email, "ACTION_NAME", "MyService.myMethod");
            loggingUtil.transactionComplete("myMethod", "MyService");
        } catch (Exception e) {
            loggingUtil.transactionFailed("myMethod", "MyService", e);
            throw e;
        }
    }
}
```

## 🛡️ Exception Handling

Exceptions are automatically logged and caught by `GlobalExceptionHandler`:

```java
// Throw these exceptions - they're automatically handled
throw new EntityNotFoundException("User not found: " + id);
throw new DuplicateResourceException("Email already registered");
throw new ValidationException("email", "Invalid email format");
throw new AccessDeniedException("You don't have permission");
```

## 📊 Log File Locations

```
Project Root/
└── logs/
    ├── application.log              (Current day's logs)
    ├── application.2026-06-02.1.log (Archived logs)
    ├── error.log                    (Current day's errors)
    └── error.2026-06-02.1.log       (Archived errors)
```

## ⚙️ Configuration

### Change Log Level for Component
Edit `application.properties`:
```properties
logging.level.com.ygc.service=DEBUG
logging.level.org.springframework.security=INFO
```

### Change File Rotation
Edit `logback-spring.xml`:
```xml
<maxFileSize>50MB</maxFileSize>    <!-- Max file size before rotation -->
<maxHistory>90</maxHistory>        <!-- Keep logs for 90 days -->
<totalSizeCap>5GB</totalSizeCap>   <!-- Max total log size -->
```

## 🔎 Search Examples

```bash
# Find all database operations
grep "DB_OPERATION" logs/application.log

# Find all errors for a specific user
grep "user@email.com" logs/error.log

# Find performance issues (slow requests)
grep "PERFORMANCE" logs/application.log

# Find all business rule violations
grep "BUSINESS_RULE" logs/application.log

# Track a specific request
grep "a1b2c3d4-e5f6" logs/application.log

# Find all failed transactions
grep "Transaction FAILED" logs/application.log
```

## 📈 Performance Monitoring

```bash
# Find requests that took longer than 1 second
grep "PERFORMANCE" logs/application.log

# Count total requests
grep "\[API_CALL\]" logs/application.log | wc -l

# Count errors
grep "\[ERROR\]" logs/error.log | wc -l

# Count user actions
grep "\[USER_ACTION\]" logs/application.log | wc -l
```

## 🐛 Debugging

```bash
# Find full exception stack trace
grep -A 20 "Exception during" logs/error.log

# Find all exceptions by type
grep "java.lang.NullPointerException" logs/error.log

# Find context around an error (5 lines before and after)
grep -B5 -A5 "EntityNotFoundException" logs/error.log
```

## 📋 Request Tracing

Each HTTP request has a unique ID (`X-Request-ID` header):

```bash
# Track a specific request's entire flow
REQUEST_ID="a1b2c3d4-e5f6-7g8h"
grep "$REQUEST_ID" logs/application.log

# See all requests and their statuses
grep "\[API_CALL\]" logs/application.log | head -20
```

## 🔧 Common Issues

### Issue: Too Many Logs
**Solution**: Reduce log level
```properties
logging.level.com.ygc=INFO  # Changed from DEBUG
```

### Issue: Disk Space Filling Up
**Solution**: Reduce retention in logback-spring.xml
```xml
<maxHistory>7</maxHistory>  <!-- Keep only 7 days -->
```

### Issue: Can't Find Specific Event
**Solution**: Check archived logs
```bash
ls logs/                    # See all log files
grep "search term" logs/*   # Search across all logs
```

## 🎯 Best Practices

1. ✅ Always include context in logging
   ```java
   loggingUtil.debug("Processing user: " + userId, "ServiceName.method");
   ```

2. ✅ Log user actions for audit trail
   ```java
   loggingUtil.userAction(user.getEmail(), "PAYMENT_SUBMITTED", "Context");
   ```

3. ✅ Log business rule violations
   ```java
   loggingUtil.businessRuleViolation("RULE_NAME", "Context", details);
   ```

4. ✅ Handle non-critical failures gracefully
   ```java
   try {
       emailService.send(email);
   } catch (Exception e) {
       loggingUtil.error("Email send failed", "Context", e);
       // Continue - email is non-critical
   }
   ```

5. ✅ Track performance for optimization
   ```java
   long start = System.currentTimeMillis();
   // ... operation ...
   loggingUtil.performanceMetric("operationName", 
       System.currentTimeMillis() - start, "Context");
   ```

## 📞 Support

For detailed information, see:
- `OBSERVABILITY.md` - Complete documentation
- `OBSERVABILITY_SUMMARY.md` - Implementation summary
- Source: `com.ygc.util.LoggingUtil` - Logging utility methods

