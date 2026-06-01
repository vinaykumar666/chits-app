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
    private final NotificationService notificationService;

    @Value("${ygc.late-fine-per-day:20}")
    private int lateFinePerDay;

    @Transactional
    public Payment submitPayment(Long membershipId, MultipartFile screenshot,
                                 Integer monthNumber, User member) throws Exception {

        // event=submitPayment() {"status":"START","membershipId":1,"monthNumber":2,"member":"user@ygc.com"}
        loggingUtil.entry("submitPayment",
                "membershipId", membershipId,
                "monthNumber", monthNumber,
                "member", member.getEmail());
        try {
            ChitMembership membership = membershipRepository.findById(membershipId)
                    .orElseThrow(() -> {
                        loggingUtil.failure("submitPayment",
                                new EntityNotFoundException("Membership not found"),
                                "membershipId", membershipId);
                        return new EntityNotFoundException("Membership not found");
                    });

            // Screenshot save
            String screenshotPath = "N/A";
            if (screenshot != null && !screenshot.isEmpty()) {
                String uploadDir = "uploads/payments/";
                new File(uploadDir).mkdirs();
                String filename = System.currentTimeMillis() + "_" + screenshot.getOriginalFilename();
                Path path = Paths.get(uploadDir + filename);
                Files.write(path, screenshot.getBytes());
                screenshotPath = path.toString();
                // event=submitPayment() {"status":"SCREENSHOT_SAVED","path":"uploads/payments/..."}
                loggingUtil.event("submitPayment", "SCREENSHOT_SAVED", "path", screenshotPath);
            }

            // Late fine calculation
            LocalDate dueDate = membership.getChit().getStartDate().plusMonths(monthNumber - 1);
            BigDecimal fine = BigDecimal.ZERO;
            if (LocalDate.now().isAfter(dueDate)) {
                long daysLate = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
                fine = BigDecimal.valueOf(daysLate * lateFinePerDay);
                // event=submitPayment() {"status":"LATE_FINE_APPLIED","daysLate":3,"fineAmount":60}
                loggingUtil.event("submitPayment", "LATE_FINE_APPLIED",
                        "daysLate", daysLate,
                        "fineAmount", fine);
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

            loggingUtil.db("submitPayment", "INSERT", "Payment");
            Payment saved = paymentRepository.save(payment);

            auditService.log(member, "SUBMIT_PAYMENT", "Payment", saved.getId(),
                    "Payment submitted for month " + monthNumber);

            // event=submitPayment() {"status":"SUCCESS","paymentId":5,"totalAmount":1020}
            loggingUtil.success("submitPayment",
                    "paymentId", saved.getId(),
                    "totalAmount", saved.getTotalAmount());
            return saved;

        } catch (Exception e) {
            loggingUtil.failure("submitPayment", e, "membershipId", membershipId);
            throw e;
        }
    }

    @Transactional
    public void verifyPayment(Long paymentId, boolean approved, String remarks, User admin) {

        // event=verifyPayment() {"status":"START","paymentId":5,"approved":true,"admin":"admin@ygc.com"}
        loggingUtil.entry("verifyPayment",
                "paymentId", paymentId,
                "approved", approved,
                "admin", admin.getEmail());
        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new EntityNotFoundException("Payment not found"));

            payment.setStatus(approved ? Payment.PaymentStatus.APPROVED : Payment.PaymentStatus.REJECTED);
            payment.setAdminRemarks(remarks);
            payment.setVerifiedBy(admin);
            payment.setVerifiedAt(java.time.LocalDateTime.now());

            loggingUtil.db("verifyPayment", "UPDATE", "Payment");
            paymentRepository.save(payment);

            if (approved) {
                try {
                    Chit chit = payment.getMembership().getChit();
                    BigDecimal commissionAmt = payment.getTotalAmount()
                            .multiply(chit.getAdminCommissionPercentage())
                            .divide(BigDecimal.valueOf(100));

                    CommissionLedger commission = new CommissionLedger();
                    commission.setChit(chit);
                    commission.setCommissionPercentage(chit.getAdminCommissionPercentage());
                    commission.setCommissionAmount(commissionAmt);
                    commission.setSource("MONTHLY_COLLECTION");
                    commission.setMonth(LocalDate.now());

                    loggingUtil.db("verifyPayment", "INSERT", "CommissionLedger");
                    commissionLedgerRepository.save(commission);

                    // event=verifyPayment() {"status":"COMMISSION_RECORDED","commissionAmount":51}
                    loggingUtil.event("verifyPayment", "COMMISSION_RECORDED",
                            "commissionAmount", commissionAmt);
                } catch (Exception e) {
                    loggingUtil.error("Failed to create commission ledger", "verifyPayment", e);
                }
            }

            User member = payment.getMembership().getUser();
            try {
                loggingUtil.externalCall("verifyPayment", "EmailService", "sendPaymentApproval");
                emailService.sendPaymentApproval(member.getEmail(), member.getFullName(),
                        payment.getMembership().getChit().getName(),
                        payment.getTotalAmount().toString(), approved);
            } catch (Exception e) {
                loggingUtil.error("Failed to send payment approval email", "verifyPayment", e);
            }

            auditService.log(admin, approved ? "APPROVE_PAYMENT" : "REJECT_PAYMENT",
                    "Payment", paymentId, remarks);
            loggingUtil.audit("verifyPayment", admin.getEmail(),
                    approved ? "APPROVE_PAYMENT" : "REJECT_PAYMENT");

            // Push real-time notification to the member
            String chitName = payment.getMembership().getChit().getName();
            if (approved) {
                notificationService.notifyPaymentReminder(
                        member.getEmail(), chitName,
                        "confirmed", payment.getTotalAmount().toPlainString());
            } else {
                notificationService.notifyPaymentDueAlert(member.getEmail(), chitName, 0);
            }

            // event=verifyPayment() {"status":"SUCCESS","paymentId":5,"decision":"APPROVED"}
            loggingUtil.success("verifyPayment",
                    "paymentId", paymentId,
                    "decision", approved ? "APPROVED" : "REJECTED");

        } catch (Exception e) {
            loggingUtil.failure("verifyPayment", e, "paymentId", paymentId);
            throw e;
        }
    }

    public List<Payment> getPendingPayments() {
        loggingUtil.db("getPendingPayments", "SELECT", "Payment");
        return paymentRepository.findByStatus(Payment.PaymentStatus.PENDING);
    }

    public List<Payment> getAllPayments() {
        loggingUtil.db("getAllPayments", "SELECT", "Payment");
        return paymentRepository.findAll();
    }

    public List<Payment> getPaymentsForMembership(ChitMembership membership) {
        // event=getPaymentsForMembership() {"status":"START","membershipId":3}
        loggingUtil.entry("getPaymentsForMembership", "membershipId", membership.getId());
        loggingUtil.db("getPaymentsForMembership", "SELECT", "Payment");
        return paymentRepository.findByMembership(membership);
    }

    public BigDecimal getTotalPaid(ChitMembership membership) {
        loggingUtil.entry("getTotalPaid", "membershipId", membership.getId());
        loggingUtil.db("getTotalPaid", "SELECT_AGG", "Payment");
        BigDecimal result = paymentRepository.sumApprovedPaymentsByMembership(membership);
        BigDecimal total = result != null ? result : BigDecimal.ZERO;
        // event=getTotalPaid() {"status":"SUCCESS","membershipId":3,"totalPaid":5000}
        loggingUtil.success("getTotalPaid", "membershipId", membership.getId(), "totalPaid", total);
        return total;
    }
}
