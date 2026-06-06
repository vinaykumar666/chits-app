package com.ygc.service;

import com.ygc.audit.AuditService;
import com.ygc.model.*;
import com.ygc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EarlyExitService {

    private final EarlyExitRequestRepository exitRepo;
    private final ChitMembershipRepository membershipRepo;
    private final PaymentRepository paymentRepo;
    private final AuditService auditService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${ygc.early-exit-deduction-percentage:2}")
    private double penaltyPct;

    @Transactional
    public EarlyExitRequest requestExit(ChitMembership membership, String reason) {
        if (exitRepo.existsByMembershipAndStatusIn(membership,
                List.of(EarlyExitRequest.ExitStatus.REQUESTED, EarlyExitRequest.ExitStatus.UNDER_REVIEW))) {
            throw new IllegalStateException("An exit request is already pending.");
        }

        BigDecimal totalPaid = paymentRepo.sumApprovedPaymentsByMembership(membership);
        BigDecimal penalty = totalPaid.multiply(BigDecimal.valueOf(penaltyPct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal refund = totalPaid.subtract(penalty);

        EarlyExitRequest req = new EarlyExitRequest();
        req.setMembership(membership);
        req.setReason(reason);
        req.setTotalPaid(totalPaid);
        req.setPenaltyPercentage(BigDecimal.valueOf(penaltyPct));
        req.setPenaltyAmount(penalty);
        req.setRefundAmount(refund);
        exitRepo.save(req);

        auditService.log(membership.getUser(), "EARLY_EXIT_REQUEST", "EarlyExitRequest",
                req.getId(), "Exit request for chit: " + membership.getChit().getName());

        // Notify admin
        if (membership.getChit().getCreatedBy() != null) {
            notificationService.notifyEarlyExitSubmitted(
                    membership.getChit().getCreatedBy().getEmail(),
                    membership.getUser().getFullName(),
                    membership.getChit().getName());
        }
        return req;
    }

    @Transactional
    public EarlyExitRequest processExit(Long requestId, boolean approved, String adminRemarks,
                                         User admin, User replacementMember) {
        EarlyExitRequest req = exitRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Exit request not found"));

        req.setReviewedBy(admin);
        req.setReviewedAt(LocalDateTime.now());
        req.setAdminRemarks(adminRemarks);
        req.setReplacementMember(replacementMember);

        if (approved) {
            req.setStatus(EarlyExitRequest.ExitStatus.APPROVED);
            // Exit the membership
            ChitMembership m = req.getMembership();
            m.setStatus(ChitMembership.MembershipStatus.EXITED);
            m.setRejectionReason("Early exit approved: " + adminRemarks);
            membershipRepo.save(m);

            // Issue 6: Notify member via SSE + email
            try {
                notificationService.notifySettlementApproved(m.getUser().getEmail(),
                        m.getChit().getName(), req.getRefundAmount().toPlainString());
                emailService.sendAnnouncement(m.getUser().getEmail(), m.getUser().getFullName(),
                        "Early Exit Approved — " + m.getChit().getName(),
                        "Your early exit has been approved. Refund: ₹" + req.getRefundAmount()
                        + " (Penalty: ₹" + req.getPenaltyAmount() + "). " + adminRemarks);
            } catch (Exception ignored) {}
        } else {
            req.setStatus(EarlyExitRequest.ExitStatus.REJECTED);
            // Issue 6: Notify member via SSE + email
            try {
                notificationService.notifySettlementRejected(
                        req.getMembership().getUser().getEmail(),
                        req.getMembership().getChit().getName(),
                        adminRemarks != null ? adminRemarks : "No reason provided");
                emailService.sendAnnouncement(
                        req.getMembership().getUser().getEmail(),
                        req.getMembership().getUser().getFullName(),
                        "Early Exit Rejected — " + req.getMembership().getChit().getName(),
                        "Your exit request was rejected. Reason: " + adminRemarks);
            } catch (Exception ignored) {}
        }

        auditService.log(admin, approved ? "APPROVE_EXIT" : "REJECT_EXIT",
                "EarlyExitRequest", requestId, adminRemarks);
        return exitRepo.save(req);
    }

    public List<EarlyExitRequest> getAllRequests() { return exitRepo.findAllWithDetails(); }
    public List<EarlyExitRequest> getPending() {
        return exitRepo.findByStatus(EarlyExitRequest.ExitStatus.REQUESTED);
    }
}
