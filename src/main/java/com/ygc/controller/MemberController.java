package com.ygc.controller;

import com.ygc.model.*;
import com.ygc.repository.*;
import com.ygc.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final UserRepository userRepository;
    private final ChitService chitService;
    private final PaymentService paymentService;
    private final AuctionService auctionService;
    private final SettlementService settlementService;
    private final SettlementRepository settlementRepository;
    private final ChitMembershipRepository membershipRepository;
    private final AuctionRepository auctionRepository;
    private final PdfCertificateService pdfCertificateService;
    private final BidCalculationService bidCalculationService;
    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = getCurrentUser(auth);
        List<ChitMembership> memberships = chitService.getMembershipsForUser(user);
        long activeCount = memberships.stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE).count();
        // Fetch early exit / maturity settlements for this user
        List<Settlement> mySettlements = settlementRepository.findAll().stream()
                .filter(s -> s.getMembership().getUser().getId().equals(user.getId()))
                .toList();
        model.addAttribute("user", user);
        model.addAttribute("memberships", memberships);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("availableChits", chitService.getAvailableChits());
        model.addAttribute("openAuctions", auctionService.getOpenAuctions());
        model.addAttribute("mySettlements", mySettlements);
        return "member/dashboard";
    }

    @GetMapping("/chits")
    public String availableChits(Authentication auth, Model model) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("chits", chitService.getAvailableChits());
        return "member/chits";
    }

    /**
     * GET: Show chit join agreement page with all 3 mandatory checkboxes.
     */
    @GetMapping("/chits/{id}/join")
    public String joinChitAgreementPage(@PathVariable Long id, Authentication auth, Model model) {
        Chit chit = chitService.findById(id);
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("chit", chit);
        return "member/chit-join-agreement";
    }

    /**
     * POST: Submit join request. Validates all 3 agreement checkboxes.
     */
    @PostMapping("/chits/{id}/join")
    public String joinChit(@PathVariable Long id,
                           @RequestParam(defaultValue = "false") boolean agreementRead,
                           @RequestParam(defaultValue = "false") boolean termsAccepted,
                           @RequestParam(defaultValue = "false") boolean infoProcessingAuthorized,
                           Authentication auth,
                           RedirectAttributes ra) {
        try {
            chitService.requestJoin(id, getCurrentUser(auth),
                    agreementRead, termsAccepted, infoProcessingAuthorized);
            ra.addFlashAttribute("success",
                    "Join request submitted! Awaiting admin approval. You'll receive a signed agreement by email upon approval.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/member/chits/" + id + "/join";
        }
        return "redirect:/member/chits";
    }

    @GetMapping("/memberships/{id}")
    public String membershipDetail(@PathVariable Long id, Authentication auth, Model model) {
        ChitMembership membership = membershipRepository.findById(id).orElseThrow();
        User user = getCurrentUser(auth);
        if (!membership.getUser().getId().equals(user.getId())) {
            return "redirect:/member/dashboard";
        }
        List<Payment> payments = paymentService.getPaymentsForMembership(membership);
        List<Auction> auctions = auctionService.getAuctionsByChit(membership.getChit());

        // Current month number based on chit start date
        Chit chit = membership.getChit();
        int currentMonthNumber = Math.max(1,
                (int) java.time.temporal.ChronoUnit.MONTHS.between(
                        chit.getStartDate(), java.time.LocalDate.now()) + 1);
        currentMonthNumber = Math.min(currentMonthNumber, chit.getDurationMonths());

        // Bid recommendations for open auctions in this chit
        Map<String, Object> bidRecommendations = null;
        boolean hasOpenAuction = auctions.stream()
                .anyMatch(a -> a.getStatus() == Auction.AuctionStatus.OPEN);
        if (hasOpenAuction && !membership.isHasWonAuction()) {
            bidRecommendations = bidCalculationService.calculateBidRecommendations(chit, currentMonthNumber);
        }

        model.addAttribute("user", user);
        model.addAttribute("membership", membership);
        model.addAttribute("payments", payments);
        model.addAttribute("totalPaid", paymentService.getTotalPaid(membership));
        model.addAttribute("auctions", auctions);
        model.addAttribute("bidRecommendations", bidRecommendations);
        model.addAttribute("hasOpenAuction", hasOpenAuction);
        // Early-exit settlement tracking: find any settlement for this membership
        List<Settlement> mySettlements = settlementRepository.findAll().stream()
                .filter(s -> s.getMembership().getId().equals(membership.getId()))
                .toList();
        model.addAttribute("mySettlements", mySettlements);
        return "member/membership-detail";
    }

    /**
     * AJAX endpoint: get bid calculations for a given bid amount.
     */
    @GetMapping("/chits/{chitId}/bid-calculator")
    @ResponseBody
    public Map<String, Object> bidCalculator(@PathVariable Long chitId,
                                              @RequestParam(required = false) BigDecimal bidAmount,
                                              @RequestParam(defaultValue = "1") Integer monthNumber) {
        Chit chit = chitService.findById(chitId);
        Map<String, Object> recs = bidCalculationService.calculateBidRecommendations(chit, monthNumber);
        if (bidAmount != null) {
            Map<String, BigDecimal> calc = bidCalculationService.calculateForBidAmount(chit, bidAmount);
            recs.put("inputBidCommission", calc.get("commission"));
            recs.put("inputBidPayout", calc.get("payout"));
        }
        return recs;
    }

    @PostMapping("/memberships/{id}/accept-terms")
    public String acceptTerms(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            ChitMembership membership = membershipRepository.findById(id).orElseThrow();
            User user = getCurrentUser(auth);
            if (!membership.getUser().getId().equals(user.getId())) {
                ra.addFlashAttribute("error", "Unauthorized action.");
                return "redirect:/member/dashboard";
            }
            membershipRepository.save(membership);
            ra.addFlashAttribute("success", "Terms accepted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/member/memberships/" + id;
    }

    @PostMapping("/payments/submit")
    public String submitPayment(@RequestParam Long membershipId,
                                @RequestParam Integer monthNumber,
                                @RequestParam(required = false) MultipartFile screenshot,
                                Authentication auth, RedirectAttributes ra) {
        try {
            paymentService.submitPayment(membershipId, screenshot, monthNumber, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Payment submitted!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/member/memberships/" + membershipId;
    }

    @PostMapping("/auctions/{id}/bid")
    public String placeBid(@PathVariable Long id, @RequestParam BigDecimal bidAmount,
                           Authentication auth, RedirectAttributes ra) {
        try {
            auctionService.placeBid(id, bidAmount, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Bid placed successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/member/dashboard";
    }

    @PostMapping("/memberships/{id}/exit")
    public String requestEarlyExit(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            settlementService.requestEarlyExit(id, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Early exit request submitted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/member/memberships/" + id;
    }
}
