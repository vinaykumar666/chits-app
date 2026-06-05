# Document Approval System - Implementation Guide

## ✅ What Has Been Implemented

### 1. **Database Models**
- ✅ `DocumentUpload.java` - Entity for uploaded documents
- ✅ `DocumentApproval.java` - Entity for tracking approval workflow

### 2. **Repositories**
- ✅ `DocumentUploadRepository.java` - Data access for documents
- ✅ `DocumentApprovalRepository.java` - Data access for approvals

### 3. **Services**
- ✅ `DocumentApprovalService.java` - Core business logic
  - Upload document with validation
  - Get pending documents
  - Filter by language/type
  - Approve/reject documents
  - Send multilingual notifications

- ✅ `MultilingualMessageService.java` - Multilingual support
  - 5 Indian languages: English, Hindi, Tamil, Telugu, Kannada
  - Message templates for all languages
  - Dynamic message replacement

### 4. **Controllers**
- ✅ `DocumentApprovalController.java` - Admin endpoints
  - Dashboard with pending documents
  - Language-filtered views
  - Document approval/rejection
  - File preview/download

- ✅ `MemberDocumentController.java` - Member endpoints
  - Document upload
  - View my uploads
  - Track status

### 5. **Templates**
- ✅ `admin/document-approval.html` - Admin approval dashboard
- ✅ `admin/document-approval-by-language.html` - Language-filtered view
- ✅ `member/document-upload.html` - Member upload form
- ✅ `member/document-uploads-list.html` - Member's documents list
- ✅ `member/document-status.html` - Document status tracking

### 6. **Integration**
- ✅ `AdminController.java` - Added DocumentApprovalService dependency
- ✅ Admin dashboard - Added pending documents count
- ✅ Email notifications - Multilingual support
- ✅ Existing logic preserved - No breaking changes

### 7. **Documentation**
- ✅ `DOCUMENT_APPROVAL_SYSTEM.md` - Complete system documentation

## 🚀 Next Steps to Activate

### Step 1: Add Navigation Menu Items

Add to your navigation template (e.g., `fragments.html` or your navbar):

**For Members:**
```html
<a href="/member/documents/upload" class="nav-link">📤 Upload Document</a>
<a href="/member/documents/my-uploads" class="nav-link">📋 My Documents</a>
```

**For Admins:**
```html
<a href="/admin/documents/approval" class="nav-link">📄 Document Approval</a>
```

### Step 2: Create Database Tables

Run these SQL commands to create the necessary tables:

```sql
-- DocumentUpload table
CREATE TABLE document_uploads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uploaded_by BIGINT NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    status VARCHAR(50) DEFAULT 'PENDING',
    approval_status VARCHAR(50) DEFAULT 'PENDING',
    language VARCHAR(10) DEFAULT 'en',
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    chit_membership_id BIGINT,
    payment_id BIGINT,
    FOREIGN KEY (uploaded_by) REFERENCES users(id),
    FOREIGN KEY (chit_membership_id) REFERENCES chit_memberships(id),
    FOREIGN KEY (payment_id) REFERENCES payments(id)
);

-- DocumentApproval table
CREATE TABLE document_approvals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_upload_id BIGINT NOT NULL UNIQUE,
    approved_by BIGINT,
    status VARCHAR(50) DEFAULT 'PENDING',
    approval_comments TEXT,
    rejection_reason TEXT,
    approved_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    sent_for_review BOOLEAN DEFAULT FALSE,
    sent_for_review_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (document_upload_id) REFERENCES document_uploads(id),
    FOREIGN KEY (approved_by) REFERENCES users(id)
);

-- Create indexes for performance
CREATE INDEX idx_document_uploads_status ON document_uploads(status);
CREATE INDEX idx_document_uploads_approval_status ON document_uploads(approval_status);
CREATE INDEX idx_document_uploads_uploaded_by ON document_uploads(uploaded_by);
CREATE INDEX idx_document_uploads_language ON document_uploads(language);
CREATE INDEX idx_document_approvals_status ON document_approvals(status);
```

### Step 3: Ensure Upload Directory Exists

