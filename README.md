# YGC Internal Spring Boot App

Java Spring Boot MVP for the YGC Internal chit management BRD.

## Stack

- Java 17
- Spring Boot 3.5.14
- Spring Security with BCrypt
- Spring Data JPA
- H2 in-memory database
- Thymeleaf
- Maven

## Run

```powershell
mvn spring-boot:run
```

If port `8080` is already occupied:

```powershell
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Open:

```text
http://127.0.0.1:8081/login
```

## Demo Login

```text
Admin email: admin@ygc.internal
Admin password: Admin@123
```

Seeded member users:

```text
aarav.sharma@example.com / Member@123
meera.iyer@example.com / YGC-7314
rahul.nair@example.com / Member@123
```

## JWT Login API

```powershell
Invoke-WebRequest `
  -Uri http://127.0.0.1:8081/api/auth/login `
  -Method Post `
  -ContentType 'application/json' `
  -Body '{"email":"admin@ygc.internal","password":"Admin@123"}'
```

## Implemented BRD Modules

- Authentication and self-registration
- Temporary password and first-login password state
- BCrypt password encryption
- JWT login endpoint
- Chit group creation and capacity/start-date enforcement
- Terms and digital signature acceptance tracking
- QR payment proof upload metadata and manual verification
- Payment statuses: pending, approved, rejected, overdue
- Rs. 20 per day late fine calculation
- Admin commission reports
- Monthly auction announcement and winner approval
- Lump sum payout recording
- Early-exit settlement with 2% deduction, dues, and fines
- Final profit distribution statement rows
- Notification log
- Audit log
- H2 console at `/h2-console`
