package com.ygc.controller;

import com.ygc.model.*;
import com.ygc.repository.*;
import com.ygc.service.*;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
@Slf4j
public class AdminPaymentController {
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final ChitMembershipRepository membershipRepository;
    private final LoggingUtil loggingUtil;
    private final UserRepository userRepository;

    @GetMapping("/upload")
    public String uploadForm(Model model) {
        loggingUtil.debug("Admin payment upload form accessed", "AdminPaymentController");
        model.addAttribute("memberships", membershipRepository.findAll());
        return "admin/payments-upload";
    }

    @PostMapping("/upload")
    public String uploadPayment(
            @RequestParam Long membershipId,
            @RequestParam BigDecimal amount,
            @RequestParam Integer monthNumber,
            @RequestParam(required = false) MultipartFile screenshot,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            loggingUtil.transactionStart("adminUploadPayment", "AdminPaymentController");
            User admin = userRepository.findByEmail(auth.getName()).orElseThrow();

            ChitMembership membership = membershipRepository.findById(membershipId).orElseThrow();
            Payment payment = new Payment();
            payment.setMembership(membership);
            payment.setAmount(amount);
            payment.setLateFine(BigDecimal.ZERO);
            payment.setTotalAmount(amount);
            payment.setMonthNumber(monthNumber);
            payment.setDueDate(LocalDate.now());
            payment.setPaidDate(LocalDate.now());
            payment.setStatus(Payment.PaymentStatus.APPROVED);
            payment.setVerifiedBy(admin);
            payment.setVerifiedAt(java.time.LocalDateTime.now());

            if (screenshot != null && !screenshot.isEmpty()) {
                String uploadDir = "uploads/payments/";
                new java.io.File(uploadDir).mkdirs();
                String filename = System.currentTimeMillis() + "_" + screenshot.getOriginalFilename();
                java.nio.file.Path path = java.nio.file.Paths.get(uploadDir + filename);
                java.nio.file.Files.write(path, screenshot.getBytes());
                payment.setScreenshotPath(path.toString());
            }

            paymentRepository.save(payment);
            loggingUtil.userAction(admin.getEmail(), "ADMIN_UPLOAD_PAYMENT", "AdminPaymentController");
            loggingUtil.transactionComplete("adminUploadPayment", "AdminPaymentController");

            redirectAttributes.addFlashAttribute("success", "Payment uploaded successfully");
            return "redirect:/admin/payments";
        } catch (Exception e) {
            loggingUtil.error("Error uploading payment", "AdminPaymentController", e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/payments/upload";
        }
    }

    @GetMapping("/{id}/view-screenshot")
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> viewScreenshot(@PathVariable Long id) throws Exception {
        Payment payment = paymentRepository.findById(id).orElseThrow();
        if (payment.getScreenshotPath() == null || "N/A".equals(payment.getScreenshotPath())) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        java.nio.file.Path path = java.nio.file.Paths.get(payment.getScreenshotPath());
        if (!java.nio.file.Files.exists(path)) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        byte[] bytes = java.nio.file.Files.readAllBytes(path);
        String contentType = java.nio.file.Files.probeContentType(path);
        if (contentType == null) contentType = "image/png";
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Type", contentType)
                .body(bytes);
    }
}

