
            User user = getCurrentUser(auth);
            Chit chit = chitService.findById(id);

            ChitMembership membership = chitService.requestJoin(id, user);

            // Send agreement emails
            ChitAgreementService agreementService = new ChitAgreementService(emailService, new LoggingUtil());
            agreementService.sendAgreement(user.getEmail(), user.getFullName(), chit.getName(), "admin@ygcinternal.com");

            redirectAttributes.addFlashAttribute("success", "Request submitted! Agreement sent to email.");
            return "redirect:/member/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/member/chits";
        }
package com.ygc.service;

import com.ygc.model.Payment;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService {
    private final LoggingUtil loggingUtil;

    public byte[] exportPaymentsToCSV(List<Payment> payments) {
        loggingUtil.transactionStart("exportPaymentsToCSV", "ReportExportService");
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("ID,Member,Chit,Amount,Late Fine,Total,Month,Status,Date\n");

            for (Payment p : payments) {
                csv.append(p.getId()).append(",")
                   .append(p.getMembership().getUser().getFullName()).append(",")
                   .append(p.getMembership().getChit().getName()).append(",")
                   .append(p.getAmount()).append(",")
                   .append(p.getLateFine()).append(",")
                   .append(p.getTotalAmount()).append(",")
                   .append(p.getMonthNumber()).append(",")
                   .append(p.getStatus()).append(",")
                   .append(p.getPaidDate()).append("\n");
            }

            loggingUtil.transactionComplete("exportPaymentsToCSV", "ReportExportService");
            return csv.toString().getBytes();
        } catch (Exception e) {
            loggingUtil.error("Error exporting payments", "ReportExportService", e);
            throw new RuntimeException(e);
        }
    }

    public String getReportHTML(String title, List<String[]> rows, String[] headers) {
        loggingUtil.debug("Generating report HTML: " + title, "ReportExportService");

    @GetMapping("/chits/{id}/join-agreement")
    public String joinAgreement(@PathVariable Long id, Authentication auth, Model model) {
        try {
            User user = getCurrentUser(auth);
            Chit chit = chitService.findById(id);

            model.addAttribute("user", user);
            model.addAttribute("chit", chit);

            ChitAgreementService agreementService = new ChitAgreementService(emailService, new LoggingUtil());
            model.addAttribute("agreementHTML", agreementService.getAgreementHTML(chit.getName(), user.getFullName()));

            return "member/chit-join-agreement";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading agreement: " + e.getMessage());
            return "redirect:/member/chits";
        }
    }

    @PostMapping("/chits/{id}/join")
    public String joinChit(@PathVariable Long id,
                          @RequestParam boolean acceptTerms,
                          Authentication auth,
                          RedirectAttributes redirectAttributes) {
        try {
            if (!acceptTerms) {
                redirectAttributes.addFlashAttribute("error", "You must accept the agreement");
                return "redirect:/member/chits/" + id + "/join-agreement";
            }
            .append("td { padding: 10px; border-bottom: 1px solid #ddd; }")
            .append("tr:hover { background: #f5f5f5; }")
            .append("</style></head><body>")
            .append("<h1>").append(title).append("</h1>")
            .append("<table>")
            .append("<thead><tr>");

        for (String header : headers) {
            html.append("<th>").append(header).append("</th>");
        }
        html.append("</tr></thead><tbody>");

        for (String[] row : rows) {
            html.append("<tr>");
            for (String cell : row) {
                html.append("<td>").append(cell).append("</td>");
            }
            html.append("</tr>");
        }

        html.append("</tbody></table>")
            .append("<p style='text-align: center; margin-top: 40px; color: #999;'>Generated on ")
            .append(java.time.LocalDateTime.now())
            .append("</p>")
            .append("</body></html>");

        return html.toString();
    }
}
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
