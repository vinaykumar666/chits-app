package com.ygc.service;

import com.ygc.audit.AuditService;
import com.ygc.model.*;
import com.ygc.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChitService {
    private final ChitRepository chitRepository;
    private final ChitMembershipRepository membershipRepository;
    private final AuditService auditService;
    private final EmailService emailService;

    @Transactional
    public Chit createChit(Chit chit, User admin) {
        BigDecimal totalValue = chit.getMonthlyAmount()
                .multiply(BigDecimal.valueOf(chit.getTotalMembers()));
        chit.setTotalChitValue(totalValue);
        chit.setCreatedBy(admin);
        chit.setEndDate(chit.getStartDate().plusMonths(chit.getDurationMonths()));
        Chit saved = chitRepository.save(chit);
        auditService.log(admin, "CREATE_CHIT", "Chit", saved.getId(), "Chit created: " + chit.getName());
        return saved;
    }

    @Transactional
    public ChitMembership requestJoin(Long chitId, User user) {
        Chit chit = chitRepository.findById(chitId)
                .orElseThrow(() -> new IllegalArgumentException("Chit not found"));

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
        ChitMembership saved = membershipRepository.save(membership);
        auditService.log(user, "JOIN_REQUEST", "ChitMembership", saved.getId(),
                "Join request for chit: " + chit.getName());
        return saved;
    }

    @Transactional
    public void approveMembership(Long membershipId, User admin) {
        ChitMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found"));
        membership.setStatus(ChitMembership.MembershipStatus.ACTIVE);
        membershipRepository.save(membership);
        auditService.log(admin, "APPROVE_MEMBERSHIP", "ChitMembership", membershipId,
                "Membership approved for user: " + membership.getUser().getEmail());
    }

    public List<Chit> getAvailableChits() {
        return chitRepository.findAvailableChits();
    }

    public List<Chit> getAllChits() {
        return chitRepository.findAll();
    }

    public Chit findById(Long id) {
        return chitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chit not found"));
    }

    public List<ChitMembership> getMembershipsForUser(User user) {
        return membershipRepository.findByUser(user);
    }

    public List<ChitMembership> getMembershipsForChit(Chit chit) {
        return membershipRepository.findByChit(chit);
    }
}