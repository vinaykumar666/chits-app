# Document Upload & Approval System

## Overview

The Document Upload & Approval System enables members to upload documents (agreements, certificates, payment proofs, identification, etc.) that require admin review and approval. The system features **multilingual support** for Indian languages with automatic email notifications in the member's preferred language.

## Features

### 1. **Member-Facing Features**
- **Upload Documents**: Members can upload documents with the following information:
  - Document Name (custom identifier)
  - Document Type (Agreement, Certificate, Payment Proof, Identification, Bank Statement, Other)
  - Language of the document (English, Hindi, Tamil, Telugu, Kannada)
  - File upload (PDF, JPG, PNG - max 10MB)

- **Track Status**: Members can:
  - View all their uploaded documents
  - Check current approval status (Pending, Under Review, Approved, Rejected)
  - View approval history with admin comments
  - Resubmit rejected documents

### 2. **Admin-Facing Features**
- **Dashboard Widget**: Shows pending document count on admin dashboard
- **Document Approval Dashboard**: 
  - View all pending documents
  - Filter documents by language
  - Preview documents (PDF inline viewer)
  - Download documents for offline review
  - Approve documents with optional comments
  - Reject documents with mandatory reason

- **Multilingual Support**: 
  - Admin can see document language at a glance
  - Notifications sent to members in their selected language
  - Support for: English, Hindi, Tamil, Telugu, Kannada

### 3. **Multilingual Features**
Documents and communications are available in 5 Indian languages:
- **English** (en) - 🇬🇧
- **Hindi** (hi) - 🇮🇳 हिन्दी
- **Tamil** (ta) - தமிழ்
- **Telugu** (te) - తెలుగు
- **Kannada** (kn) - ಕನ್ನಡ

All approval/rejection emails are automatically sent in the member's selected language for better comprehension.

## Database Schema

### DocumentUpload Entity
```java
- id (Long): Primary Key
- uploadedBy (User): FK to User
- documentName (String): Custom document name
- documentType (String): AGREEMENT, CERTIFICATE, PAYMENT_PROOF, IDENTIFICATION, etc.
- filePath (String): Full path to uploaded file
- fileName (String): Original filename
- fileSize (Long): File size in bytes
- status (DocumentStatus): PENDING, APPROVED, REJECTED, ARCHIVED
- approvalStatus (ApprovalStatus): PENDING, APPROVED, REJECTED, UNDER_REVIEW
- language (String): ISO 639-1 code (en, hi, ta, te, kn)
- uploadedAt (LocalDateTime): Timestamp of upload
- chitMembership (ChitMembership): Optional FK for membership-linked documents
- payment (Payment): Optional FK for payment-linked documents
```

### DocumentApproval Entity
```java
- id (Long): Primary Key
- documentUpload (DocumentUpload): One-to-One relationship
- approvedBy (User): Admin who reviewed
- status (ApprovalStatus): PENDING, UNDER_REVIEW, APPROVED, REJECTED, HOLD
- approvalComments (String): Admin comments on approval
- rejectionReason (String): Reason for rejection
- approvedAt (LocalDateTime): When decision was made
- reviewedAt (LocalDateTime): When review started
- sentForReview (Boolean): Flag for workflow
- sentForReviewAt (LocalDateTime): When sent for review
```

## API Endpoints

### Member Endpoints
- `GET /member/documents/upload` - Display upload form
- `POST /member/documents/upload` - Upload document
- `GET /member/documents/my-uploads` - View all member's documents
- `GET /member/documents/{id}/status` - View document status and approval history

### Admin Endpoints
- `GET /admin/documents/approval` - Admin approval dashboard
- `GET /admin/documents/approval/by-language?language={lang}` - Filter by language
- `GET /admin/documents/{id}/view` - View document details
- `GET /admin/documents/{id}/preview` - Preview document (inline PDF)
- `GET /admin/documents/{id}/download` - Download document
- `POST /admin/documents/{id}/approve` - Approve document
- `POST /admin/documents/{id}/reject` - Reject document
- `GET /admin/documents/{id}/approval-history` - Get approval details (AJAX)
- `GET /admin/documents/api/pending` - Get pending documents list (JSON)
- `GET /admin/documents/api/by-type?type={type}` - Get documents by type (JSON)

## Workflow

### Document Upload Flow
1. Member navigates to `/member/documents/upload`
2. Fills in document details:
   - Document Name
   - Document Type
   - Language (for notifications)
   - Selects file
3. System validates:
   - File not empty
   - File size ≤ 10MB
4. Document is saved with status = PENDING
5. DocumentApproval record created with status = PENDING
6. Admin notified (future feature: dashboard widget)

### Approval Workflow
1. Admin navigates to `/admin/documents/approval`
2. Views list of pending documents
3. Can filter by language for targeted review
4. Clicks "Preview" to view document inline
5. **Approval Path**:
   - Clicks "Approve"
   - Optionally adds comments
   - System:
     - Updates document status to APPROVED
     - Updates approval status to APPROVED
     - Sends approval email in member's language
6. **Rejection Path**:
   - Clicks "Reject"
   - Must provide rejection reason
   - System:
     - Updates document status to REJECTED
     - Updates approval status to REJECTED
     - Sends rejection email in member's language
     - Member can resubmit

