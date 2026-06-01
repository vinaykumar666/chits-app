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
    private final LoggingUtil loggingUtil;

    @Transactional
    public Chit createChit(Chit chit, User admin) {
        loggingUtil.transactionStart("createChit", "ChitService");
        try {
            loggingUtil.debug("Creating chit: " + chit.getName(), "ChitService.createChit");

            BigDecimal totalValue = chit.getMonthlyAmount()
                    .multiply(BigDecimal.valueOf(chit.getTotalMembers()));
            chit.setTotalChitValue(totalValue);
            chit.setCreatedBy(admin);
            chit.setEndDate(chit.getStartDate().plusMonths(chit.getDurationMonths()));

            loggingUtil.databaseOperation("INSERT", "Chit", "ChitService.createChit");
            Chit saved = chitRepository.save(chit);

            auditService.log(admin, "CREATE_CHIT", "Chit", saved.getId(), "Chit created: " + chit.getName());
            loggingUtil.transactionComplete("createChit", "ChitService");
            loggingUtil.userAction(admin.getEmail(), "CREATE_CHIT", "ChitService.createChit");

            return saved;
        } catch (Exception e) {
            loggingUtil.transactionFailed("createChit", "ChitService", e);
            throw e;
        }
    }

    @Transactional
    public ChitMembership requestJoin(Long chitId, User user) {
        loggingUtil.transactionStart("requestJoin", "ChitService");
        try {
            loggingUtil.debug("User requesting to join chit: " + chitId, "ChitService.requestJoin");

            Chit chit = chitRepository.findById(chitId)
                    .orElseThrow(() -> {
                        loggingUtil.warn("Chit not found: " + chitId, "ChitService.requestJoin");
                        return new EntityNotFoundException("Chit not found");
                    });

            if (chit.getStatus() != Chit.ChitStatus.OPEN) {
                loggingUtil.businessRuleViolation("CHIT_NOT_OPEN", "ChitService.requestJoin",
                    "Chit status: " + chit.getStatus());
                throw new IllegalStateException("Chit is not open for new members");
            }

            if (LocalDate.now().isAfter(chit.getStartDate())) {
                loggingUtil.businessRuleViolation("CHIT_STARTED", "ChitService.requestJoin",
                    "Start date: " + chit.getStartDate());
                throw new IllegalStateException("Chit has already started; no new members allowed");
            }

            if (membershipRepository.existsByChitAndUser(chit, user)) {
                loggingUtil.businessRuleViolation("DUPLICATE_MEMBERSHIP", "ChitService.requestJoin",
                    "User: " + user.getEmail() + ", Chit: " + chitId);
                throw new IllegalStateException("Already requested or member of this chit");
            }

            long currentCount = membershipRepository.countByChitAndStatusNot(chit, ChitMembership.MembershipStatus.EXITED);
            if (currentCount >= chit.getTotalMembers()) {
                loggingUtil.businessRuleViolation("CHIT_FULL", "ChitService.requestJoin",
                    "Current: " + currentCount + ", Total: " + chit.getTotalMembers());
                throw new IllegalStateException("Chit is full");
            }

            ChitMembership membership = new ChitMembership();
            membership.setChit(chit);
            membership.setUser(user);
            membership.setStatus(ChitMembership.MembershipStatus.PENDING);

            loggingUtil.databaseOperation("INSERT", "ChitMembership", "ChitService.requestJoin");
            ChitMembership saved = membershipRepository.save(membership);

            auditService.log(user, "JOIN_REQUEST", "ChitMembership", saved.getId(),
                    "Join request for chit: " + chit.getName());
            loggingUtil.transactionComplete("requestJoin", "ChitService");
            loggingUtil.userAction(user.getEmail(), "JOIN_REQUEST", "ChitService.requestJoin");

            return saved;
        } catch (Exception e) {
            loggingUtil.transactionFailed("requestJoin", "ChitService", e);
            throw e;
        }
    }

    @Transactional
    public void approveMembership(Long membershipId, User admin) {
        loggingUtil.transactionStart("approveMembership", "ChitService");
        try {
            loggingUtil.debug("Approving membership: " + membershipId, "ChitService.approveMembership");

            ChitMembership membership = membershipRepository.findById(membershipId)
                    .orElseThrow(() -> {
                        loggingUtil.warn("Membership not found: " + membershipId, "ChitService.approveMembership");
                        return new EntityNotFoundException("Membership not found");
                    });

            membership.setStatus(ChitMembership.MembershipStatus.ACTIVE);
            loggingUtil.databaseOperation("UPDATE", "ChitMembership", "ChitService.approveMembership");
            membershipRepository.save(membership);

            auditService.log(admin, "APPROVE_MEMBERSHIP", "ChitMembership", membershipId,
                    "Membership approved for user: " + membership.getUser().getEmail());
            loggingUtil.transactionComplete("approveMembership", "ChitService");
            loggingUtil.userAction(admin.getEmail(), "APPROVE_MEMBERSHIP", "ChitService.approveMembership");

        } catch (Exception e) {
            loggingUtil.transactionFailed("approveMembership", "ChitService", e);
            throw e;
        }
    }

    public List<Chit> getAvailableChits() {
        try {
            loggingUtil.debug("Fetching available chits", "ChitService.getAvailableChits");
            loggingUtil.databaseOperation("SELECT", "Chit", "ChitService.getAvailableChits");

            List<Chit> chits = chitRepository.findAvailableChits();
            loggingUtil.info("Retrieved " + chits.size() + " available chits", "ChitService.getAvailableChits");

            return chits;
        } catch (Exception e) {
            loggingUtil.error("Error fetching available chits", "ChitService.getAvailableChits", e);
            throw e;
        }
    }

    public List<Chit> getAllChits() {
        try {
            loggingUtil.debug("Fetching all chits", "ChitService.getAllChits");
            loggingUtil.databaseOperation("SELECT", "Chit", "ChitService.getAllChits");

            List<Chit> chits = chitRepository.findAll();
            loggingUtil.info("Retrieved " + chits.size() + " chits", "ChitService.getAllChits");

            return chits;
        } catch (Exception e) {
            loggingUtil.error("Error fetching all chits", "ChitService.getAllChits", e);
            throw e;
        }
    }

    public Chit findById(Long id) {
        try {
            loggingUtil.debug("Finding chit by id: " + id, "ChitService.findById");
            loggingUtil.databaseOperation("SELECT", "Chit", "ChitService.findById");

            return chitRepository.findById(id)
                    .orElseThrow(() -> {
                        loggingUtil.warn("Chit not found: " + id, "ChitService.findById");
                        return new EntityNotFoundException("Chit not found");
                    });
        } catch (Exception e) {
            loggingUtil.error("Error finding chit", "ChitService.findById", e);
            throw e;
        }
    }

    public List<ChitMembership> getMembershipsForUser(User user) {
        try {
            loggingUtil.debug("Fetching memberships for user: " + user.getEmail(), "ChitService.getMembershipsForUser");
            loggingUtil.databaseOperation("SELECT", "ChitMembership", "ChitService.getMembershipsForUser");

            List<ChitMembership> memberships = membershipRepository.findByUser(user);
            loggingUtil.info("Retrieved " + memberships.size() + " memberships", "ChitService.getMembershipsForUser");

            return memberships;
        } catch (Exception e) {
            loggingUtil.error("Error fetching memberships", "ChitService.getMembershipsForUser", e);
            throw e;
        }
    }

    public List<ChitMembership> getMembershipsForChit(Chit chit) {
        try {
            loggingUtil.debug("Fetching memberships for chit: " + chit.getId(), "ChitService.getMembershipsForChit");
            loggingUtil.databaseOperation("SELECT", "ChitMembership", "ChitService.getMembershipsForChit");

            List<ChitMembership> memberships = membershipRepository.findByChit(chit);
            loggingUtil.info("Retrieved " + memberships.size() + " memberships", "ChitService.getMembershipsForChit");

            return memberships;
        } catch (Exception e) {
            loggingUtil.error("Error fetching chit memberships", "ChitService.getMembershipsForChit", e);
            throw e;
        }
    }
}