package com.ygc.controller;

import com.ygc.model.*;
import com.ygc.repository.*;
import com.ygc.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Member-facing PDF export endpoints.
 * Each member can only export their own data.
 */
@RestController
@RequestMapping("/member/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEMBER')")
public class MemberReportController {

    private final ReportExportService reportExportService;
    private final UserRepository userRepository;
    private final ChitMembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    /** Download a PDF summary of all chit memberships for the current user. */
    @GetMapping("/my-memberships/pdf")
    public ResponseEntity<byte[]> myMembershipsPdf(Authentication auth) {
        User user = getCurrentUser(auth);
        var memberships = membershipRepository.findByUser(user);
        byte[] pdf = reportExportService.generateMemberSummaryReport(user, memberships);
        return pdfResponse(pdf, "YGC_My_Memberships_" + ts() + ".pdf");
    }

    /** Download a payment-history PDF for a specific membership (owner-only). */
    @GetMapping("/memberships/{id}/payments/pdf")
    public ResponseEntity<byte[]> membershipPaymentsPdf(@PathVariable Long id, Authentication auth) {
        User user = getCurrentUser(auth);
        ChitMembership membership = membershipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found"));

        // Security: members can only download their own statements
        if (!membership.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var payments = paymentRepository.findByMembership(membership);
        byte[] pdf = reportExportService.generateMemberPaymentHistoryReport(user, membership, payments);
        String filename = "YGC_" + membership.getChit().getName().replaceAll("[^a-zA-Z0-9_-]", "_")
                + "_Payments_" + ts() + ".pdf";
        return pdfResponse(pdf, filename);
    }

    private String ts() {
        return LocalDateTime.now().format(TS);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
