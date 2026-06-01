package com.ygc.service;

import com.ygc.audit.AuditService;
import com.ygc.model.*;
import com.ygc.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ChitMembershipRepository membershipRepository;
    private final CommissionLedgerRepository commissionLedgerRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${ygc.late-fine-per-day:20}")
    private int lateFinePerDay;

    @Transactional
    public Payment submitPayment(Long membershipId, MultipartFile screenshot,
                                 Integer monthNumber, User member) throws Exception {
        ChitMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found"));

        String screenshotPath = "N/A";
        if (screenshot != null && !screenshot.isEmpty()) {
            String uploadDir = "uploads/payments/";
            new File(uploadDir).mkdirs();
            String filename = System.currentTimeMillis() + "_" + screenshot.getOriginalFilename();
            Path path = Paths.get(uploadDir + filename);
            Files.write(path, screenshot.getBytes());
            screenshotPath = path.toString();
        }

        LocalDate dueDate = membership.getChit().getStartDate().plusMonths(monthNumber - 1);
        BigDecimal fine = BigDecimal.ZERO;
        if (LocalDate.now().isAfter(dueDate)) {
            long daysLate = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
            fine = BigDecimal.valueOf(daysLate * lateFinePerDay);
        }

        Payment payment = new Payment();
        payment.setMembership(membership);
        payment.setAmount(membership.getChit().getMonthlyAmount());
        payment.setLateFine(fine);
        payment.setTotalAmount(membership.getChit().getMonthlyAmount().add(fine));
        payment.setScreenshotPath(screenshotPath);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setDueDate(dueDate);
        payment.setPaidDate(LocalDate.now());
        payment.setMonthNumber(monthNumber);

        Payment saved = paymentRepository.save(payment);
        auditService.log(member, "SUBMIT_PAYMENT", "Payment", saved.getId(),
                "Payment submitted for month " + monthNumber);
        return saved;
    }

    @Transactional
    public void verifyPayment(Long paymentId, boolean approved, String remarks, User admin) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        payment.setStatus(approved ? Payment.PaymentStatus.APPROVED : Payment.PaymentStatus.REJECTED);
        payment.setAdminRemarks(remarks);
        payment.setVerifiedBy(admin);
        payment.setVerifiedAt(java.time.LocalDateTime.now());
        paymentRepository.save(payment);

        if (approved) {
            Chit chit = payment.getMembership().getChit();
            CommissionLedger commission = new CommissionLedger();
            commission.setChit(chit);
            commission.setCommissionPercentage(chit.getAdminCommissionPercentage());
            commission.setCommissionAmount(payment.getTotalAmount()
                    .multiply(chit.getAdminCommissionPercentage())
                    .divide(BigDecimal.valueOf(100)));
            commission.setSource("MONTHLY_COLLECTION");
            commission.setMonth(LocalDate.now());
            commissionLedgerRepository.save(commission);
        }

        User member = payment.getMembership().getUser();
        emailService.sendPaymentApproval(member.getEmail(), member.getFullName(),
                payment.getMembership().getChit().getName(),
                payment.getTotalAmount().toString(), approved);
        auditService.log(admin, approved ? "APPROVE_PAYMENT" : "REJECT_PAYMENT",
                "Payment", paymentId, remarks);
    }

    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatus(Payment.PaymentStatus.PENDING);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Payment> getPaymentsForMembership(ChitMembership membership) {
        return paymentRepository.findByMembership(membership);
    }

    public BigDecimal getTotalPaid(ChitMembership membership) {
        BigDecimal result = paymentRepository.sumApprovedPaymentsByMembership(membership);
        return result != null ? result : BigDecimal.ZERO;
    }
}
