package com.ygc.controller;

import com.ygc.model.*;
import com.ygc.repository.*;
import com.ygc.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;

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

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("totalChits", chitRepository.count());
        model.addAttribute("totalMembers", userRepository.findAll().stream().filter(u -> u.getRole() == User.Role.MEMBER).count());
        model.addAttribute("pendingPayments", paymentService.getPendingPayments().size());
        model.addAttribute("pendingSettlements", settlementService.getPendingSettlements().size());
        model.addAttribute("openAuctions", auctionService.getOpenAuctions().size());
        model.addAttribute("recentAudits", auditLogRepository.findAll()
                .stream().sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(10).toList());
        return "admin/dashboard";
    }

    // --- Chit Management ---
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
    public String createChit(@RequestParam String name,
                             @RequestParam String description,
                             @RequestParam BigDecimal monthlyAmount,
                             @RequestParam Integer totalMembers,
                             @RequestParam Integer durationMonths,
                             @RequestParam BigDecimal adminCommissionPercentage,
                             @RequestParam(required = false) BigDecimal minBidAmount,
                             @RequestParam(required = false) BigDecimal maxBidAmount,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                             Authentication auth,
                             RedirectAttributes ra) {
        try {
            Chit chit = new Chit();
            chit.setName(name);
            chit.setDescription(description);
            chit.setMonthlyAmount(monthlyAmount);
            chit.setTotalMembers(totalMembers);
            chit.setDurationMonths(durationMonths);
            chit.setAdminCommissionPercentage(adminCommissionPercentage);
            chit.setMinBidAmount(minBidAmount);
            chit.setMaxBidAmount(maxBidAmount);
            chit.setStartDate(startDate);
            chitService.createChit(chit, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Chit created successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/chits";
    }

    @GetMapping("/chits/{id}")
    public String chitDetail(@PathVariable Long id, Model model, Authentication auth) {
        Chit chit = chitService.findById(id);
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("chit", chit);
        model.addAttribute("memberships", chitService.getMembershipsForChit(chit));
        model.addAttribute("auctions", auctionService.getAuctionsByChit(chit));
        return "admin/chit-detail";
    }

    @PostMapping("/memberships/{id}/approve")
    public String approveMembership(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            ChitMembership membership = membershipRepository.findById(id).orElseThrow();
            Long chitId = membership.getChit().getId();
            chitService.approveMembership(id, getCurrentUser(auth));
            ra.addFlashAttribute("success",
                "Membership approved! Signed agreement PDF generated and emailed to member and admin.");
            return "redirect:/admin/chits/" + chitId;
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/chits";
    }

    @PostMapping("/memberships/{id}/reject")
    public String rejectMembership(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            ChitMembership membership = membershipRepository.findById(id).orElseThrow();
            Long chitId = membership.getChit().getId();
            membership.setStatus(ChitMembership.MembershipStatus.EXITED);
            membershipRepository.save(membership);
            ra.addFlashAttribute("success", "Membership rejected");
            return "redirect:/admin/chits/" + chitId;
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/chits";
    }

    // --- Payment Verification ---
    @GetMapping("/payments")
    public String pendingPayments(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("payments", paymentService.getPendingPayments());
        model.addAttribute("allPayments", paymentService.getAllPayments());
        return "admin/payments";
    }

    @PostMapping("/payments/{id}/verify")
    public String verifyPayment(@PathVariable Long id,
                                @RequestParam boolean approved,
                                @RequestParam(required = false) String remarks,
                                Authentication auth, RedirectAttributes ra) {
        try {
            paymentService.verifyPayment(id, approved, remarks != null ? remarks : (approved ? "Approved" : "Rejected"), getCurrentUser(auth));
            ra.addFlashAttribute("success", "Payment " + (approved ? "approved" : "rejected"));
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    // --- Auction Management ---
    @GetMapping("/auctions")
    public String auctions(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("chits", chitService.getAllChits());
        model.addAttribute("openAuctions", auctionService.getOpenAuctions());
        model.addAttribute("allAuctions", auctionService.getAllAuctions());

        // Pre-compute bid recommendations for each open auction's chit
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
    public String createAuction(@RequestParam Long chitId,
                                @RequestParam Integer monthNumber,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate auctionDate,
                                Authentication auth, RedirectAttributes ra) {
        try {
            auctionService.createAuction(chitId, monthNumber, auctionDate, chitRepository, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Auction announced! Members have been notified.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/auctions";
    }

    @PostMapping("/auctions/{id}/open")
    public String openAuction(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            auctionService.openAuction(id, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Auction is now open for bidding");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/auctions";
    }

    @PostMapping("/auctions/{id}/close")
    public String closeAuction(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            auctionService.closeAuction(id, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Auction closed. Winner selected!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/auctions";
    }

    @PostMapping("/auctions/{id}/release-payout")
    public String releasePayout(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            auctionService.releasePayout(id, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Payout released successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/auctions";
    }

    // --- Settlement Management ---
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
            settlementService.approveSettlement(id, approved, remarks != null ? remarks : (approved ? "Approved" : "Rejected"), getCurrentUser(auth));
            ra.addFlashAttribute("success", "Settlement " + (approved ? "approved" : "rejected"));
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/settlements";
    }

    // --- Payment Approve Page (separate from verify flow) ---
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
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/payments";
    }


    // --- Commission Reports ---
    @GetMapping("/reports/commission")
    public String commissionReport(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("ledger", commissionLedgerRepository.findAll());
        model.addAttribute("chits", chitService.getAllChits());
        BigDecimal totalCommission = commissionLedgerRepository.findAll().stream()
                .map(CommissionLedger::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalCommission", totalCommission);
        return "admin/commission-report";
    }

    // --- Members ---
    @GetMapping("/members")
    public String members(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("members", userRepository.findAll());
        return "admin/members";
    }

    // --- Audit ---
    @GetMapping("/audit")
    public String audit(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("auditLogs", auditLogRepository.findAll()
                .stream().sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp())).toList());
        return "admin/audit";
    }
}
// This will be appended - but let's check the existing class structure first
