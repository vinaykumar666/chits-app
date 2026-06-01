package com.ygc.service;

import com.ygc.audit.AuditService;
import com.ygc.exception.EntityNotFoundException;
import com.ygc.model.*;
import com.ygc.repository.*;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ChitMembershipRepository membershipRepository;
    private final CommissionLedgerRepository commissionLedgerRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final LoggingUtil loggingUtil;

    @Value("${ygc.late-fine-per-day:20}")
    private int lateFinePerDay;

    @Transactional
    public Payment submitPayment(Long membershipId, MultipartFile screenshot,
                                 Integer monthNumber, User member) throws Exception {
        loggingUtil.transactionStart("submitPayment", "PaymentService");
        try {
            loggingUtil.debug("Submitting payment for month: " + monthNumber, "PaymentService.submitPayment");

            ChitMembership membership = membershipRepository.findById(membershipId)
                    .orElseThrow(() -> {
                        loggingUtil.warn("Membership not found: " + membershipId, "PaymentService.submitPayment");
                        return new EntityNotFoundException("Membership not found");
                    });

            String screenshotPath = "N/A";
            if (screenshot != null && !screenshot.isEmpty()) {
                try {
                    String uploadDir = "uploads/payments/";
                    new File(uploadDir).mkdirs();
                    String filename = System.currentTimeMillis() + "_" + screenshot.getOriginalFilename();
                    Path path = Paths.get(uploadDir + filename);
                    Files.write(path, screenshot.getBytes());
                    screenshotPath = path.toString();
                    loggingUtil.debug("Payment screenshot saved to: " + screenshotPath, "PaymentService.submitPayment");
                } catch (Exception e) {
                    loggingUtil.error("Failed to save payment screenshot", "PaymentService.submitPayment", e);
                    throw e;
                }
            }

            LocalDate dueDate = membership.getChit().getStartDate().plusMonths(monthNumber - 1);
            BigDecimal fine = BigDecimal.ZERO;
            if (LocalDate.now().isAfter(dueDate)) {
                long daysLate = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
                fine = BigDecimal.valueOf(daysLate * lateFinePerDay);
                loggingUtil.info("Payment is " + daysLate + " days late. Fine: " + fine, "PaymentService.submitPayment");
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

            loggingUtil.databaseOperation("INSERT", "Payment", "PaymentService.submitPayment");
            Payment saved = paymentRepository.save(payment);

            auditService.log(member, "SUBMIT_PAYMENT", "Payment", saved.getId(),
                    "Payment submitted for month " + monthNumber);
            loggingUtil.transactionComplete("submitPayment", "PaymentService");
            loggingUtil.userAction(member.getEmail(), "SUBMIT_PAYMENT", "PaymentService.submitPayment");

            return saved;
        } catch (Exception e) {
            loggingUtil.transactionFailed("submitPayment", "PaymentService", e);
            throw e;
        }
    }

    @Transactional
    public void verifyPayment(Long paymentId, boolean approved, String remarks, User admin) {
        loggingUtil.transactionStart("verifyPayment", "PaymentService");
        try {
            loggingUtil.debug("Verifying payment: " + paymentId + ", Approved: " + approved, "PaymentService.verifyPayment");

            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> {
                        loggingUtil.warn("Payment not found: " + paymentId, "PaymentService.verifyPayment");
                        return new EntityNotFoundException("Payment not found");
                    });

            payment.setStatus(approved ? Payment.PaymentStatus.APPROVED : Payment.PaymentStatus.REJECTED);
            payment.setAdminRemarks(remarks);
            payment.setVerifiedBy(admin);
            payment.setVerifiedAt(java.time.LocalDateTime.now());

            loggingUtil.databaseOperation("UPDATE", "Payment", "PaymentService.verifyPayment");
            paymentRepository.save(payment);

            if (approved) {
                try {
                    Chit chit = payment.getMembership().getChit();
                    CommissionLedger commission = new CommissionLedger();
                    commission.setChit(chit);
                    commission.setCommissionPercentage(chit.getAdminCommissionPercentage());
                    commission.setCommissionAmount(payment.getTotalAmount()
                            .multiply(chit.getAdminCommissionPercentage())
                            .divide(BigDecimal.valueOf(100)));
                    commission.setSource("MONTHLY_COLLECTION");
                    commission.setMonth(LocalDate.now());

                    loggingUtil.databaseOperation("INSERT", "CommissionLedger", "PaymentService.verifyPayment");
                    commissionLedgerRepository.save(commission);
                    loggingUtil.info("Commission ledger created: " + commission.getCommissionAmount(), "PaymentService.verifyPayment");
                } catch (Exception e) {
                    loggingUtil.error("Failed to create commission ledger", "PaymentService.verifyPayment", e);
                    // Non-critical error, continue
                }
            }

            User member = payment.getMembership().getUser();
            try {
                emailService.sendPaymentApproval(member.getEmail(), member.getFullName(),
                        payment.getMembership().getChit().getName(),
                        payment.getTotalAmount().toString(), approved);
            } catch (Exception e) {
                loggingUtil.error("Failed to send payment approval email", "PaymentService.verifyPayment", e);
                // Non-critical, continue
            }

            auditService.log(admin, approved ? "APPROVE_PAYMENT" : "REJECT_PAYMENT",
                    "Payment", paymentId, remarks);
            loggingUtil.transactionComplete("verifyPayment", "PaymentService");
            loggingUtil.userAction(admin.getEmail(), approved ? "APPROVE_PAYMENT" : "REJECT_PAYMENT", "PaymentService.verifyPayment");

        } catch (Exception e) {
            loggingUtil.transactionFailed("verifyPayment", "PaymentService", e);
            throw e;
        }
    }

    public List<Payment> getPendingPayments() {
        try {
            loggingUtil.debug("Fetching pending payments", "PaymentService.getPendingPayments");
            loggingUtil.databaseOperation("SELECT", "Payment", "PaymentService.getPendingPayments");

            List<Payment> payments = paymentRepository.findByStatus(Payment.PaymentStatus.PENDING);
            loggingUtil.info("Retrieved " + payments.size() + " pending payments", "PaymentService.getPendingPayments");

            return payments;
        } catch (Exception e) {
            loggingUtil.error("Error fetching pending payments", "PaymentService.getPendingPayments", e);
            throw e;
        }
    }

    public List<Payment> getAllPayments() {
        try {
            loggingUtil.debug("Fetching all payments", "PaymentService.getAllPayments");
            loggingUtil.databaseOperation("SELECT", "Payment", "PaymentService.getAllPayments");

            List<Payment> payments = paymentRepository.findAll();
            loggingUtil.info("Retrieved " + payments.size() + " total payments", "PaymentService.getAllPayments");

            return payments;
        } catch (Exception e) {
            loggingUtil.error("Error fetching all payments", "PaymentService.getAllPayments", e);
            throw e;
        }
    }

    public List<Payment> getPaymentsForMembership(ChitMembership membership) {
        try {
            loggingUtil.debug("Fetching payments for membership: " + membership.getId(), "PaymentService.getPaymentsForMembership");
            loggingUtil.databaseOperation("SELECT", "Payment", "PaymentService.getPaymentsForMembership");

            List<Payment> payments = paymentRepository.findByMembership(membership);
            loggingUtil.info("Retrieved " + payments.size() + " payments for membership", "PaymentService.getPaymentsForMembership");

            return payments;
        } catch (Exception e) {
            loggingUtil.error("Error fetching membership payments", "PaymentService.getPaymentsForMembership", e);
            throw e;
        }
    }

    public BigDecimal getTotalPaid(ChitMembership membership) {
        try {
            loggingUtil.debug("Calculating total paid for membership: " + membership.getId(), "PaymentService.getTotalPaid");
            loggingUtil.databaseOperation("SELECT_AGGREGATE", "Payment", "PaymentService.getTotalPaid");

            BigDecimal result = paymentRepository.sumApprovedPaymentsByMembership(membership);
            BigDecimal total = result != null ? result : BigDecimal.ZERO;
            loggingUtil.info("Total paid: " + total, "PaymentService.getTotalPaid");

            return total;
        } catch (Exception e) {
            loggingUtil.error("Error calculating total paid", "PaymentService.getTotalPaid", e);
            throw e;
        }
    }
}
