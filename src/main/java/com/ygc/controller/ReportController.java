package com.ygc.controller;

import com.ygc.model.User;
import com.ygc.repository.*;
import com.ygc.service.ChitService;
import com.ygc.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportExportService reportExportService;
    private final CommissionLedgerRepository commissionLedgerRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final ChitService chitService;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final java.math.BigDecimal ZERO = java.math.BigDecimal.ZERO;

    @GetMapping("/commission/pdf")
    public ResponseEntity<byte[]> commissionPdf() {
        var ledger = commissionLedgerRepository.findAll();
        var total  = ledger.stream().map(l -> l.getCommissionAmount()).reduce(ZERO, java.math.BigDecimal::add);
        byte[] pdf = reportExportService.generateCommissionReport(ledger, total);
        return pdfResponse(pdf, "YGC_Commission_" + ts() + ".pdf");
    }

    @GetMapping("/payments/pdf")
    public ResponseEntity<byte[]> paymentsPdf() {
        byte[] pdf = reportExportService.generatePaymentReport(paymentRepository.findAll());
        return pdfResponse(pdf, "YGC_Payments_" + ts() + ".pdf");
    }

    @GetMapping("/settlements/pdf")
    public ResponseEntity<byte[]> settlementsPdf() {
        byte[] pdf = reportExportService.generateSettlementReport(settlementRepository.findAll());
        return pdfResponse(pdf, "YGC_Settlements_" + ts() + ".pdf");
    }

    @GetMapping("/members/pdf")
    public ResponseEntity<byte[]> membersPdf() {
        var members = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.MEMBER)
                .collect(Collectors.toList());
        byte[] pdf = reportExportService.generateMembersReport(members);
        return pdfResponse(pdf, "YGC_Members_" + ts() + ".pdf");
    }

    @GetMapping("/chits/pdf")
    public ResponseEntity<byte[]> chitsPdf() {
        byte[] pdf = reportExportService.generateChitsReport(chitService.getAllChits());
        return pdfResponse(pdf, "YGC_Chits_" + ts() + ".pdf");
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
