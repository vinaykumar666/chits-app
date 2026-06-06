package com.ygc.controller;

import com.ygc.audit.AuditService;
import com.ygc.model.*;
import com.ygc.repository.*;
import com.ygc.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
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
    private final SettlementRepository settlementRepository;
    private final ChitHistoryService chitHistoryService;
    private final ChitAgreementService chitAgreementService;
    private final EarlyExitService earlyExitService;
    private final RiskScoreService riskScoreService;
    private final LoginTrackingService loginTrackingService;
    private final AuditService auditService;
    private final EarlyExitRequestRepository earlyExitRequestRepository;

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    // ── Dashboard ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("totalChits", chitRepository.count());
        model.addAttribute("totalMembers", userRepository.findAll().stream().filter(u -> u.getRole() == User.Role.MEMBER).count());
        long pendingPay = 0; long pendingSettle = 0; long openAuct = 0;
        long pendingJoins = 0; long pendingExits = 0;
        try { pendingPay = paymentService.getPendingPayments().size(); } catch (Exception ignored) {}
        try { pendingSettle = settlementService.getPendingSettlements().size(); } catch (Exception ignored) {}
        try { openAuct = auctionService.getOpenAuctions().size(); } catch (Exception ignored) {}
        try { pendingJoins = membershipRepository.findAll().stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.PENDING).count(); } catch (Exception ignored) {}
        try { pendingExits = earlyExitService.getAllRequests().stream()
                .filter(r -> r.getStatus() == EarlyExitRequest.ExitStatus.REQUESTED).count(); } catch (Exception ignored) {}
        model.addAttribute("pendingPayments", pendingPay);
        model.addAttribute("pendingSettlements", pendingSettle);
        model.addAttribute("openAuctions", openAuct);
        model.addAttribute("pendingJoins", pendingJoins);
        model.addAttribute("pendingExits", pendingExits);
        model.addAttribute("recentAudits", auditLogRepository.findAll()
                .stream().sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(10).toList());

        // Smart Insights — auto-detected patterns
        List<String> insights = new java.util.ArrayList<>();
        try {
            if (pendingPay > 5) insights.add("⚠️ " + pendingPay + " payments awaiting verification — review queue is growing");
            if (pendingJoins > 0 && pendingJoins > 3) insights.add("📋 " + pendingJoins + " pending join requests — members are waiting for approval");
            long activeChits = chitRepository.findAll().stream().filter(c -> c.getStatus() == Chit.ChitStatus.ACTIVE).count();
            long totalMemberCount = userRepository.findAll().stream().filter(u -> u.getRole() == User.Role.MEMBER && u.isActive()).count();
            if (activeChits > 0 && totalMemberCount > 0) {
                double avgMembers = (double) membershipRepository.findAll().stream()
                        .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE).count() / activeChits;
                if (avgMembers < 3) insights.add("📉 Average " + String.format("%.1f", avgMembers) + " members per active chit — consider member outreach");
            }
            long lockedCount = userRepository.findAll().stream().filter(User::isAccountLocked).count();
            if (lockedCount > 0) insights.add("🔒 " + lockedCount + " account(s) locked — check Security Log");
            long overduePayments = 0;
            try { overduePayments = paymentService.getAllPayments().stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.OVERDUE).count(); } catch (Exception ignored) {}
            if (overduePayments > 0) insights.add("🔴 " + overduePayments + " overdue payment(s) across all chits");
            if (pendingExits > 0) insights.add("🚪 " + pendingExits + " early exit request(s) need review");
        } catch (Exception ignored) {}
        model.addAttribute("insights", insights);

        return "admin/dashboard";
    }

    // ── Chit Management ───────────────────────────────────────────────────
    @GetMapping("/chits")
    public String listChits(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("chits", chitService.getAllChits());
        return "admin/chits";
    }

    @GetMapping("/chits/create")
    public String createChitForm(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        return "admin/chit-create";
    }

    @PostMapping("/chits/create")
    public String createChit(@RequestParam String name, @RequestParam String description,
                             @RequestParam BigDecimal monthlyAmount, @RequestParam Integer totalMembers,
                             @RequestParam Integer durationMonths, @RequestParam BigDecimal adminCommissionPercentage,
                             @RequestParam(required = false) BigDecimal minBidAmount,
                             @RequestParam(required = false) BigDecimal maxBidAmount,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                             Authentication auth, RedirectAttributes ra) {
        try {
            Chit chit = new Chit();
            chit.setName(name); chit.setDescription(description);
            chit.setMonthlyAmount(monthlyAmount); chit.setTotalMembers(totalMembers);
            chit.setDurationMonths(durationMonths); chit.setAdminCommissionPercentage(adminCommissionPercentage);
            chit.setMinBidAmount(minBidAmount); chit.setMaxBidAmount(maxBidAmount);
            chit.setStartDate(startDate);
            chitService.createChit(chit, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Chit created successfully!");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/chits";
    }

    @GetMapping("/chits/{id}")
    public String chitDetail(@PathVariable Long id, Model model, Authentication auth) {
        Chit chit = chitService.findById(id);
        List<ChitMembership> memberships = chitService.getMembershipsForChit(chit);
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("chit", chit);
        model.addAttribute("memberships", memberships);
        model.addAttribute("auctions", auctionService.getAuctionsByChit(chit));

        // Issue 11: Chit-level analytics — wrapped in try-catch for safety
        BigDecimal totalCollected = BigDecimal.ZERO;
        long paidMembers = 0;
        long unpaidMembers = 0;
        long overdueCount = 0;
        try {
            for (ChitMembership m : memberships) {
                if (m.getStatus() == ChitMembership.MembershipStatus.ACTIVE) {
                    BigDecimal memberPaid = paymentService.getTotalPaid(m);
                    if (memberPaid == null) memberPaid = BigDecimal.ZERO;
                    totalCollected = totalCollected.add(memberPaid);
                    if (memberPaid.compareTo(BigDecimal.ZERO) > 0) paidMembers++;
                    else unpaidMembers++;
                    try {
                        List<Payment> memberPayments = paymentService.getPaymentsForMembership(m);
                        overdueCount += memberPayments.stream()
                                .filter(p -> p.getStatus() == Payment.PaymentStatus.OVERDUE).count();
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            // Analytics failure is non-fatal
        }

        BigDecimal totalChitValue = chit.getTotalChitValue() != null ? chit.getTotalChitValue() : BigDecimal.ZERO;
        BigDecimal totalPending = totalChitValue.subtract(totalCollected).max(BigDecimal.ZERO);
        int collectionPct = 0;
        if (totalChitValue.compareTo(BigDecimal.ZERO) > 0) {
            collectionPct = totalCollected.multiply(BigDecimal.valueOf(100))
                    .divide(totalChitValue, 0, java.math.RoundingMode.HALF_UP).intValue();
            collectionPct = Math.min(100, Math.max(0, collectionPct));
        }

        model.addAttribute("totalCollected", totalCollected);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("paidMembers", paidMembers);
        model.addAttribute("unpaidMembers", unpaidMembers);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("activeMembers", memberships.stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE).count());
        model.addAttribute("collectionPct", collectionPct);

        // Pre-compute per-member payment stats (avoids lazy-loading m.payments in template)
        java.util.Map<Long, BigDecimal> memberPaidMap = new java.util.HashMap<>();
        java.util.Map<Long, Long> memberOverdueMap = new java.util.HashMap<>();
        java.util.Map<Long, Long> memberPaidMonthsMap = new java.util.HashMap<>();
        for (ChitMembership m : memberships) {
            try {
                BigDecimal paid = paymentService.getTotalPaid(m);
                memberPaidMap.put(m.getId(), paid != null ? paid : BigDecimal.ZERO);
                List<Payment> mPayments = paymentService.getPaymentsForMembership(m);
                memberOverdueMap.put(m.getId(), mPayments.stream()
                        .filter(p -> p.getStatus() == Payment.PaymentStatus.OVERDUE).count());
                memberPaidMonthsMap.put(m.getId(), mPayments.stream()
                        .filter(p -> p.getStatus() == Payment.PaymentStatus.APPROVED).count());
            } catch (Exception e) {
                memberPaidMap.put(m.getId(), BigDecimal.ZERO);
                memberOverdueMap.put(m.getId(), 0L);
                memberPaidMonthsMap.put(m.getId(), 0L);
            }
        }
        model.addAttribute("memberPaidMap", memberPaidMap);
        model.addAttribute("memberOverdueMap", memberOverdueMap);
        model.addAttribute("memberPaidMonthsMap", memberPaidMonthsMap);

        return "admin/chit-detail";
    }

    // ── Edit Chit ──────────────────────────────────────────────────────────
    @GetMapping("/chits/{id}/edit")
    public String editChitForm(@PathVariable Long id, Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("chit", chitService.findById(id));
        return "admin/chit-edit";
    }

    @PostMapping("/chits/{id}/edit")
    public String editChit(@PathVariable Long id,
                           @RequestParam String name, @RequestParam String description,
                           @RequestParam BigDecimal monthlyAmount, @RequestParam Integer totalMembers,
                           @RequestParam Integer durationMonths, @RequestParam BigDecimal adminCommissionPercentage,
                           @RequestParam(required = false) BigDecimal minBidAmount,
                           @RequestParam(required = false) BigDecimal maxBidAmount,
                           @RequestParam String status,
                           @RequestParam(required = false) String closingReason,
                           Authentication auth, RedirectAttributes ra) {
        try {
            Chit chit = chitService.findById(id);
            chit.setName(name); chit.setDescription(description);
            chit.setMonthlyAmount(monthlyAmount); chit.setTotalMembers(totalMembers);
            chit.setDurationMonths(durationMonths); chit.setAdminCommissionPercentage(adminCommissionPercentage);
            chit.setMinBidAmount(minBidAmount); chit.setMaxBidAmount(maxBidAmount);
            Chit.ChitStatus newStatus = Chit.ChitStatus.valueOf(status);
            chit.setStatus(newStatus);
            // Store closing reason when admin cancels or completes a chit
            if (newStatus == Chit.ChitStatus.CANCELLED || newStatus == Chit.ChitStatus.COMPLETED) {
                if (closingReason != null && !closingReason.isBlank()) {
                    chit.setClosingReason(closingReason);
                }
            } else {
                chit.setClosingReason(null);
            }
            chitRepository.save(chit);

            // Issue 10: Auto-generate cancellation settlements when chit is cancelled
            if (newStatus == Chit.ChitStatus.CANCELLED) {
                try {
                    List<Settlement> settlements = settlementService.generateCancellationSettlements(chit, getCurrentUser(auth));
                    if (!settlements.isEmpty()) {
                        ra.addFlashAttribute("info", settlements.size() + " settlement(s) auto-generated for affected members.");
                    }
                } catch (Exception e) {
                    ra.addFlashAttribute("warning", "Settlements could not be auto-generated: " + e.getMessage());
                }
            }

            // Notify all active members of this chit
            String detail = "Name: " + name + ", Status: " + status;
            chitService.getMembershipsForChit(chit).stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE)
                .forEach(m -> {
                    notificationService.notifyChitUpdated(m.getUser().getEmail(), name, detail);
                    emailService.sendChitUpdated(m.getUser().getEmail(), m.getUser().getFullName(), name, detail);
                });
            ra.addFlashAttribute("success", "Chit updated and members notified!");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/chits/" + id;
    }

    @PostMapping("/chits/{id}/delete")
    @org.springframework.transaction.annotation.Transactional
    public String deleteChit(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            Chit chit = chitService.findById(id);
            if (chit.getStatus() == Chit.ChitStatus.ACTIVE) {
                ra.addFlashAttribute("error", "Cannot delete an active chit. Cancel it first.");
                return "redirect:/admin/chits/" + id;
            }
            // Use entity-based delete to trigger cascade (not deleteById which can skip)
            chitRepository.delete(chit);
            chitRepository.flush();
            ra.addFlashAttribute("success", "Chit deleted successfully.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/chits";
    }

    // ── Membership Approve/Reject (with reason) ───────────────────────────
    @PostMapping("/memberships/{id}/approve")
    public String approveMembership(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            ChitMembership membership = membershipRepository.findById(id).orElseThrow();
            Long chitId = membership.getChit().getId();
            // Issue 6: Require agreement acceptance before admin can approve
            if (!membership.isAgreementAccepted()) {
                ra.addFlashAttribute("error", "Cannot approve: Member has not accepted the agreement yet. Wait for member to review and accept.");
                return "redirect:/admin/chits/" + chitId;
            }
            chitService.approveMembership(id, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Membership approved! Agreement PDF sent.");
            return "redirect:/admin/chits/" + chitId;
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/chits";
    }

    @PostMapping("/memberships/{id}/reject")
    public String rejectMembership(@PathVariable Long id,
                                   @RequestParam(required = false, defaultValue = "Your request did not meet our criteria.") String reason,
                                   Authentication auth, RedirectAttributes ra) {
        try {
            ChitMembership membership = membershipRepository.findById(id).orElseThrow();
            Long chitId = membership.getChit().getId();
            membership.setStatus(ChitMembership.MembershipStatus.EXITED);
            membership.setRejectionReason(reason);
            membership.setRejectionCount(membership.getRejectionCount() + 1);
            membershipRepository.save(membership);

            notificationService.notifyChitRegistrationRejected(
                membership.getUser().getEmail(), membership.getUser().getFullName(),
                membership.getChit().getName(), reason);
            emailService.sendMembershipRejected(
                membership.getUser().getEmail(), membership.getUser().getFullName(),
                membership.getChit().getName(), reason);

            ra.addFlashAttribute("success", "Membership rejected. Member has been notified with reason.");
            return "redirect:/admin/chits/" + chitId;
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/chits";
    }

    // ── Payment Verification (with reason) ────────────────────────────────
    @GetMapping("/payments")
    public String pendingPayments(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        try {
            model.addAttribute("payments", paymentService.getPendingPayments());
        } catch (Exception e) {
            model.addAttribute("payments", java.util.Collections.emptyList());
            model.addAttribute("error", "Failed to load pending payments: " + e.getMessage());
        }
        try {
            model.addAttribute("allPayments", paymentService.getAllPayments());
        } catch (Exception e) {
            model.addAttribute("allPayments", java.util.Collections.emptyList());
        }
        return "admin/payments";
    }

    @PostMapping("/payments/{id}/verify")
    public String verifyPayment(@PathVariable Long id,
                                @RequestParam boolean approved,
                                @RequestParam(required = false) String remarks,
                                Authentication auth, RedirectAttributes ra) {
        try {
            String reason = (remarks != null && !remarks.isBlank()) ? remarks : (approved ? "Approved by admin" : "Rejected by admin — contact admin for details");
            paymentService.verifyPayment(id, approved, reason, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Payment " + (approved ? "approved" : "rejected") + ". Member notified via push & email.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/payments";
    }

    @GetMapping("/payments/{id}/approve")
    public String approvePaymentPage(@PathVariable Long id, Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("payment", paymentService.getAllPayments().stream()
                .filter(p -> p.getId().equals(id)).findFirst().orElseThrow());
        return "admin/payments-approve";
    }

    @PostMapping("/payments/{id}/approve")
    public String approvePayment(@PathVariable Long id,
                                 @RequestParam(required = false, defaultValue = "Approved") String remarks,
                                 Authentication auth, RedirectAttributes ra) {
        try {
            paymentService.verifyPayment(id, true, remarks, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Payment approved");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/payments";
    }

    // ── Auction Management ────────────────────────────────────────────────
    @GetMapping("/auctions")
    public String auctions(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("chits", chitService.getAllChits());
        model.addAttribute("openAuctions", auctionService.getOpenAuctions());
        model.addAttribute("allAuctions", auctionService.getAllAuctions());
        java.util.Map<Long, java.util.Map<String, Object>> bidRecs = new java.util.HashMap<>();
        for (Auction auction : auctionService.getOpenAuctions()) {
            Chit chit = auction.getChit();
            int month = auction.getMonthNumber() != null ? auction.getMonthNumber() : 1;
            bidRecs.put(auction.getId(), bidCalculationService.calculateBidRecommendations(chit, month));
        }
        model.addAttribute("bidRecommendations", bidRecs);
        return "admin/auctions";
    }

    @PostMapping("/auctions/create")
    public String createAuction(@RequestParam Long chitId, @RequestParam Integer monthNumber,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate auctionDate,
                                Authentication auth, RedirectAttributes ra) {
        try {
            auctionService.createAuction(chitId, monthNumber, auctionDate, chitRepository, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Auction announced! Members notified.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/auctions";
    }

    @PostMapping("/auctions/{id}/open")
    public String openAuction(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try { auctionService.openAuction(id, getCurrentUser(auth)); ra.addFlashAttribute("success", "Auction open for bidding"); }
        catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/auctions";
    }

    @PostMapping("/auctions/{id}/close")
    public String closeAuction(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try { auctionService.closeAuction(id, getCurrentUser(auth)); ra.addFlashAttribute("success", "Auction closed. Winner selected!"); }
        catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/auctions";
    }

    @PostMapping("/auctions/{id}/release-payout")
    public String releasePayout(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try { auctionService.releasePayout(id, getCurrentUser(auth)); ra.addFlashAttribute("success", "Payout released!"); }
        catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/auctions";
    }

    // ── Settlement Management (with reason) ──────────────────────────────
    @GetMapping("/settlements")
    public String settlements(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("settlements", settlementService.getPendingSettlements());
        model.addAttribute("allSettlements", settlementService.getAllSettlements());
        return "admin/settlements";
    }

    @PostMapping("/settlements/{id}/process")
    public String processSettlement(@PathVariable Long id,
                                    @RequestParam boolean approved,
                                    @RequestParam(required = false) String remarks,
                                    Authentication auth, RedirectAttributes ra) {
        try {
            String reason = (remarks != null && !remarks.isBlank()) ? remarks : (approved ? "Approved by admin" : "Rejected by admin");
            settlementService.approveSettlement(id, approved, reason, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Settlement " + (approved ? "approved" : "rejected") + ". Member notified.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/settlements";
    }

    // ── Commission Reports ────────────────────────────────────────────────
    @GetMapping("/reports/commission")
    public String commissionReport(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("ledger", commissionLedgerRepository.findAll());
        model.addAttribute("chits", chitService.getAllChits());
        BigDecimal totalCommission = commissionLedgerRepository.findAll().stream()
                .map(CommissionLedger::getCommissionAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalCommission", totalCommission);
        return "admin/commission-report";
    }

    // ── Member Management ─────────────────────────────────────────────────
    @GetMapping("/members")
    public String members(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("members", userRepository.findAll());
        return "admin/members";
    }

    // ── Admin Create User ──────────────────────────────────────────────────
    @PostMapping("/members/create")
    public String adminCreateUser(@RequestParam String email,
                                  @RequestParam String fullName,
                                  @RequestParam(required = false) String phone,
                                  @RequestParam(required = false) String address,
                                  Authentication auth, RedirectAttributes ra) {
        try {
            User newUser = userService.registerUser(email, fullName, phone, address);
            ra.addFlashAttribute("success",
                    "Member '" + fullName + "' created. Temporary password emailed to " + email + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Create member failed: " + e.getMessage());
        }
        return "redirect:/admin/members";
    }

    @GetMapping("/members/{id}/edit")
    public String editMemberForm(@PathVariable Long id, Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("member", userService.findById(id));
        return "admin/member-edit";
    }

    @PostMapping("/members/{id}/edit")
    public String editMember(@PathVariable Long id,
                             @RequestParam String fullName,
                             @RequestParam String phone,
                             @RequestParam String address,
                             @RequestParam(required = false) String role,
                             @RequestParam(required = false) Boolean active,
                             Authentication auth, RedirectAttributes ra) {
        try {
            User member = userService.findById(id);
            String changes = "";
            if (!member.getFullName().equals(fullName)) changes += "Name updated. ";
            if (phone != null && !phone.equals(member.getPhone())) changes += "Phone updated. ";

            member.setFullName(fullName);
            if (phone != null) member.setPhone(phone);
            if (address != null) member.setAddress(address);
            if (role != null && getCurrentUser(auth).getRole() == User.Role.ADMIN) {
                member.setRole(User.Role.valueOf(role));
                changes += "Role changed to " + role + ". ";
            }
            if (active != null) {
                member.setActive(active);
                changes += "Status: " + (active ? "Active" : "Inactive") + ". ";
            }
            userRepository.save(member);

            // Notify user of profile update
            if (!changes.isEmpty()) {
                notificationService.notifyUserUpdated(member.getEmail(), changes);
                emailService.sendUserUpdated(member.getEmail(), member.getFullName(), changes);
            }
            ra.addFlashAttribute("success", "Member updated and notified!");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/members";
    }

    @PostMapping("/members/{id}/reset-password")
    public String resetMemberPassword(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            User member = userService.findById(id);
            String tempPass = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            member.setPassword(passwordEncoder.encode(tempPass));
            member.setFirstLogin(true);
            userRepository.save(member);
            emailService.sendRegistrationConfirmation(member.getEmail(), member.getFullName(), tempPass);
            notificationService.notifyUserUpdated(member.getEmail(), "Your password has been reset by admin. Check email for new temp password.");
            ra.addFlashAttribute("success", "Password reset. New temp password sent to member's email.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/members";
    }

    @PostMapping("/members/{id}/toggle-status")
    public String toggleMemberStatus(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            User member = userService.findById(id);
            member.setActive(!member.isActive());
            userRepository.save(member);
            String status = member.isActive() ? "activated" : "deactivated";
            notificationService.notifyUserUpdated(member.getEmail(), "Your account has been " + status + " by admin.");
            ra.addFlashAttribute("success", "Member " + status + " successfully.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/members";
    }

    // ── Delete User Completely ────────────────────────────────────────────
    @PostMapping("/members/{id}/delete")
    @org.springframework.transaction.annotation.Transactional
    public String deleteMember(@PathVariable Long id, @RequestParam(required = false) String reason,
                               Authentication auth, RedirectAttributes ra) {
        try {
            User admin = getCurrentUser(auth);
            User member = userService.findById(id);

            if (member.getRole() == User.Role.ADMIN) {
                ra.addFlashAttribute("error", "Cannot delete an admin user.");
                return "redirect:/admin/members";
            }

            String memberName = member.getFullName();
            String memberEmail = member.getEmail();
            String deleteReason = (reason != null && !reason.isBlank()) ? reason : "Removed by admin";

            // 1. Exit all active/pending memberships
            List<ChitMembership> memberships = membershipRepository.findByUser(member);
            int exitedCount = 0;
            for (ChitMembership m : memberships) {
                if (m.getStatus() == ChitMembership.MembershipStatus.ACTIVE
                        || m.getStatus() == ChitMembership.MembershipStatus.PENDING) {
                    m.setStatus(ChitMembership.MembershipStatus.EXITED);
                    m.setRejectionReason("User permanently deleted: " + deleteReason);
                    membershipRepository.save(m);
                    exitedCount++;
                }
            }

            // 2. Comprehensive audit BEFORE deletion (preserved permanently)
            auditService.log(admin, "PERMANENT_DELETE_USER", "User", member.getId(),
                    "PERMANENTLY DELETED — Name: " + memberName
                    + " | Email: " + memberEmail
                    + " | Phone: " + (member.getPhone() != null ? member.getPhone() : "N/A")
                    + " | Address: " + (member.getAddress() != null ? member.getAddress() : "N/A")
                    + " | Aadhaar: " + (member.getAadhaarNumber() != null ? "XXXX" + member.getAadhaarNumber().substring(Math.max(0, member.getAadhaarNumber().length() - 4)) : "N/A")
                    + " | Memberships exited: " + exitedCount
                    + " | Reason: " + deleteReason);

            // 3. Email notification to the deleted user
            try {
                emailService.sendAnnouncement(memberEmail, memberName,
                        "Account Permanently Removed — YGC Internal",
                        "Your YGC Internal account has been permanently removed. Reason: " + deleteReason
                        + ". If you believe this is an error, contact admin at +91 8919508889.");
            } catch (Exception ignored) {}

            // 4. Nullify all FK references to this user before hard-delete
            // Audit logs: set user to null (audit text already has the user info)
            auditLogRepository.findAll().stream()
                    .filter(a -> a.getUser() != null && a.getUser().getId().equals(id))
                    .forEach(a -> { a.setUser(null); auditLogRepository.save(a); });

            // Login history: delete all records for this user
            try {
                loginTrackingService.deleteByUser(member);
            } catch (Exception ignored) {}

            // Early exit requests: nullify reviewer references
            try {
                earlyExitRequestRepository.findAll().stream()
                        .filter(e -> e.getReviewedBy() != null && e.getReviewedBy().getId().equals(id))
                        .forEach(e -> { e.setReviewedBy(null); earlyExitRequestRepository.save(e); });
            } catch (Exception ignored) {}

            // Settlements: nullify approvedBy
            try {
                settlementRepository.findAll().stream()
                        .filter(s -> s.getApprovedBy() != null && s.getApprovedBy().getId().equals(id))
                        .forEach(s -> { s.setApprovedBy(null); settlementRepository.save(s); });
            } catch (Exception ignored) {}

            // Chits: nullify createdBy
            try {
                chitRepository.findAll().stream()
                        .filter(c -> c.getCreatedBy() != null && c.getCreatedBy().getId().equals(id))
                        .forEach(c -> { c.setCreatedBy(null); chitRepository.save(c); });
            } catch (Exception ignored) {}

            // 5. Delete all memberships (cascade deletes payments, bids, etc.)
            for (ChitMembership m : memberships) {
                membershipRepository.delete(m);
            }
            membershipRepository.flush();

            // 6. HARD DELETE the user — email is now freed for re-registration
            userRepository.delete(member);
            userRepository.flush();

            ra.addFlashAttribute("success",
                    "User '" + memberName + "' permanently deleted. Email " + memberEmail
                    + " is now available for re-registration. " + exitedCount + " membership(s) exited. Full details in Audit Log.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }
        return "redirect:/admin/members";
    }

    // ── Early Exit Management ──────────────────────────────────────────────
    @GetMapping("/early-exits")
    public String earlyExits(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        try {
            model.addAttribute("requests", earlyExitService.getAllRequests());
        } catch (Exception e) {
            model.addAttribute("requests", java.util.Collections.emptyList());
            model.addAttribute("error", "Failed to load exit requests: " + e.getMessage());
        }
        return "admin/early-exits";
    }

    @PostMapping("/early-exits/{id}/process")
    public String processEarlyExit(@PathVariable Long id,
                                    @RequestParam boolean approved,
                                    @RequestParam(required = false) String remarks,
                                    Authentication auth, RedirectAttributes ra) {
        try {
            earlyExitService.processExit(id, approved, remarks != null ? remarks : "", getCurrentUser(auth), null);
            ra.addFlashAttribute("success", (approved ? "Approved" : "Rejected") + " early exit request #" + id);
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/early-exits";
    }

    // ── Risk Dashboard ──────────────────────────────────────────────────────
    @GetMapping("/risk-dashboard")
    public String riskDashboard(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        try {
            model.addAttribute("alerts", riskScoreService.predictDefaulters());
        } catch (Exception e) {
            model.addAttribute("alerts", java.util.Collections.emptyList());
            model.addAttribute("riskError", "Risk calculation error: " + e.getMessage());
        }
        try {
            model.addAttribute("recentLogins", loginTrackingService.getRecentLogins());
        } catch (Exception e) {
            model.addAttribute("recentLogins", java.util.Collections.emptyList());
        }
        return "admin/risk-dashboard";
    }

    // ── Announcements ─────────────────────────────────────────────────────
    @GetMapping("/announcements")
    public String announcementsPage(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("members", userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.MEMBER).toList());
        return "admin/announcements";
    }

    // ── One-Time Data Flush (Admin only) ──────────────────────────────────
    @PostMapping("/flush-test-data")
    @org.springframework.transaction.annotation.Transactional
    public String flushTestData(Authentication auth, RedirectAttributes ra) {
        try {
            User admin = getCurrentUser(auth);
            if (admin.getRole() != User.Role.ADMIN) {
                ra.addFlashAttribute("error", "Only admin can flush data.");
                return "redirect:/admin/dashboard";
            }

            // Clear in dependency order to avoid FK violations
            // 1. Clear audit logs
            long auditCount = auditLogRepository.count();
            auditLogRepository.deleteAll();

            // 2. Clear early exit requests
            long exitCount = earlyExitRequestRepository.count();
            earlyExitRequestRepository.deleteAll();

            // 3. Clear settlements (depends on memberships — but cascade should handle)
            long settlementCount = settlementRepository.count();
            settlementRepository.deleteAll();

            // 4. Clear all chits (cascades to memberships, payments, auctions, commissions)
            long chitCount = chitRepository.count();
            List<Chit> allChits = chitRepository.findAll();
            for (Chit c : allChits) {
                chitRepository.delete(c);
            }
            chitRepository.flush();

            // 5. Clear commission ledger (in case orphans remain)
            commissionLedgerRepository.deleteAll();

            // 6. Soft-delete all member users (keep admin)
            List<User> members = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.Role.MEMBER)
                    .toList();
            long memberCount = members.size();
            for (User m : members) {
                m.setActive(false);
                userRepository.save(m);
            }

            auditService.log(admin, "FLUSH_TEST_DATA", "System", null,
                    "Flushed: " + chitCount + " chits, " + settlementCount + " settlements, "
                    + auditCount + " audit logs, " + memberCount + " members deactivated");

            ra.addFlashAttribute("success",
                    "Test data flushed! " + chitCount + " chits deleted (with all payments, memberships, auctions), "
                    + settlementCount + " settlements cleared, " + memberCount + " members deactivated.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Flush failed: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/announcements/send")
    public String sendAnnouncement(@RequestParam String title,
                                   @RequestParam String message,
                                   @RequestParam(defaultValue = "all") String target,
                                   Authentication auth, RedirectAttributes ra) {
        try {
            List<User> recipients = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.Role.MEMBER && u.isActive())
                    .toList();

            if ("all".equals(target)) {
                // Broadcast SSE notification
                notificationService.notifyAnnouncement(title, message);
                // Email all members
                recipients.forEach(u -> emailService.sendAnnouncement(u.getEmail(), u.getFullName(), title, message));
                ra.addFlashAttribute("success", "Announcement sent to " + recipients.size() + " members via push notification + email!");
            } else {
                // Target specific user by email
                User targetUser = userRepository.findByEmail(target).orElseThrow();
                notificationService.notifyAnnouncement(title, message);
                emailService.sendAnnouncement(targetUser.getEmail(), targetUser.getFullName(), title, message);
                ra.addFlashAttribute("success", "Announcement sent to " + targetUser.getFullName() + ".");
            }
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/announcements";
    }

    // ── Audit ──────────────────────────────────────────────────────────────
    @GetMapping("/audit")
    public String audit(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("auditLogs", auditLogRepository.findAll()
                .stream().sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp())).toList());
        return "admin/audit";
    }

    // ── Issue 3: Login & Security Tracking Dashboard ──────────────────────
    @GetMapping("/login-tracking")
    public String loginTracking(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));

        List<LoginHistory> allLogins = loginTrackingService.getRecentLogins();
        model.addAttribute("recentLogins", allLogins);

        // Locked accounts
        List<User> locked = userRepository.findAll().stream()
                .filter(User::isAccountLocked).toList();
        model.addAttribute("lockedAccounts", locked);

        // Failed login users
        List<User> failedUsers = userRepository.findAll().stream()
                .filter(u -> u.getConsecutiveFailedLogins() > 0)
                .sorted((a, b) -> b.getConsecutiveFailedLogins() - a.getConsecutiveFailedLogins())
                .toList();
        model.addAttribute("failedLoginUsers", failedUsers);

        // Aadhaar stats
        List<User> members = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.MEMBER && u.isActive()).toList();
        long aadhaarVerified = members.stream().filter(User::isAadhaarVerified).count();
        long aadhaarPending = members.stream().filter(u -> !u.isAadhaarVerified()).count();
        model.addAttribute("aadhaarVerified", aadhaarVerified);
        model.addAttribute("aadhaarPending", aadhaarPending);
        model.addAttribute("allMembers", members);

        // Enhanced: Login statistics (last 30 days)
        long totalLogins = allLogins.size();
        long failedLogins = allLogins.stream().filter(l -> !l.isSuccess()).count();
        long successLogins = totalLogins - failedLogins;
        int successRate = totalLogins > 0 ? (int)(successLogins * 100 / totalLogins) : 100;
        long uniqueIPs = allLogins.stream().map(LoginHistory::getIpAddress)
                .filter(ip -> ip != null).distinct().count();
        model.addAttribute("totalLogins", totalLogins);
        model.addAttribute("failedLogins", failedLogins);
        model.addAttribute("successRate", successRate);
        model.addAttribute("uniqueIPs", uniqueIPs);
        model.addAttribute("aadhaarCompliancePct",
                members.isEmpty() ? 100 : (int)(aadhaarVerified * 100 / members.size()));

        // Suspicious: users with multiple different IPs in recent logins
        java.util.Map<String, java.util.Set<String>> userIpMap = new java.util.HashMap<>();
        for (LoginHistory l : allLogins) {
            if (l.getUser() != null && l.getIpAddress() != null) {
                userIpMap.computeIfAbsent(l.getUser().getEmail(), k -> new java.util.HashSet<>())
                        .add(l.getIpAddress());
            }
        }
        List<String> suspiciousUsers = userIpMap.entrySet().stream()
                .filter(e -> e.getValue().size() >= 3)
                .map(java.util.Map.Entry::getKey).toList();
        model.addAttribute("suspiciousUsers", suspiciousUsers);
        model.addAttribute("suspiciousCount", suspiciousUsers.size());

        return "admin/login-tracking";
    }

    // ── Aadhaar Verification Toggle ──────────────────────────────────────
    @PostMapping("/members/{id}/toggle-aadhaar")
    public String toggleAadhaar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            User member = userService.findById(id);
            member.setAadhaarVerified(!member.isAadhaarVerified());
            userRepository.save(member);
            String status = member.isAadhaarVerified() ? "verified" : "unverified";
            auditService.log(getCurrentUser(auth), "AADHAAR_" + status.toUpperCase(),
                    "User", id, "Aadhaar " + status + " for: " + member.getFullName());
            ra.addFlashAttribute("success", "Aadhaar " + status + " for " + member.getFullName());
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/login-tracking";
    }

    // ── Reset Failed Login Counter ──────────────────────────────────────
    @PostMapping("/members/{id}/reset-login-counter")
    public String resetLoginCounter(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            User member = userService.findById(id);
            member.setConsecutiveFailedLogins(0);
            member.setAccountLocked(false);
            userRepository.save(member);
            auditService.log(getCurrentUser(auth), "RESET_LOGIN_COUNTER",
                    "User", id, "Reset failed login counter for: " + member.getFullName());
            ra.addFlashAttribute("success", "Login counter reset and account unlocked for " + member.getFullName());
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/login-tracking";
    }
    // ── Chit Member Management ─────────────────────────────────────────────
    //  Add a member directly to a chit (admin-initiated)
    //  Remove an active/pending member with reason + email notifications

    /**
     * POST /admin/chits/{chitId}/members/add
     * Admin adds an existing user to a chit by email.
     */
    @PostMapping("/chits/{chitId}/members/add")
    public String addMemberToChit(@PathVariable Long chitId,
                                  @RequestParam String memberEmail,
                                  Authentication auth,
                                  RedirectAttributes ra) {
        try {
            Chit chit = chitService.findById(chitId);
            User member = userRepository.findByEmail(memberEmail)
                    .orElseThrow(() -> new RuntimeException("No user found with email: " + memberEmail));

            if (membershipRepository.findActiveOrPendingByChitAndUser(chit, member).isPresent()) {
                ra.addFlashAttribute("error", "Member " + member.getFullName() + " already has an active or pending membership in this chit.");
                return "redirect:/admin/chits/" + chitId;
            }

            ChitMembership membership = new ChitMembership();
            membership.setChit(chit);
            membership.setUser(member);
            membership.setStatus(ChitMembership.MembershipStatus.PENDING);
            membershipRepository.save(membership);

            // Notify member — clear actionable message
            notificationService.notifyUserUpdated(member.getEmail(),
                    "You've been invited to join '" + chit.getName() + "'. Open your dashboard to review the agreement and accept.");
            emailService.sendUserUpdated(member.getEmail(), member.getFullName(),
                    "You have been invited to chit '" + chit.getName() + "' by the admin. "
                            + "Please log in to your dashboard, review the agreement, and accept to proceed. "
                            + "Monthly: ₹" + chit.getMonthlyAmount() + " | Duration: " + chit.getDurationMonths() + " months.");

            ra.addFlashAttribute("success",
                    member.getFullName() + " added to chit as PENDING. Notification sent.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Add member failed: " + e.getMessage());
        }
        return "redirect:/admin/chits/" + chitId;
    }

    /**
     * POST /admin/chits/{chitId}/members/{membershipId}/remove
     * Admin removes a member from a chit with a mandatory reason.
     * Sends email to both admin and the removed member.
     */
    @PostMapping("/chits/{chitId}/members/{membershipId}/remove")
    public String removeMemberFromChit(@PathVariable Long chitId,
                                       @PathVariable Long membershipId,
                                       @RequestParam String reason,
                                       Authentication auth,
                                       RedirectAttributes ra) {
        try {
            Chit chit           = chitService.findById(chitId);
            ChitMembership m    = membershipRepository.findById(membershipId)
                    .orElseThrow(() -> new RuntimeException("Membership not found: " + membershipId));
            User admin          = getCurrentUser(auth);

            m.setStatus(ChitMembership.MembershipStatus.EXITED);
            m.setRejectionReason(reason);
            membershipRepository.save(m);

            // Email to removed member
            emailService.sendMemberRemovedFromChit(
                    m.getUser().getEmail(), m.getUser().getFullName(),
                    chit.getName(), reason);

            // Email to admin (confirmation)
            emailService.sendMemberRemovedFromChit(
                    admin.getEmail(), admin.getFullName(),
                    "[Admin Copy] Removed " + m.getUser().getFullName() + " from " + chit.getName(),
                    reason);

            // Push notification to user
            notificationService.notifyUserUpdated(m.getUser().getEmail(),
                    "You have been removed from chit '" + chit.getName() + "'. Reason: " + reason);

            ra.addFlashAttribute("success",
                    m.getUser().getFullName() + " removed from chit. Notifications sent to member and admin.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Remove member failed: " + e.getMessage());
        }
        return "redirect:/admin/chits/" + chitId;
    }

    // ── Agreement Download (Admin can download any agreement anytime) ─────
    @GetMapping("/agreements/{membershipId}/download")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadAgreement(
            @PathVariable Long membershipId, Authentication auth) {
        ChitMembership m = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Membership not found: " + membershipId));

        String pdfPath = m.getAgreementPdfPath();
        if (pdfPath == null || pdfPath.isBlank()) {
            try {
                pdfPath = chitAgreementService.generateAndDistributeAgreementPdf(m, getCurrentUser(auth));
            } catch (Exception regen) {
                throw new RuntimeException("No agreement PDF and regeneration failed: " + regen.getMessage());
            }
        }

        java.nio.file.Path file = java.nio.file.Paths.get(pdfPath);
        if (!java.nio.file.Files.exists(file)) {
            // File missing from disk — regenerate
            try {
                pdfPath = chitAgreementService.generateAndDistributeAgreementPdf(m, getCurrentUser(auth));
                file = java.nio.file.Paths.get(pdfPath);
            } catch (Exception regen) {
                throw new RuntimeException("Agreement PDF not found and regeneration failed.");
            }
        }

        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);
        String filename = "Agreement_" + (m.getAgreementNumber() != null ? m.getAgreementNumber() : membershipId) + ".pdf";

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(resource);
    }

    // ── Chit History (Closed / Archived Chits) ─────────────────────────────

    /**
     * GET /admin/chit-history
     * Lists all archived chit entries from the chit_history table.
     */
    @GetMapping("/chit-history")
    public String chitHistory(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        List<ChitHistory> histories = chitHistoryService.findAll();
        model.addAttribute("histories", histories);
        // Pre-compute filtered counts — SpEL .?[] inside Thymeleaf #lists can fail at runtime
        model.addAttribute("deletedCount", histories.stream()
                .filter(h -> "DELETED".equals(h.getFinalStatus())).count());
        model.addAttribute("completedCount", histories.stream()
                .filter(h -> "COMPLETED".equals(h.getFinalStatus())).count());
        model.addAttribute("cancelledCount", histories.stream()
                .filter(h -> "CANCELLED".equals(h.getFinalStatus())).count());
        return "admin/chit-history";
    }

    /**
     * GET /admin/chit-history/{id}/pdf
     * Download the analysis PDF for a closed chit.
     */
    @GetMapping("/chit-history/{id}/pdf")
    public org.springframework.http.ResponseEntity<byte[]> downloadChitHistoryPdf(
            @PathVariable Long id) {
        try {
            byte[] pdf = chitHistoryService.getPdfBytesForHistory(id);
            ChitHistory history = chitHistoryService.findById(id)
                    .orElseThrow(() -> new RuntimeException("History not found: " + id));
            String filename = "ChitAnalysis_" + history.getChitName().replaceAll("[^a-zA-Z0-9_-]", "_") + ".pdf";
            return org.springframework.http.ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /admin/chit-history/{id}/json
     * View the raw JSON snapshot for a closed chit (admin only).
     */
    @GetMapping("/chit-history/{id}/json")
    public org.springframework.http.ResponseEntity<String> viewChitHistoryJson(@PathVariable Long id) {
        return chitHistoryService.findById(id)
                .map(h -> org.springframework.http.ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(h.getCompleteDataJson()))
                .orElse(org.springframework.http.ResponseEntity.notFound().build());
    }

    // ── Modified deleteChit — archive before delete ─────────────────────────

    /**
     * POST /admin/chits/{id}/delete-with-archive
     *
     * Archives complete chit data to chit_history (with PDF),
     * emails admin the analysis PDF, THEN deletes the chit.
     *
     * Replace or supplement the existing /delete endpoint with this one.
     */
    @PostMapping("/chits/{id}/delete-with-archive")
    @org.springframework.transaction.annotation.Transactional
    public String deleteChitWithArchive(@PathVariable Long id,
                                        @RequestParam(required = false, defaultValue = "") String closingReason,
                                        Authentication auth,
                                        RedirectAttributes ra) {
        try {
            Chit chit   = chitService.findById(id);
            User admin  = getCurrentUser(auth);

            if (chit.getStatus() == Chit.ChitStatus.ACTIVE) {
                ra.addFlashAttribute("error", "Cannot delete an active chit — cancel it first.");
                return "redirect:/admin/chits/" + id;
            }

            // 1. Archive to chit_history + generate PDF
            String reason = closingReason.isBlank() ? "Deleted by admin" : closingReason;
            ChitHistory history = chitHistoryService.archiveChit(chit, "DELETED", reason, admin);

            // 2. Email the PDF to admin
            if (history.getAnalysisPdfPath() != null) {
                String subject  = "Chit Deleted & Archived — " + chit.getName();
                String body     = "<p>Chit <strong>" + chit.getName() + "</strong> has been deleted and archived.</p>"
                        + "<p><strong>Reason:</strong> " + reason + "</p>"
                        + "<p>Complete analysis PDF is attached.</p>";
                emailService.sendHtmlEmailWithAttachment(
                        admin.getEmail(), subject, body,
                        history.getAnalysisPdfPath(),
                        "ChitAnalysis_" + chit.getName().replaceAll("[^a-zA-Z0-9_-]", "_") + ".pdf");
            }

            // 3. Notify all active members the chit is being closed
            chitService.getMembershipsForChit(chit).stream()
                    .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE
                            || m.getStatus() == ChitMembership.MembershipStatus.PENDING)
                    .forEach(m -> {
                        notificationService.notifyUserUpdated(m.getUser().getEmail(),
                                "Chit '" + chit.getName() + "' has been permanently closed. Reason: " + reason);
                        emailService.sendChitUpdated(m.getUser().getEmail(), m.getUser().getFullName(),
                                chit.getName(), "Chit permanently closed/deleted. Reason: " + reason);
                    });

            // 4. Hard-delete — entity-based for proper cascade
            chitRepository.delete(chit);
            chitRepository.flush();
            ra.addFlashAttribute("success",
                    "Chit deleted. Full analysis PDF emailed to admin and saved to Chit History.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }
        return "redirect:/admin/chits";
    }

}
