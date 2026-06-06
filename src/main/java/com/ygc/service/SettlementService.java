package com.ygc.service;

import com.ygc.audit.AuditService;
import com.ygc.model.*;
import com.ygc.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {
    private final SettlementRepository settlementRepository;
    private final ChitMembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Value("${ygc.early-exit-deduction-percentage:2}")
    private BigDecimal earlyExitDeduction;

    @Value("${ygc.cancellation-settlement-charge:1}")
    private BigDecimal cancellationChargePercentage;

    /**
     * Issue 10: Auto-generate settlement payments for all active members when a chit is cancelled.
     * Settlement charge percentage is configurable via ygc.cancellation-settlement-charge property.
     */
    @Transactional
    public List<Settlement> generateCancellationSettlements(Chit chit, User admin) {
        List<Settlement> settlements = new java.util.ArrayList<>();
        List<ChitMembership> activeMembers = membershipRepository.findByChit(chit).stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE)
                .toList();

        for (ChitMembership membership : activeMembers) {
            BigDecimal totalPaid = paymentRepository.sumApprovedPaymentsByMembership(membership);
            if (totalPaid == null) totalPaid = BigDecimal.ZERO;
            BigDecimal pendingFines = paymentRepository.sumLateFinesByMembership(membership);
            if (pendingFines == null) pendingFines = BigDecimal.ZERO;

            BigDecimal deduction = totalPaid.multiply(cancellationChargePercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal finalAmount = totalPaid.subtract(deduction).subtract(pendingFines);

            Settlement settlement = new Settlement();
            settlement.setMembership(membership);
            settlement.setTotalPaidAmount(totalPaid);
            settlement.setLateFines(pendingFines);
            settlement.setDeductionPercentage(cancellationChargePercentage);
            settlement.setDeductionAmount(deduction);
            settlement.setFinalSettlementAmount(finalAmount.max(BigDecimal.ZERO));
            settlement.setType(Settlement.SettlementType.EARLY_EXIT);
            settlement.setStatus(Settlement.SettlementStatus.PENDING);
            settlement.setAdminRemarks("Auto-generated: chit cancelled");
            Settlement saved = settlementRepository.save(settlement);
            settlements.add(saved);

            // Notify member
            User member = membership.getUser();
            try {
                notificationService.notifySettlementApproved(member.getEmail(),
                        chit.getName(), finalAmount.toPlainString());
                emailService.sendSettlementConfirmation(member.getEmail(), member.getFullName(),
                        chit.getName(), finalAmount.toPlainString());
            } catch (Exception ignored) {}

            auditService.log(admin, "CANCELLATION_SETTLEMENT", "Settlement", saved.getId(),
                    "Auto-settlement for cancelled chit: " + chit.getName()
                    + " | Member: " + member.getFullName()
                    + " | Amount: ₹" + finalAmount);
        }
        return settlements;
    }

    @Transactional
    public Settlement requestEarlyExit(Long membershipId, User member) {
        ChitMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found"));

        BigDecimal totalPaid = paymentRepository.sumApprovedPaymentsByMembership(membership);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        BigDecimal pendingFines = paymentRepository.sumLateFinesByMembership(membership);
        if (pendingFines == null) pendingFines = BigDecimal.ZERO;
        BigDecimal deduction = totalPaid.multiply(earlyExitDeduction).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = totalPaid.subtract(deduction).subtract(pendingFines);

        Settlement settlement = new Settlement();
        settlement.setMembership(membership);
        settlement.setTotalPaidAmount(totalPaid);
        settlement.setLateFines(pendingFines);
        settlement.setDeductionPercentage(earlyExitDeduction);
        settlement.setDeductionAmount(deduction);
        settlement.setFinalSettlementAmount(finalAmount.max(BigDecimal.ZERO));
        settlement.setType(Settlement.SettlementType.EARLY_EXIT);
        settlement.setStatus(Settlement.SettlementStatus.PENDING);
        Settlement saved = settlementRepository.save(settlement);
        auditService.log(member, "REQUEST_EARLY_EXIT", "Settlement", saved.getId(),
                "Early exit request. Final amount: \u20b9" + finalAmount);

        // Notify all admin users about early exit request
        try {
            membershipRepository.findByChit(membership.getChit()).stream()
                .map(ChitMembership::getUser)
                .filter(u -> u.getRole() == User.Role.ADMIN)
                .map(User::getEmail)
                .distinct()
                .forEach(adminEmail -> notificationService.notifyEarlyExitSubmitted(
                    adminEmail, member.getFullName(), membership.getChit().getName()));
            // Also notify chit creator directly
            if (membership.getChit().getCreatedBy() != null) {
                notificationService.notifyEarlyExitSubmitted(
                    membership.getChit().getCreatedBy().getEmail(),
                    member.getFullName(), membership.getChit().getName());
            }
        } catch (Exception ignored) {}

        return saved;
    }

    @Transactional
    public Settlement processMaturitySettlement(Long membershipId, BigDecimal profitShare, User admin) {
        ChitMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found"));

        BigDecimal totalPaid = paymentRepository.sumApprovedPaymentsByMembership(membership);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        Settlement settlement = new Settlement();
        settlement.setMembership(membership);
        settlement.setTotalPaidAmount(totalPaid);
        settlement.setProfitShare(profitShare);
        settlement.setFinalSettlementAmount(totalPaid.add(profitShare));
        settlement.setType(Settlement.SettlementType.MATURITY);
        settlement.setStatus(Settlement.SettlementStatus.PENDING);
        return settlementRepository.save(settlement);
    }

    @Transactional
    public void approveSettlement(Long settlementId, boolean approved, String remarks, User admin) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found"));
        settlement.setStatus(approved ? Settlement.SettlementStatus.APPROVED : Settlement.SettlementStatus.REJECTED);
        settlement.setAdminRemarks(remarks);
        settlement.setApprovedBy(admin);
        settlement.setProcessedAt(LocalDateTime.now());
        settlementRepository.save(settlement);

        User member = settlement.getMembership().getUser();
        String chitName = settlement.getMembership().getChit().getName();

        if (approved) {
            ChitMembership membership = settlement.getMembership();
            membership.setStatus(ChitMembership.MembershipStatus.SETTLED);
            membershipRepository.save(membership);
            emailService.sendSettlementConfirmation(member.getEmail(), member.getFullName(),
                    chitName, settlement.getFinalSettlementAmount().toString());
            notificationService.notifySettlementApproved(member.getEmail(), chitName,
                    settlement.getFinalSettlementAmount().toPlainString());
            if (settlement.getType() == Settlement.SettlementType.MATURITY) {
                notificationService.notifyChitMaturity(member.getEmail(), chitName);
            }
        } else {
            String reason = remarks != null ? remarks : "No reason provided";
            emailService.sendSettlementRejected(member.getEmail(), member.getFullName(), chitName, reason);
            notificationService.notifySettlementRejected(member.getEmail(), chitName, reason);
        }
        auditService.log(admin, approved ? "APPROVE_SETTLEMENT" : "REJECT_SETTLEMENT",
                "Settlement", settlementId, remarks);
    }

    public List<Settlement> getPendingSettlements() {
        return settlementRepository.findByStatus(Settlement.SettlementStatus.PENDING);
    }

    public List<Settlement> getAllSettlements() {
        return settlementRepository.findAll();
    }
}
