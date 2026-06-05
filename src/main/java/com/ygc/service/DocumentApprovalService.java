package com.ygc.service;

import com.ygc.model.DocumentApproval;
import com.ygc.model.DocumentUpload;
import com.ygc.model.User;
import com.ygc.repository.DocumentApprovalRepository;
import com.ygc.repository.DocumentUploadRepository;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentApprovalService {
    private final DocumentUploadRepository documentUploadRepository;
    private final DocumentApprovalRepository documentApprovalRepository;
    private final MultilingualMessageService multilingualMessageService;
    private final EmailService emailService;
    private final LoggingUtil loggingUtil;

    private static final String UPLOAD_DIR = "uploads/documents/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Upload a document for approval workflow
     */
    @Transactional
    public DocumentUpload uploadDocument(
            User uploadedBy,
            String documentName,
            String documentType,
            MultipartFile file,
            String language,
            Long membershipId,
            Long paymentId) throws Exception {

        loggingUtil.debug("Uploading document: " + documentName, "DocumentApprovalService");

        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }

        // Create upload directory if it doesn't exist
        File uploadDirectory = new File(UPLOAD_DIR);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }

        // Save file
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR + filename);
        Files.write(filePath, file.getBytes());

        // Create document upload entity
        DocumentUpload documentUpload = new DocumentUpload();
        documentUpload.setUploadedBy(uploadedBy);
        documentUpload.setDocumentName(documentName);
        documentUpload.setDocumentType(documentType);
        documentUpload.setFilePath(filePath.toString());
        documentUpload.setFileName(file.getOriginalFilename());
        documentUpload.setFileSize(file.getSize());
        documentUpload.setLanguage(language != null ? language : "en");
        documentUpload.setStatus(DocumentUpload.DocumentStatus.PENDING);
        documentUpload.setApprovalStatus(DocumentUpload.ApprovalStatus.PENDING);
        documentUpload.setUploadedAt(LocalDateTime.now());

        if (membershipId != null) {
            // Set membership reference if needed (will be lazy loaded)
        }
        if (paymentId != null) {
            // Set payment reference if needed (will be lazy loaded)
        }

        DocumentUpload saved = documentUploadRepository.save(documentUpload);

        // Create document approval record
        DocumentApproval approval = new DocumentApproval();
        approval.setDocumentUpload(saved);
        approval.setStatus(DocumentApproval.ApprovalStatus.PENDING);
        approval.setCreatedAt(LocalDateTime.now());
        approval.setUpdatedAt(LocalDateTime.now());
        documentApprovalRepository.save(approval);

        loggingUtil.userAction(uploadedBy.getEmail(), "DOCUMENT_UPLOAD", "DocumentApprovalService");
        return saved;
    }

    /**
     * Get all pending documents for admin approval
     */
    public List<DocumentUpload> getPendingDocuments() {
        loggingUtil.debug("Fetching pending documents", "DocumentApprovalService");
        return documentUploadRepository.findDocumentsPendingApproval(DocumentUpload.ApprovalStatus.PENDING);
    }

    /**
     * Get pending documents by language
     */
    public List<DocumentUpload> getPendingDocumentsByLanguage(String language) {
        List<DocumentUpload> documents = getPendingDocuments();
        return documents.stream()
                .filter(d -> d.getLanguage().equalsIgnoreCase(language))
                .toList();
    }

    /**
     * Get pending documents by type
     */
    public List<DocumentUpload> getPendingDocumentsByType(String documentType) {
        loggingUtil.debug("Fetching pending documents by type: " + documentType, "DocumentApprovalService");
        return documentUploadRepository.findByDocumentTypeAndApprovalStatus(
                documentType,
                DocumentUpload.ApprovalStatus.PENDING
        );
    }

    /**
     * Approve a document
     */
    @Transactional
    public void approveDocument(Long documentId, User approver, String comments, String language) throws Exception {
        loggingUtil.transactionStart("approveDocument", "DocumentApprovalService");

        DocumentUpload document = documentUploadRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        DocumentApproval approval = documentApprovalRepository.findByDocumentUpload_Id(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Approval record not found"));

        // Update document
        document.setStatus(DocumentUpload.DocumentStatus.APPROVED);
        document.setApprovalStatus(DocumentUpload.ApprovalStatus.APPROVED);
        documentUploadRepository.save(document);

        // Update approval
        approval.setApprovedBy(approver);
        approval.setStatus(DocumentApproval.ApprovalStatus.APPROVED);
        approval.setApprovedAt(LocalDateTime.now());
        approval.setApprovalComments(comments);
        approval.setUpdatedAt(LocalDateTime.now());
        documentApprovalRepository.save(approval);

        // Send notification email
        sendApprovalEmail(document, true, comments, language);

        loggingUtil.userAction(approver.getEmail(), "APPROVE_DOCUMENT", "DocumentApprovalService");
        loggingUtil.transactionComplete("approveDocument", "DocumentApprovalService");
    }

    /**
     * Reject a document
     */
    @Transactional
    public void rejectDocument(Long documentId, User approver, String rejectionReason, String language) throws Exception {
        loggingUtil.transactionStart("rejectDocument", "DocumentApprovalService");

        DocumentUpload document = documentUploadRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        DocumentApproval approval = documentApprovalRepository.findByDocumentUpload_Id(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Approval record not found"));

        // Update document
        document.setStatus(DocumentUpload.DocumentStatus.REJECTED);
        document.setApprovalStatus(DocumentUpload.ApprovalStatus.REJECTED);
        documentUploadRepository.save(document);

        // Update approval
        approval.setApprovedBy(approver);
        approval.setStatus(DocumentApproval.ApprovalStatus.REJECTED);
        approval.setRejectionReason(rejectionReason);
        approval.setApprovedAt(LocalDateTime.now());
        approval.setUpdatedAt(LocalDateTime.now());
        documentApprovalRepository.save(approval);

        // Send rejection email
        sendApprovalEmail(document, false, rejectionReason, language);

        loggingUtil.userAction(approver.getEmail(), "REJECT_DOCUMENT", "DocumentApprovalService");
        loggingUtil.transactionComplete("rejectDocument", "DocumentApprovalService");
    }

    /**
     * Get approval history for a document
     */
    public Optional<DocumentApproval> getApprovalHistory(Long documentId) {
        return documentApprovalRepository.findByDocumentUpload_Id(documentId);
    }

    /**
     * Count pending approvals
     */
    public long countPendingApprovals() {
        return documentApprovalRepository.countByStatus(DocumentApproval.ApprovalStatus.PENDING);
    }

    /**
     * View document file
     */
    public byte[] viewDocument(Long documentId) throws Exception {
        DocumentUpload document = documentUploadRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        return Files.readAllBytes(Paths.get(document.getFilePath()));
    }

    /**
     * Send approval/rejection email
     */
    private void sendApprovalEmail(DocumentUpload document, boolean approved, String message, String language) {
        try {
            String userLanguage = document.getLanguage() != null ? document.getLanguage() : language;
            String userEmail = document.getUploadedBy().getEmail();
            String userName = document.getUploadedBy().getFullName();

            if (approved) {
                String subject = multilingualMessageService.getMessage(userLanguage, "email.document.approval.subject");
                Map<String, String> params = new HashMap<>();
                params.put("name", userName);
                params.put("document", document.getDocumentName());

                String bodyTemplate = multilingualMessageService.getMessage(userLanguage, "email.document.approval.body");
                String body = multilingualMessageService.getMessageWithParams(userLanguage, "email.document.approval.body", params);

                emailService.sendEmail(userEmail, subject, body);
            } else {
                String subject = multilingualMessageService.getMessage(userLanguage, "email.document.rejection.subject");
                Map<String, String> params = new HashMap<>();
                params.put("name", userName);
                params.put("document", document.getDocumentName());
                params.put("reason", message != null ? message : "No reason provided");

                String body = multilingualMessageService.getMessageWithParams(userLanguage, "email.document.rejection.body", params);
                emailService.sendEmail(userEmail, subject, body);
            }
        } catch (Exception e) {
            loggingUtil.error("Error sending approval email", "DocumentApprovalService", e);
        }
    }

    /**
     * Get document by ID
     */
    public DocumentUpload getDocumentById(Long documentId) throws Exception {
        return documentUploadRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
    }

    /**
     * Get documents uploaded by a specific user
     */
    public java.util.List<DocumentUpload> getDocumentsByUser(User user) {
        loggingUtil.debug("Fetching documents for user: " + user.getEmail(), "DocumentApprovalService");
        return documentUploadRepository.findByUploadedBy_Id(user.getId());
    }
}