### Member Status Tracking
1. Member navigates to `/member/documents/my-uploads`
2. Can see all documents with status badges:
   - ⏳ Pending
   - 🔍 Under Review
   - ✓ Approved
   - ✗ Rejected
3. Clicks "View Status" for specific document
4. Sees:
   - Document details
   - Current approval status
   - Admin comments (if approved)
   - Rejection reason (if rejected)
   - Option to upload new document

## Services

### DocumentApprovalService
Located at: `com.ygc.service.DocumentApprovalService`

Key Methods:
- `uploadDocument()` - Process and save uploaded document
- `getPendingDocuments()` - Get all pending for approval
- `getPendingDocumentsByLanguage()` - Filter by language
- `getPendingDocumentsByType()` - Filter by document type
- `approveDocument()` - Approve with optional comments
- `rejectDocument()` - Reject with mandatory reason
- `getApprovalHistory()` - Get approval details
- `viewDocument()` - Get file bytes for preview/download
- `getDocumentById()` - Get single document
- `getDocumentsByUser()` - Get member's documents
- `countPendingApprovals()` - Count for dashboard

### MultilingualMessageService
Located at: `com.ygc.service.MultilingualMessageService`

Provides multilingual strings for:
- Document status messages
- Email subjects and bodies
- UI labels
- Notification messages

Supported languages: English, Hindi, Tamil, Telugu, Kannada

## Repositories

### DocumentUploadRepository
```java
- findByStatus()
- findByApprovalStatus()
- findByChitMembership_Id()
- findByPayment_Id()
- findByUploadedBy_Id()
- findDocumentsPendingApproval()
- findByDocumentTypeAndApprovalStatus()
- findByLanguage()
```

### DocumentApprovalRepository
```java
- findByDocumentUpload_Id()
- findByStatus()
- findPendingApprovals()
- findByApprovedBy_Id()
- countByStatus()
```

## File Storage

Documents are stored in: `uploads/documents/`

File naming convention: `{timestamp}_{originalFilename}`

Example: `1717594832000_agreement.pdf`

**Size Limit**: 10MB per document

## Email Notifications

### Approval Email
- **Subject**: "Document Approval - YGC Internal" (in member's language)
- **Body**: Includes document name and admin comments
- **Language**: Sent in member's selected language

### Rejection Email
- **Subject**: "Document Rejection - YGC Internal" (in member's language)
- **Body**: Includes document name and rejection reason
- **Language**: Sent in member's selected language

## Templates

### Admin Templates
- `admin/document-approval.html` - Main admin approval dashboard
- `admin/document-approval-by-language.html` - Language-filtered view

### Member Templates
- `member/document-upload.html` - Upload form with language selection
- `member/document-uploads-list.html` - All member's documents with status
- `member/document-status.html` - Detailed status page with approval history

## Integration Points

### Admin Dashboard
The admin dashboard now includes a "Pending Documents" widget showing:
- Count of pending document approvals
- Quick link to approval dashboard

### Existing Features
- **User Model**: Extended relationships with DocumentUpload
- **Email Service**: Used for sending multilingual notifications
- **Logging Service**: All actions logged via LoggingUtil
- **Authentication**: Uses Spring Security for role-based access

## How to Use

### For Members
1. Navigate to "Documents" section (add to nav menu)
2. Click "Upload Document"
3. Fill in document details
4. Select document language (important for notifications!)
5. Choose file to upload
6. Submit
7. View status in "My Uploads"
8. Receive email notification when approved/rejected

### For Admins
1. Dashboard shows "Pending Documents" count
2. Click on dashboard widget or navigate to "Document Approval"
3. Browse pending documents
4. Use language filter for targeted review
5. Preview documents inline
6. Approve with comments OR reject with reason
7. Member automatically notified in their language

## Configuration

Add to `application.properties`:
```properties
# Document Upload Settings
document.upload.max-size=10485760
document.upload.directory=uploads/documents/
document.language.default=en
```

## Security Considerations

- File upload validation (size, type)
- Member can only view their own documents
- Admin role required for approval dashboard
- File storage outside web root
- CSRF protection on forms
- Audit logging of all approvals

## Future Enhancements

1. **Document Categories**: More granular document types
2. **Batch Approval**: Approve multiple documents at once
3. **Document Expiry**: Set expiration dates on approvals
4. **Comments Threading**: Back-and-forth communication
5. **Document Templates**: Pre-defined document types
6. **OCR Integration**: Extract data from documents
7. **Digital Signatures**: E-signature support
8. **Workflow States**: More granular approval workflow
9. **Scheduled Reminders**: Notify members of pending reviews
10. **Analytics**: Document approval metrics and trends

## Troubleshooting

### File Upload Fails
- Check file size (max 10MB)
- Verify file format (PDF, JPG, PNG)
- Ensure `uploads/documents/` directory exists and is writable

### Email Not Sent
- Check email service configuration
- Verify member email address
- Check application logs for errors
- Verify document language is valid

### Document Not Visible to Admin
- Ensure document status is "PENDING"
- Check admin has ADMIN role
- Verify approval record exists

### Member Can't See Their Document
- Verify they're logged in with correct account
- Check document belongs to logged-in user
- Verify document was saved successfully

