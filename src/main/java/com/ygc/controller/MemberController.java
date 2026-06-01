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

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final UserRepository userRepository;
    private final ChitService chitService;
    private final PaymentService paymentService;
    private final AuctionService auctionService;
    private final SettlementService settlementService;
    private final ChitMembershipRepository membershipRepository;
    private final AuctionRepository auctionRepository;
    private final PdfCertificateService pdfCertificateService;

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = getCurrentUser(auth);
        List<ChitMembership> memberships = chitService.getMembershipsForUser(user);
        long activeCount = memberships.stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE).count();
        model.addAttribute("user", user);
        model.addAttribute("memberships", memberships);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("availableChits", chitService.getAvailableChits());
        return "member/dashboard";
    }

    @GetMapping("/chits")
    public String availableChits(Authentication auth, Model model) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("chits", chitService.getAvailableChits());
        return "member/chits";
    }

    @PostMapping("/chits/{id}/join")
    public String joinChit(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            chitService.requestJoin(id, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Join request submitted! Awaiting admin approval.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
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
        model.addAttribute("user", user);
        model.addAttribute("membership", membership);
        model.addAttribute("payments", payments);
        model.addAttribute("totalPaid", paymentService.getTotalPaid(membership));
        model.addAttribute("auctions", auctions);
        return "member/membership-detail";
    }

    @PostMapping("/payments/submit")
    public String submitPayment(@RequestParam Long membershipId,
                                @RequestParam Integer monthNumber,
                                @RequestParam(required = false) MultipartFile screenshot,
                                Authentication auth, RedirectAttributes ra) {
        try {
            paymentService.submitPayment(membershipId, screenshot, monthNumber, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Payment submitted for verification!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/member/memberships/" + membershipId;
    }

    @PostMapping("/auctions/{id}/bid")
    public String placeBid(@PathVariable Long id,
                           @RequestParam BigDecimal bidAmount,
                           Authentication auth, RedirectAttributes ra) {
        try {
            auctionService.placeBid(id, bidAmount, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Bid placed successfully! Best of luck.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/member/dashboard";
    }

    @PostMapping("/memberships/{id}/exit")
    public String requestEarlyExit(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            settlementService.requestEarlyExit(id, getCurrentUser(auth));
            ra.addFlashAttribute("success", "Early exit request submitted. Awaiting admin approval.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/member/memberships/" + id;
    }

    @PostMapping("/memberships/{id}/accept-terms")
    public String acceptTerms(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            ChitMembership membership = membershipRepository.findById(id).orElseThrow();
            User user = getCurrentUser(auth);
            if (!membership.getUser().getId().equals(user.getId())) {
                ra.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/member/dashboard";
            }
            membership.setTermsAccepted(true);
            String certPath = pdfCertificateService.generateMemberCertificate(membership);
            membership.setCertificatePath(certPath);
            membershipRepository.save(membership);
            ra.addFlashAttribute("success", "Terms accepted! Your digital certificate has been generated.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not accept terms: " + e.getMessage());
        }
        return "redirect:/member/memberships/" + id;
    }
}
