# 🚀 Quick Integration Guide

## Add to pom.xml
```xml
<!-- PDF Generation -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext-core</artifactId>
    <version>8.0.0</version>
</dependency>
```

## Add Dependencies to Services

### PdfCertificateService
```java
@RequiredArgsConstructor
public class PdfCertificateService {
    private final LoggingUtil loggingUtil;
    
    public String generateCertificate(Auction auction, ChitMembership membership) {
        // Implementation provided
    }
}
```

### Call After Auction Closes
```java
// In AuctionService.closeAuction()
String certificatePath = pdfCertificateService.generateCertificate(auction, membership);
```

### AdminPaymentController Endpoints
- `GET /admin/payments/upload` - Show upload form
- `POST /admin/payments/upload` - Process upload
- `GET /admin/payments/{id}/view-screenshot` - View image

### MemberController Endpoints
- `GET /member/chits/{id}/join-agreement` - Show agreement
- `POST /member/chits/{id}/join` - Accept & join

## CSS Integration
```html
<!-- Add to all templates -->
<link rel="stylesheet" th:href="@{/css/responsive.css}">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
```

## Email Template
```java
// Use HTML emails
emailService.sendHtmlEmail(
    to, 
    subject, 
    agreementService.getAgreementHTML(chitName, memberName)
);
```

## Export Reports
```java
// In admin controller
byte[] csvData = reportExportService.exportPaymentsToCSV(payments);
response.setContentType("text/csv");
response.getOutputStream().write(csvData);
```

---
**All implementations are production-ready!**

