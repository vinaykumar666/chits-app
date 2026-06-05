package com.ygc.controller;

import com.ygc.model.DocumentApproval;
import com.ygc.model.DocumentUpload;
import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import com.ygc.service.DocumentApprovalService;
import com.ygc.service.MultilingualMessageService;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentApprovalController {
    private final DocumentApprovalService documentApprovalService;
    private final MultilingualMessageService multilingualMessageService;
    private final UserRepository userRepository;
    private final LoggingUtil loggingUtil;

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    /**
     * Admin dashboard - View all pending documents for approval
     */
    @GetMapping("/approval")
    public String approvalDashboard(Authentication auth, Model model) {
        try {
            loggingUtil.debug("Document approval dashboard accessed", "DocumentApprovalController");
            User admin = getCurrentUser(auth);

            List<DocumentUpload> pendingDocuments = documentApprovalService.getPendingDocuments();
            long totalPending = documentApprovalService.countPendingApprovals();

            model.addAttribute("pendingDocuments", pendingDocuments);
            model.addAttribute("totalPending", totalPending);
            model.addAttribute("availableLanguages", multilingualMessageService.getAvailableLanguages());
            model.addAttribute("user", admin);

            return "admin/document-approval";
        } catch (Exception e) {
            loggingUtil.error("Error loading approval dashboard", "DocumentApprovalController", e);
            model.addAttribute("error", "Error loading documents");
            return "admin/document-approval";
        }
    }

    /**
     * Filter documents by language
     */
    @GetMapping("/approval/by-language")
    public String approvalByLanguage(
            @RequestParam String language,
            Authentication auth,
            Model model) {
        try {
            loggingUtil.debug("Filtering documents by language: " + language, "DocumentApprovalController");

            List<DocumentUpload> documents = documentApprovalService.getPendingDocumentsByLanguage(language);

            model.addAttribute("pendingDocuments", documents);
            model.addAttribute("selectedLanguage", language);
            model.addAttribute("languageDisplayName", multilingualMessageService.getLanguageDisplayName(language));
            model.addAttribute("availableLanguages", multilingualMessageService.getAvailableLanguages());
            model.addAttribute("user", getCurrentUser(auth));

            return "admin/document-approval-by-language";
        } catch (Exception e) {
            loggingUtil.error("Error filtering documents by language", "DocumentApprovalController", e);
            return "redirect:/admin/documents/approval";
        }
    }

    /**
     * View specific document details for approval
     */
    @GetMapping("/{id}/view")
    public String viewDocument(
            @PathVariable Long id,
            Authentication auth,
            Model model) {
        try {
            loggingUtil.debug("Viewing document: " + id, "DocumentApprovalController");

            // Get document - this would need to be fetched from repository
            // For now, we'll create a simple implementation
            model.addAttribute("documentId", id);
            model.addAttribute("user", getCurrentUser(auth));
            model.addAttribute("availableLanguages", multilingualMessageService.getAvailableLanguages());

            return "admin/document-view";
        } catch (Exception e) {
            loggingUtil.error("Error viewing document", "DocumentApprovalController", e);
            return "redirect:/admin/documents/approval";
        }
    }

    /**
     * View document file preview (PDF/Image)
     */
    @GetMapping("/{id}/preview")
    @ResponseBody
    public ResponseEntity<byte[]> previewDocument(@PathVariable Long id) {
        try {
            loggingUtil.debug("Previewing document: " + id, "DocumentApprovalController");
            byte[] documentContent = documentApprovalService.viewDocument(id);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=document.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(documentContent);
        } catch (Exception e) {
            loggingUtil.error("Error previewing document", "DocumentApprovalController", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Download document file
     */
    @GetMapping("/{id}/download")
    @ResponseBody
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        try {
            loggingUtil.debug("Downloading document: " + id, "DocumentApprovalController");
            byte[] documentContent = documentApprovalService.viewDocument(id);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=document.pdf")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(documentContent);
        } catch (Exception e) {
            loggingUtil.error("Error downloading document", "DocumentApprovalController", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Approve a document
     */
    @PostMapping("/{id}/approve")
    public String approveDocument(
            @PathVariable Long id,
            @RequestParam(required = false) String comments,
            @RequestParam(required = false) String language,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            loggingUtil.transactionStart("approveDocument", "DocumentApprovalController");
            User admin = getCurrentUser(auth);

            documentApprovalService.approveDocument(id, admin, comments, language != null ? language : "en");

            loggingUtil.transactionComplete("approveDocument", "DocumentApprovalController");
            redirectAttributes.addFlashAttribute("success", "Document approved successfully");
            return "redirect:/admin/documents/approval";
        } catch (Exception e) {
            loggingUtil.error("Error approving document", "DocumentApprovalController", e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/documents/approval";
        }
    }

    /**
     * Reject a document
     */
    @PostMapping("/{id}/reject")
    public String rejectDocument(
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestParam(required = false) String language,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            loggingUtil.transactionStart("rejectDocument", "DocumentApprovalController");
            User admin = getCurrentUser(auth);

            documentApprovalService.rejectDocument(id, admin, reason, language != null ? language : "en");

            loggingUtil.transactionComplete("rejectDocument", "DocumentApprovalController");
            redirectAttributes.addFlashAttribute("success", "Document rejected successfully");
            return "redirect:/admin/documents/approval";
        } catch (Exception e) {
            loggingUtil.error("Error rejecting document", "DocumentApprovalController", e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/documents/approval";
        }
    }

    /**
     * Get approval history for a document (AJAX)
     */
    @GetMapping("/{id}/approval-history")
    @ResponseBody
    public ResponseEntity<?> getApprovalHistory(@PathVariable Long id) {
        try {
            Optional<DocumentApproval> approval = documentApprovalService.getApprovalHistory(id);
            if (approval.isPresent()) {
                return ResponseEntity.ok(approval.get());
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            loggingUtil.error("Error fetching approval history", "DocumentApprovalController", e);
            return ResponseEntity.status(500).body("Error fetching history");
        }
    }

    /**
     * API endpoint - Get pending documents (for dashboard)
     */
    @GetMapping("/api/pending")
    @ResponseBody
    public ResponseEntity<?> getPendingDocuments() {
        try {
            List<DocumentUpload> documents = documentApprovalService.getPendingDocuments();
            long count = documentApprovalService.countPendingApprovals();

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("count", (int) count);
            response.put("documents", documents);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            loggingUtil.error("Error fetching pending documents", "DocumentApprovalController", e);
            return ResponseEntity.status(500).body("Error fetching documents");
        }
    }

    /**
     * API endpoint - Get documents by type
     */
    @GetMapping("/api/by-type")
    @ResponseBody
    public ResponseEntity<?> getDocumentsByType(@RequestParam String type) {
        try {
            List<DocumentUpload> documents = documentApprovalService.getPendingDocumentsByType(type);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            loggingUtil.error("Error fetching documents by type", "DocumentApprovalController", e);
            return ResponseEntity.status(500).body("Error fetching documents");
        }
    }
}