Create the upload directory (if it doesn't exist):
```bash
# Linux/Mac
mkdir -p uploads/documents
chmod 755 uploads/documents

# Windows (in project root)
mkdir uploads\documents
```

### Step 4: Update Email Configuration

Make sure your `application.properties` has email configured:

```properties
spring.mail.host=your-smtp-host
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

### Step 5: Update Security Configuration

If you have custom security rules, ensure these endpoints are accessible:

```java
// For members
"/member/documents/**" - MEMBER role required

// For admins
"/admin/documents/**" - ADMIN role required
```

## 📋 Usage Guide

### For Members

1. **Upload Document**
   - Go to "Upload Document" link
   - Fill in document details
   - Select document language (important!)
   - Choose file (PDF, JPG, PNG - max 10MB)
   - Click "Upload Document"

2. **Track Status**
   - Go to "My Documents"
   - See all uploaded documents with status
   - Click "View Status" to see approval details
   - Receive email notification in your selected language

### For Admins

1. **View Pending Documents**
   - Dashboard shows "Pending Documents" count
   - Click on widget or go to "Document Approval"
   - See all pending documents

2. **Filter by Language**
   - Click language buttons to filter
   - Helpful for language-specific review

3. **Review Document**
   - Click "Preview" to view document inline
   - Click "Download" to save locally

4. **Approve Document**
   - Click "Approve"
   - Optionally add comments
   - Email sent to member in their language

5. **Reject Document**
   - Click "Reject"
   - Provide rejection reason (required)
   - Email sent to member in their language

## 🔐 Security Features

✅ File upload validation (size, type)
✅ Member can only view their documents
✅ Admin role required for approval
✅ CSRF protection on forms
✅ File storage outside web root
✅ Audit logging of all actions
✅ Input validation and sanitization

## 🌍 Multilingual Support

The system supports automatic multilingual emails:

| Language | Code | Flag |
|----------|------|------|
| English | en | 🇬🇧 |
| Hindi | hi | 🇮🇳 |
| Tamil | ta | தமிழ் |
| Telugu | te | తెలుగు |
| Kannada | kn | ಕನ್ನಡ |

Members select language when uploading documents. Approval/rejection emails are automatically sent in their selected language.

## 📊 API Endpoints Reference

### Admin APIs
```
GET    /admin/documents/approval                      - Approval dashboard
GET    /admin/documents/approval/by-language          - Filter by language
GET    /admin/documents/{id}/view                     - View document details
GET    /admin/documents/{id}/preview                  - Preview (inline PDF)
GET    /admin/documents/{id}/download                 - Download file
POST   /admin/documents/{id}/approve                  - Approve document
POST   /admin/documents/{id}/reject                   - Reject document
GET    /admin/documents/{id}/approval-history         - Get approval details
GET    /admin/documents/api/pending                   - Get pending (JSON)
GET    /admin/documents/api/by-type                   - Get by type (JSON)
```

### Member APIs
```
GET    /member/documents/upload                       - Upload form
POST   /member/documents/upload                       - Submit document
GET    /member/documents/my-uploads                   - View my documents
GET    /member/documents/{id}/status                  - Check status
```

## 🧪 Testing Checklist

- [ ] Member can upload document
- [ ] File size validation works
- [ ] Document appears in admin dashboard
- [ ] Admin can preview document
- [ ] Admin can approve document
- [ ] Member receives approval email in correct language
- [ ] Admin can reject document with reason
- [ ] Member receives rejection email in correct language
- [ ] Member can resubmit rejected documents
- [ ] Language filter works in admin dashboard
- [ ] Member can only see their own documents

## 📝 Troubleshooting

### Documents not appearing in admin dashboard
- Check `document_uploads` table has entries
- Verify approval status is PENDING
- Ensure admin user has ADMIN role

### Email not sent
- Check email configuration in `application.properties`
- Verify member email address is valid
- Check application logs
- Ensure language code is valid

### File upload fails
- Check `uploads/documents/` directory exists and is writable
- Verify file size < 10MB
- Confirm file type is PDF, JPG, or PNG

### Compilation errors
- Ensure all repositories are created
- Verify service classes are in `com.ygc.service` package
- Check controller classes are in `com.ygc.controller` package
- Run `mvn clean compile` to verify

## 💡 Tips & Best Practices

1. **Language Selection**: Educate members to select correct language for better communication
2. **Document Naming**: Guide members to use clear document names (e.g., "Bank Statement 2024", not just "document")
3. **File Quality**: Encourage high-quality scans/uploads for easier admin review
4. **Quick Processing**: Set expectation for review timeframe
5. **Batch Approval**: Admins can quickly approve multiple documents by filtering language/type
6. **Archives**: Implement periodic archival of old approvals for better performance

## 📞 Support & Questions

For issues or questions, refer to:
- `/docs/DOCUMENT_APPROVAL_SYSTEM.md` - Complete documentation
- Source code comments in service and controller classes
- Email notification templates in `MultilingualMessageService`

---

**Implementation Status**: ✅ **COMPLETE**

All features are ready to use. Just ensure database tables are created and upload directory exists.

