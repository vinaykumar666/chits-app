package com.ygc.service;

import com.ygc.audit.AuditService;
import com.ygc.exception.EntityNotFoundException;
import com.ygc.model.*;
import com.ygc.repository.*;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChitService {
    private final ChitRepository chitRepository;
    private final ChitMembershipRepository membershipRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final ChitAgreementService agreementService;
    private final LoggingUtil loggingUtil;
    private final NotificationService notificationService;

    @Transactional
    public Chit createChit(Chit chit, User admin) {
        loggingUtil.transactionStart("createChit", "ChitService");
        try {
            BigDecimal totalValue = chit.getMonthlyAmount()
                    .multiply(BigDecimal.valueOf(chit.getTotalMembers()));
            chit.setTotalChitValue(totalValue);
            chit.setCreatedBy(admin);
            chit.setEndDate(chit.getStartDate().plusMonths(chit.getDurationMonths()));

            loggingUtil.databaseOperation("INSERT", "Chit", "ChitService.createChit");
            Chit saved = chitRepository.save(chit);

            auditService.log(admin, "CREATE_CHIT", "Chit", saved.getId(), "Chit created: " + chit.getName());
            loggingUtil.transactionComplete("createChit", "ChitService");
            return saved;
        } catch (Exception e) {
            loggingUtil.transactionFailed("createChit", "ChitService", e);
            throw e;
        }
    }

    /**
     * Submits a join request after agreement acceptance.
     * All three agreement flags must be true or the request is rejected.
     */
    @Transactional
    public ChitMembership requestJoin(Long chitId, User user,
                                       boolean agreementRead,
                                       boolean termsAccepted,
                                       boolean infoProcessingAuthorized) {
        loggingUtil.transactionStart("requestJoin", "ChitService");
        try {
            Chit chit = chitRepository.findById(chitId)
                    .orElseThrow(() -> new EntityNotFoundException("Chit not found"));

            if (chit.getStatus() != Chit.ChitStatus.OPEN) {
                throw new IllegalStateException("Chit is not open for new members");
            }
            if (LocalDate.now().isAfter(chit.getStartDate())) {
                throw new IllegalStateException("Chit has already started; no new members allowed");
            }
            if (membershipRepository.existsByChitAndUser(chit, user)) {
                throw new IllegalStateException("Already requested or member of this chit");
            }
            long currentCount = membershipRepository.countByChitAndStatusNot(chit, ChitMembership.MembershipStatus.EXITED);
            if (currentCount >= chit.getTotalMembers()) {
                throw new IllegalStateException("Chit is full");
            }

            ChitMembership membership = new ChitMembership();
            membership.setChit(chit);
            membership.setUser(user);
            membership.setStatus(ChitMembership.MembershipStatus.PENDING);

            loggingUtil.databaseOperation("INSERT", "ChitMembership", "ChitService.requestJoin");
            ChitMembership saved = membershipRepository.save(membership);

            // Record agreement acceptance (validates all 3 checkboxes)
            agreementService.recordAgreementAcceptance(saved, agreementRead, termsAccepted, infoProcessingAuthorized);

            auditService.log(user, "JOIN_REQUEST", "ChitMembership", saved.getId(),
                    "Join request with agreement for chit: " + chit.getName());
            loggingUtil.transactionComplete("requestJoin", "ChitService");
            return saved;
        } catch (Exception e) {
            loggingUtil.transactionFailed("requestJoin", "ChitService", e);
            throw e;
        }
    }

    /** Legacy overload - kept for backward compatibility; requires all agreements to be true */
    @Transactional
    public ChitMembership requestJoin(Long chitId, User user) {
        return requestJoin(chitId, user, true, true, true);
    }

    @Transactional
    public void approveMembership(Long membershipId, User admin) {
        loggingUtil.transactionStart("approveMembership", "ChitService");
        try {
            ChitMembership membership = membershipRepository.findById(membershipId)
                    .orElseThrow(() -> new EntityNotFoundException("Membership not found"));

            membership.setStatus(ChitMembership.MembershipStatus.ACTIVE);
            loggingUtil.databaseOperation("UPDATE", "ChitMembership", "ChitService.approveMembership");
            membershipRepository.save(membership);

            // Generate & distribute signed agreement PDF
            try {
                agreementService.generateAndDistributeAgreementPdf(membership, admin);
            } catch (Exception e) {
                loggingUtil.error("Agreement PDF generation failed (non-fatal): " + e.getMessage(),
                        "ChitService.approveMembership", e);
            }

            auditService.log(admin, "APPROVE_MEMBERSHIP", "ChitMembership", membershipId,
                    "Membership approved for user: " + membership.getUser().getEmail());

            // Push chit registration approval notification
            notificationService.notifyChitRegistrationApproved(
                    membership.getUser().getEmail(),
                    membership.getUser().getFullName(),
                    membership.getChit().getName());

            loggingUtil.transactionComplete("approveMembership", "ChitService");
        } catch (Exception e) {
            loggingUtil.transactionFailed("approveMembership", "ChitService", e);
            throw e;
        }
    }

    public List<Chit> getAvailableChits() {
        loggingUtil.databaseOperation("SELECT", "Chit", "ChitService.getAvailableChits");
        return chitRepository.findAvailableChits();
    }

    public List<Chit> getAllChits() {
        loggingUtil.databaseOperation("SELECT", "Chit", "ChitService.getAllChits");
        return chitRepository.findAll();
    }

    public Chit findById(Long id) {
        loggingUtil.databaseOperation("SELECT", "Chit", "ChitService.findById");
        return chitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Chit not found"));
    }

    public List<ChitMembership> getMembershipsForUser(User user) {
        loggingUtil.databaseOperation("SELECT", "ChitMembership", "ChitService.getMembershipsForUser");
        return membershipRepository.findByUser(user);
    }

    public List<ChitMembership> getMembershipsForChit(Chit chit) {
        loggingUtil.databaseOperation("SELECT", "ChitMembership", "ChitService.getMembershipsForChit");
        return membershipRepository.findByChit(chit);
    }
}
