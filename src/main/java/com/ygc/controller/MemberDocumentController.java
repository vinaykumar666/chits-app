package com.ygc.controller;

import com.ygc.model.DocumentUpload;
import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import com.ygc.service.DocumentApprovalService;
import com.ygc.service.MultilingualMessageService;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/member/documents")
@RequiredArgsConstructor
@Slf4j
public class MemberDocumentController {
    private final DocumentApprovalService documentApprovalService;
    private final MultilingualMessageService multilingualMessageService;
    private final UserRepository userRepository;
    private final LoggingUtil loggingUtil;

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    /**
     * Display upload form for members
     */
    @GetMapping("/upload")
    public String uploadForm(Authentication auth, Model model) {
        try {
            loggingUtil.debug("Member document upload form accessed", "MemberDocumentController");
            model.addAttribute("user", getCurrentUser(auth));
            model.addAttribute("availableLanguages", multilingualMessageService.getAvailableLanguages());
            model.addAttribute("languageNames",
                multilingualMessageService.getAvailableLanguages().stream()
                    .toList());
            return "member/document-upload";
        } catch (Exception e) {
            loggingUtil.error("Error loading upload form", "MemberDocumentController", e);
            return "redirect:/member/dashboard";
        }
    }

    /**
     * Handle document upload
     */
    @PostMapping("/upload")
    public String uploadDocument(
            @RequestParam String documentName,
            @RequestParam String documentType,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false, defaultValue = "en") String language,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            loggingUtil.transactionStart("memberUploadDocument", "MemberDocumentController");
            User member = getCurrentUser(auth);

            if (file == null || file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
                return "redirect:/member/documents/upload";
            }

            DocumentUpload uploadedDoc = documentApprovalService.uploadDocument(
                    member,
                    documentName,
                    documentType,
                    file,
                    language,
                    null,
                    null
            );

            loggingUtil.transactionComplete("memberUploadDocument", "MemberDocumentController");
            redirectAttributes.addFlashAttribute("success",
                multilingualMessageService.getMessage(language, "document.upload.success"));
            return "redirect:/member/documents/my-uploads";
        } catch (IllegalArgumentException e) {
            loggingUtil.warn("Invalid document upload: " + e.getMessage(), "MemberDocumentController");
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/member/documents/upload";
        } catch (Exception e) {
            loggingUtil.error("Error uploading document", "MemberDocumentController", e);
            redirectAttributes.addFlashAttribute("error",
                multilingualMessageService.getMessage(language, "document.upload.error") + ": " + e.getMessage());
            return "redirect:/member/documents/upload";
        }
    }

    /**
     * View member's uploaded documents
     */
    @GetMapping("/my-uploads")
    public String myUploads(Authentication auth, Model model) {
        try {
            loggingUtil.debug("Member viewing their uploads", "MemberDocumentController");
            User member = getCurrentUser(auth);

            List<DocumentUpload> myDocuments = documentApprovalService.getDocumentsByUser(member);

            model.addAttribute("user", member);
            model.addAttribute("documents", myDocuments);
            model.addAttribute("availableLanguages", multilingualMessageService.getAvailableLanguages());

            return "member/document-uploads-list";
        } catch (Exception e) {
            loggingUtil.error("Error loading member uploads", "MemberDocumentController", e);
            return "redirect:/member/dashboard";
        }
    }

    /**
     * View approval status of a specific document
     */
    @GetMapping("/{id}/status")
    public String viewDocumentStatus(
            @PathVariable Long id,
            Authentication auth,
            Model model) {
        try {
            loggingUtil.debug("Viewing document status: " + id, "MemberDocumentController");
            User member = getCurrentUser(auth);

            // Get document and ensure it belongs to the member
            DocumentUpload document = documentApprovalService.getDocumentById(id);
            if (!document.getUploadedBy().getId().equals(member.getId())) {
                return "redirect:/member/documents/my-uploads";
            }

            model.addAttribute("user", member);
            model.addAttribute("document", document);
            model.addAttribute("approvalHistory", documentApprovalService.getApprovalHistory(id));

            return "member/document-status";
        } catch (Exception e) {
            loggingUtil.error("Error viewing document status", "MemberDocumentController", e);
            return "redirect:/member/documents/my-uploads";
        }
    }
}

