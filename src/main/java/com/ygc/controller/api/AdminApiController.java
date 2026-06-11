package com.ygc.controller.api;

import com.ygc.audit.AuditService;
import com.ygc.dto.*;
import com.ygc.model.*;
import com.ygc.repository.*;
import com.ygc.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminApiController {

    private final ApiSupport apiSupport;
    private final DtoMapper dtoMapper;
    private final UserRepository userRepository;
    private final ChitService chitService;
    private final PaymentService paymentService;
    private final AuctionService auctionService;
    private final SettlementService settlementService;
    private final ChitRepository chitRepository;
    private final BidCalculationService bidCalculationService;
    private final AuditLogRepository auditLogRepository;
    private final CommissionLedgerRepository commissionLedgerRepository;
    private final ChitMembershipRepository membershipRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ChitHistoryService chitHistoryService;
    private final AuditService auditService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(Authentication auth) {
        Map<String, Object> body = new HashMap<>();
        body.put("user", dtoMapper.toUserDto(apiSupport.currentUser(auth)));
        body.put("totalChits", chitRepository.count());
        body.put("totalMembers", userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.MEMBER).count());
        body.put("pendingPayments", paymentService.getPendingPayments().size());
        body.put("pendingSettlements", settlementService.getPendingSettlements().size());
        body.put("openAuctions", auctionService.getOpenAuctions().size());
        body.put("recentAudits", dtoMapper.toAuditLogDtos(auditLogRepository.findAll()
                .stream().sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(10).toList()));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/chits")
    public ResponseEntity<List<ChitDto>> listChits() {
        return ResponseEntity.ok(dtoMapper.toChitDtos(chitService.getAllChits()));
    }

    @PostMapping("/chits")
    public ResponseEntity<ChitDto> createChit(@RequestBody Map<String, Object> body, Authentication auth) {
        Chit chit = new Chit();
        chit.setName((String) body.get("name"));
        chit.setDescription((String) body.get("description"));
        chit.setMonthlyAmount(new BigDecimal(body.get("monthlyAmount").toString()));
        chit.setTotalMembers(Integer.valueOf(body.get("totalMembers").toString()));
        chit.setDurationMonths(Integer.valueOf(body.get("durationMonths").toString()));
        chit.setAdminCommissionPercentage(new BigDecimal(body.get("adminCommissionPercentage").toString()));
        if (body.get("minBidAmount") != null) chit.setMinBidAmount(new BigDecimal(body.get("minBidAmount").toString()));
        if (body.get("maxBidAmount") != null) chit.setMaxBidAmount(new BigDecimal(body.get("maxBidAmount").toString()));
        chit.setStartDate(LocalDate.parse(body.get("startDate").toString()));
        Chit created = chitService.createChit(chit, apiSupport.currentUser(auth));
        return ResponseEntity.ok(dtoMapper.toChitDto(created));
    }

    @GetMapping("/chits/{id}")
    public ResponseEntity<Map<String, Object>> chitDetail(@PathVariable Long id) {
        Chit chit = chitService.findById(id);
        return ResponseEntity.ok(Map.of(
                "chit", dtoMapper.toChitDto(chit),
                "memberships", dtoMapper.toMembershipDtos(chitService.getMembershipsForChit(chit)),
                "auctions", dtoMapper.toAuctionDtos(auctionService.getAuctionsByChit(chit))));
    }

    @PutMapping("/chits/{id}")
    public ResponseEntity<ChitDto> editChit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Chit chit = chitService.findById(id);
        chit.setName((String) body.get("name"));
        chit.setDescription((String) body.get("description"));
        chit.setMonthlyAmount(new BigDecimal(body.get("monthlyAmount").toString()));
        chit.setTotalMembers(Integer.valueOf(body.get("totalMembers").toString()));
        chit.setDurationMonths(Integer.valueOf(body.get("durationMonths").toString()));
        chit.setAdminCommissionPercentage(new BigDecimal(body.get("adminCommissionPercentage").toString()));
        if (body.get("minBidAmount") != null) chit.setMinBidAmount(new BigDecimal(body.get("minBidAmount").toString()));
        if (body.get("maxBidAmount") != null) chit.setMaxBidAmount(new BigDecimal(body.get("maxBidAmount").toString()));
        Chit.ChitStatus newStatus = Chit.ChitStatus.valueOf(body.get("status").toString());
        chit.setStatus(newStatus);
        String closingReason = body.get("closingReason") != null ? body.get("closingReason").toString() : null;
        if (newStatus == Chit.ChitStatus.CANCELLED || newStatus == Chit.ChitStatus.COMPLETED) {
            if (closingReason != null && !closingReason.isBlank()) chit.setClosingReason(closingReason);
        } else {
            chit.setClosingReason(null);
        }
        chitRepository.save(chit);
        String detail = "Name: " + chit.getName() + ", Status: " + newStatus;
        chitService.getMembershipsForChit(chit).stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE)
                .forEach(m -> {
                    notificationService.notifyChitUpdated(m.getUser().getEmail(), chit.getName(), detail);
                    emailService.sendChitUpdated(m.getUser().getEmail(), m.getUser().getFullName(), chit.getName(), detail);
                });
        return ResponseEntity.ok(dtoMapper.toChitDto(chit));
    }

    @DeleteMapping("/chits/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteChit(@PathVariable Long id) {
        Chit chit = chitService.findById(id);
        if (chit.getStatus() == Chit.ChitStatus.ACTIVE) {
            throw new IllegalStateException("Cannot delete an active chit. Cancel it first.");
        }
        chitRepository.delete(chit);
        chitRepository.flush();
        return ResponseEntity.ok(Map.of("message", "Chit deleted successfully."));
    }

    @PostMapping("/memberships/{id}/approve")
    public ResponseEntity<Map<String, String>> approveMembership(@PathVariable Long id, Authentication auth) {
        chitService.approveMembership(id, apiSupport.currentUser(auth));
        return ResponseEntity.ok(Map.of("message", "Membership approved! Agreement PDF sent."));
    }

    @PostMapping("/memberships/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectMembership(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null && body.get("reason") != null
                ? body.get("reason") : "Your request did not meet our criteria.";
        ChitMembership membership = membershipRepository.findById(id).orElseThrow();
        membership.setStatus(ChitMembership.MembershipStatus.EXITED);
        membership.setRejectionReason(reason);
        membershipRepository.save(membership);
        notificationService.notifyChitRegistrationRejected(
                membership.getUser().getEmail(), membership.getUser().getFullName(),
                membership.getChit().getName(), reason);
        emailService.sendMembershipRejected(
                membership.getUser().getEmail(), membership.getUser().getFullName(),
                membership.getChit().getName(), reason);
        return ResponseEntity.ok(Map.of("message", "Membership rejected. Member notified."));
    }

    @GetMapping("/payments")
    public ResponseEntity<Map<String, Object>> payments() {
        return ResponseEntity.ok(Map.of(
                "pendingPayments", dtoMapper.toPaymentDtos(paymentService.getPendingPayments()),
                "allPayments", dtoMapper.toPaymentDtos(paymentService.getAllPayments())));
    }

    @PostMapping("/payments/{id}/verify")
    public ResponseEntity<Map<String, String>> verifyPayment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String remarks = body.get("remarks") != null ? body.get("remarks").toString() : null;
        String reason = (remarks != null && !remarks.isBlank()) ? remarks
                : (approved ? "Approved by admin" : "Rejected by admin — contact admin for details");
        paymentService.verifyPayment(id, approved, reason, apiSupport.currentUser(auth));
        return ResponseEntity.ok(Map.of("message", "Payment " + (approved ? "approved" : "rejected")));
    }

    @GetMapping("/auctions")
    public ResponseEntity<Map<String, Object>> auctions() {
        Map<Long, Map<String, Object>> bidRecs = new HashMap<>();
        for (Auction auction : auctionService.getOpenAuctions()) {
            Chit chit = auction.getChit();
            int month = auction.getMonthNumber() != null ? auction.getMonthNumber() : 1;
            bidRecs.put(auction.getId(), bidCalculationService.calculateBidRecommendations(chit, month));
        }
        return ResponseEntity.ok(Map.of(
                "chits", dtoMapper.toChitDtos(chitService.getAllChits()),
                "openAuctions", dtoMapper.toAuctionDtos(auctionService.getOpenAuctions()),
                "allAuctions", dtoMapper.toAuctionDtos(auctionService.getAllAuctions()),
                "bidRecommendations", bidRecs));
    }

    @PostMapping("/auctions")
    public ResponseEntity<Map<String, String>> createAuction(@RequestBody Map<String, String> body, Authentication auth) {
        auctionService.createAuction(
                Long.valueOf(body.get("chitId")),
                Integer.valueOf(body.get("monthNumber")),
                LocalDate.parse(body.get("auctionDate")),
                chitRepository,
                apiSupport.currentUser(auth));
        return ResponseEntity.ok(Map.of("message", "Auction announced! Members notified."));
    }

    @PostMapping("/auctions/{id}/open")
    public ResponseEntity<Map<String, String>> openAuction(@PathVariable Long id, Authentication auth) {
        auctionService.openAuction(id, apiSupport.currentUser(auth));
        return ResponseEntity.ok(Map.of("message", "Auction open for bidding"));
    }

    @PostMapping("/auctions/{id}/close")
    public ResponseEntity<Map<String, String>> closeAuction(@PathVariable Long id, Authentication auth) {
        auctionService.closeAuction(id, apiSupport.currentUser(auth));
        return ResponseEntity.ok(Map.of("message", "Auction closed. Winner selected!"));
    }

    @PostMapping("/auctions/{id}/release-payout")
    public ResponseEntity<Map<String, String>> releasePayout(@PathVariable Long id, Authentication auth) {
        auctionService.releasePayout(id, apiSupport.currentUser(auth));
        return ResponseEntity.ok(Map.of("message", "Payout released!"));
    }

    @GetMapping("/settlements")
    public ResponseEntity<Map<String, Object>> settlements() {
        return ResponseEntity.ok(Map.of(
                "pendingSettlements", dtoMapper.toSettlementDtos(settlementService.getPendingSettlements()),
                "allSettlements", dtoMapper.toSettlementDtos(settlementService.getAllSettlements())));
    }

    @PostMapping("/settlements/{id}/process")
    public ResponseEntity<Map<String, String>> processSettlement(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String remarks = body.get("remarks") != null ? body.get("remarks").toString() : null;
        String reason = (remarks != null && !remarks.isBlank()) ? remarks
                : (approved ? "Approved by admin" : "Rejected by admin");
        settlementService.approveSettlement(id, approved, reason, apiSupport.currentUser(auth));
        return ResponseEntity.ok(Map.of("message", "Settlement " + (approved ? "approved" : "rejected")));
    }

    @GetMapping("/reports/commission")
    public ResponseEntity<Map<String, Object>> commissionReport() {
        List<CommissionLedger> ledger = commissionLedgerRepository.findAll();
        BigDecimal total = ledger.stream()
                .map(CommissionLedger::getCommissionAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return ResponseEntity.ok(Map.of(
                "ledger", dtoMapper.toCommissionLedgerDtos(ledger),
                "chits", dtoMapper.toChitDtos(chitService.getAllChits()),
                "totalCommission", total));
    }

    @GetMapping("/members")
    public ResponseEntity<List<UserDto>> members() {
        return ResponseEntity.ok(dtoMapper.toUserDtos(userRepository.findAll()));
    }

    @PostMapping("/members")
    public ResponseEntity<Map<String, Object>> createMember(@RequestBody RegisterRequest request) {
        User newUser = userService.registerUser(
                request.getEmail(), request.getFullName(), request.getPhone(), request.getAddress());
        return ResponseEntity.ok(Map.of(
                "message", "Member created. Temporary password emailed.",
                "user", dtoMapper.toUserDto(newUser)));
    }

    @GetMapping("/members/{id}")
    public ResponseEntity<UserDto> getMember(@PathVariable Long id) {
        return ResponseEntity.ok(dtoMapper.toUserDto(userService.findById(id)));
    }

    @PutMapping("/members/{id}")
    public ResponseEntity<UserDto> editMember(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        User member = userService.findById(id);
        String changes = "";
        String fullName = body.get("fullName").toString();
        if (!member.getFullName().equals(fullName)) changes += "Name updated. ";
        member.setFullName(fullName);
        if (body.get("phone") != null) {
            String phone = body.get("phone").toString();
            if (!phone.equals(member.getPhone())) changes += "Phone updated. ";
            member.setPhone(phone);
        }
        if (body.get("address") != null) member.setAddress(body.get("address").toString());
        if (body.get("role") != null && apiSupport.currentUser(auth).getRole() == User.Role.ADMIN) {
            member.setRole(User.Role.valueOf(body.get("role").toString()));
            changes += "Role changed. ";
        }
        if (body.get("active") != null) {
            boolean active = Boolean.TRUE.equals(body.get("active"));
            member.setActive(active);
            changes += "Status: " + (active ? "Active" : "Inactive") + ". ";
        }
        userRepository.save(member);
        if (!changes.isEmpty()) {
            notificationService.notifyUserUpdated(member.getEmail(), changes);
            emailService.sendUserUpdated(member.getEmail(), member.getFullName(), changes);
        }
        return ResponseEntity.ok(dtoMapper.toUserDto(member));
    }

    @PostMapping("/members/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetMemberPassword(@PathVariable Long id) {
        User member = userService.findById(id);
        String tempPass = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        member.setPassword(passwordEncoder.encode(tempPass));
        member.setFirstLogin(true);
        userRepository.save(member);
        emailService.sendRegistrationConfirmation(member.getEmail(), member.getFullName(), tempPass);
        notificationService.notifyUserUpdated(member.getEmail(), "Your password has been reset by admin.");
        return ResponseEntity.ok(Map.of("message", "Password reset. New temp password sent to member's email."));
    }

    @PostMapping("/members/{id}/toggle-status")
    public ResponseEntity<Map<String, String>> toggleMemberStatus(@PathVariable Long id) {
        User member = userService.findById(id);
        member.setActive(!member.isActive());
        userRepository.save(member);
        String status = member.isActive() ? "activated" : "deactivated";
        notificationService.notifyUserUpdated(member.getEmail(), "Your account has been " + status + " by admin.");
        return ResponseEntity.ok(Map.of("message", "Member " + status + " successfully."));
    }

    @DeleteMapping("/members/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteMember(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication auth) {
        User admin = apiSupport.currentUser(auth);
        User member = userService.findById(id);
        if (member.getRole() == User.Role.ADMIN) {
            throw new IllegalStateException("Cannot delete an admin user.");
        }
        String deleteReason = (reason != null && !reason.isBlank()) ? reason : "Removed by admin";
        List<ChitMembership> activeMemberships = membershipRepository.findByUser(member).stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE
                        || m.getStatus() == ChitMembership.MembershipStatus.PENDING)
                .toList();
        for (ChitMembership m : activeMemberships) {
            m.setStatus(ChitMembership.MembershipStatus.EXITED);
            m.setRejectionReason("User deleted: " + deleteReason);
            membershipRepository.save(m);
        }
        auditService.log(admin, "DELETE_USER", "User", member.getId(),
                "Deleted user: " + member.getFullName() + " (" + member.getEmail() + ") — " + deleteReason);
        try {
            emailService.sendAnnouncement(member.getEmail(), member.getFullName(),
                    "Account Removed — YGC Internal",
                    "Your YGC Internal account has been removed. Reason: " + deleteReason);
        } catch (Exception ignored) {}
        notificationService.notifyAdminUserDeleted(admin.getEmail(), member.getFullName());
        userRepository.delete(member);
        userRepository.flush();
        return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
    }

    @GetMapping("/announcements/members")
    public ResponseEntity<List<UserDto>> announcementTargets() {
        return ResponseEntity.ok(dtoMapper.toUserDtos(
                userRepository.findAll().stream()
                        .filter(u -> u.getRole() == User.Role.MEMBER).toList()));
    }

    @PostMapping("/announcements")
    public ResponseEntity<Map<String, String>> sendAnnouncement(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        String message = body.get("message");
        String target = body.getOrDefault("target", "all");
        List<User> recipients = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.MEMBER && u.isActive())
                .toList();
        if ("all".equals(target)) {
            notificationService.notifyAnnouncement(title, message);
            recipients.forEach(u -> emailService.sendAnnouncement(u.getEmail(), u.getFullName(), title, message));
            return ResponseEntity.ok(Map.of("message", "Announcement sent to " + recipients.size() + " members."));
        }
        User targetUser = userRepository.findByEmail(target).orElseThrow();
        notificationService.notifyAnnouncement(title, message);
        emailService.sendAnnouncement(targetUser.getEmail(), targetUser.getFullName(), title, message);
        return ResponseEntity.ok(Map.of("message", "Announcement sent to " + targetUser.getFullName() + "."));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AuditLogDto>> audit() {
        return ResponseEntity.ok(dtoMapper.toAuditLogDtos(auditLogRepository.findAll()
                .stream().sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp())).toList()));
    }

    @PostMapping("/chits/{chitId}/members")
    public ResponseEntity<Map<String, String>> addMemberToChit(
            @PathVariable Long chitId,
            @RequestBody Map<String, String> body) {
        Chit chit = chitService.findById(chitId);
        User member = userRepository.findByEmail(body.get("memberEmail"))
                .orElseThrow(() -> new RuntimeException("No user found with email: " + body.get("memberEmail")));
        if (membershipRepository.existsByChitAndUser(chit, member)) {
            throw new IllegalStateException("Member is already in this chit.");
        }
        ChitMembership membership = new ChitMembership();
        membership.setChit(chit);
        membership.setUser(member);
        membership.setStatus(ChitMembership.MembershipStatus.PENDING);
        membershipRepository.save(membership);
        notificationService.notifyUserUpdated(member.getEmail(),
                "You have been added to chit '" + chit.getName() + "' by admin.");
        return ResponseEntity.ok(Map.of("message", member.getFullName() + " added to chit as PENDING."));
    }

    @PostMapping("/chits/{chitId}/members/{membershipId}/remove")
    public ResponseEntity<Map<String, String>> removeMemberFromChit(
            @PathVariable Long chitId,
            @PathVariable Long membershipId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Chit chit = chitService.findById(chitId);
        ChitMembership m = membershipRepository.findById(membershipId).orElseThrow();
        User admin = apiSupport.currentUser(auth);
        String reason = body.get("reason");
        m.setStatus(ChitMembership.MembershipStatus.EXITED);
        m.setRejectionReason(reason);
        membershipRepository.save(m);
        emailService.sendMemberRemovedFromChit(m.getUser().getEmail(), m.getUser().getFullName(), chit.getName(), reason);
        emailService.sendMemberRemovedFromChit(admin.getEmail(), admin.getFullName(),
                "[Admin Copy] Removed " + m.getUser().getFullName() + " from " + chit.getName(), reason);
        notificationService.notifyUserUpdated(m.getUser().getEmail(),
                "You have been removed from chit '" + chit.getName() + "'. Reason: " + reason);
        return ResponseEntity.ok(Map.of("message", m.getUser().getFullName() + " removed from chit."));
    }

    @GetMapping("/chit-history")
    public ResponseEntity<Map<String, Object>> chitHistory() {
        List<ChitHistory> histories = chitHistoryService.findAll();
        return ResponseEntity.ok(Map.of(
                "histories", dtoMapper.toChitHistoryDtos(histories),
                "deletedCount", histories.stream().filter(h -> "DELETED".equals(h.getFinalStatus())).count(),
                "completedCount", histories.stream().filter(h -> "COMPLETED".equals(h.getFinalStatus())).count(),
                "cancelledCount", histories.stream().filter(h -> "CANCELLED".equals(h.getFinalStatus())).count()));
    }

    @PostMapping("/chits/{id}/delete-with-archive")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteChitWithArchive(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        Chit chit = chitService.findById(id);
        User admin = apiSupport.currentUser(auth);
        if (chit.getStatus() == Chit.ChitStatus.ACTIVE) {
            throw new IllegalStateException("Cannot delete an active chit — cancel it first.");
        }
        String reason = body != null && body.get("closingReason") != null ? body.get("closingReason") : "Deleted by admin";
        ChitHistory history = chitHistoryService.archiveChit(chit, "DELETED", reason, admin);
        if (history.getAnalysisPdfPath() != null) {
            String subject = "Chit Deleted & Archived — " + chit.getName();
            String html = "<p>Chit <strong>" + chit.getName() + "</strong> has been deleted and archived.</p>"
                    + "<p><strong>Reason:</strong> " + reason + "</p>";
            emailService.sendHtmlEmailWithAttachment(admin.getEmail(), subject, html,
                    history.getAnalysisPdfPath(),
                    "ChitAnalysis_" + chit.getName().replaceAll("[^a-zA-Z0-9_-]", "_") + ".pdf");
        }
        chitService.getMembershipsForChit(chit).stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE
                        || m.getStatus() == ChitMembership.MembershipStatus.PENDING)
                .forEach(m -> notificationService.notifyUserUpdated(m.getUser().getEmail(),
                        "Chit '" + chit.getName() + "' has been permanently closed. Reason: " + reason));
        chitRepository.delete(chit);
        chitRepository.flush();
        return ResponseEntity.ok(Map.of("message", "Chit deleted and archived."));
    }
}
