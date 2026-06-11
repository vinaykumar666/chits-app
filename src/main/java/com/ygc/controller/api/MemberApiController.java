package com.ygc.controller.api;

import com.ygc.dto.*;
import com.ygc.model.*;
import com.ygc.repository.ChitMembershipRepository;
import com.ygc.repository.SettlementRepository;
import com.ygc.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberApiController {

    private final ApiSupport apiSupport;
    private final DtoMapper dtoMapper;
    private final ChitService chitService;
    private final PaymentService paymentService;
    private final AuctionService auctionService;
    private final SettlementService settlementService;
    private final SettlementRepository settlementRepository;
    private final ChitMembershipRepository membershipRepository;
    private final BidCalculationService bidCalculationService;

    private Map<String, String> buildMyChitStatusMap(User user) {
        Map<String, String> myChitStatus = new HashMap<>();
        for (ChitMembership m : chitService.getMembershipsForUser(user)) {
            myChitStatus.put(String.valueOf(m.getChit().getId()), m.getStatus().name());
        }
        return myChitStatus;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(Authentication auth) {
        User user = apiSupport.currentUser(auth);
        List<ChitMembership> memberships = chitService.getMembershipsForUser(user);
        long activeCount = memberships.stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE).count();
        List<Settlement> mySettlements = settlementRepository.findAll().stream()
                .filter(s -> s.getMembership().getUser().getId().equals(user.getId()))
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("user", dtoMapper.toUserDto(user));
        body.put("memberships", dtoMapper.toMembershipDtos(memberships));
        body.put("activeCount", activeCount);
        body.put("availableChits", dtoMapper.toChitDtos(chitService.getAvailableChits()));
        body.put("myChitStatus", buildMyChitStatusMap(user));
        body.put("openAuctions", dtoMapper.toAuctionDtos(auctionService.getOpenAuctions()));
        body.put("mySettlements", dtoMapper.toSettlementDtos(mySettlements));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/chits")
    public ResponseEntity<Map<String, Object>> availableChits(Authentication auth) {
        User user = apiSupport.currentUser(auth);
        return ResponseEntity.ok(Map.of(
                "chits", dtoMapper.toChitDtos(chitService.getAvailableChits()),
                "myChitStatus", buildMyChitStatusMap(user)));
    }

    @GetMapping("/chits/{id}")
    public ResponseEntity<ChitDto> chitDetail(@PathVariable Long id) {
        return ResponseEntity.ok(dtoMapper.toChitDto(chitService.findById(id)));
    }

    @PostMapping("/chits/{id}/join")
    public ResponseEntity<Map<String, String>> joinChit(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        boolean agreementRead = Boolean.TRUE.equals(body.get("agreementRead"));
        boolean termsAccepted = Boolean.TRUE.equals(body.get("termsAccepted"));
        boolean infoProcessingAuthorized = Boolean.TRUE.equals(body.get("infoProcessingAuthorized"));
        String joinReason = body.get("joinReason") != null ? body.get("joinReason").toString() : null;
        chitService.requestJoin(id, apiSupport.currentUser(auth),
                agreementRead, termsAccepted, infoProcessingAuthorized, joinReason);
        return ResponseEntity.ok(Map.of("message",
                "Join request submitted! Awaiting admin approval."));
    }

    @GetMapping("/memberships/{id}")
    public ResponseEntity<Map<String, Object>> membershipDetail(@PathVariable Long id, Authentication auth) {
        ChitMembership membership = membershipRepository.findById(id).orElseThrow();
        User user = apiSupport.currentUser(auth);
        if (!membership.getUser().getId().equals(user.getId())) {
            throw new com.ygc.exception.AccessDeniedException("Unauthorized");
        }

        List<Payment> payments = paymentService.getPaymentsForMembership(membership);
        List<Auction> auctions = auctionService.getAuctionsByChit(membership.getChit());
        Chit chit = membership.getChit();
        int currentMonthNumber = Math.max(1,
                (int) java.time.temporal.ChronoUnit.MONTHS.between(
                        chit.getStartDate(), java.time.LocalDate.now()) + 1);
        currentMonthNumber = Math.min(currentMonthNumber, chit.getDurationMonths());

        Map<String, Object> bidRecommendations = null;
        boolean hasOpenAuction = auctions.stream()
                .anyMatch(a -> a.getStatus() == Auction.AuctionStatus.OPEN);
        if (hasOpenAuction && !membership.isHasWonAuction()) {
            bidRecommendations = bidCalculationService.calculateBidRecommendations(chit, currentMonthNumber);
        }

        List<Settlement> mySettlements = settlementRepository.findAll().stream()
                .filter(s -> s.getMembership().getId().equals(membership.getId()))
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("membership", dtoMapper.toMembershipDto(membership));
        body.put("payments", dtoMapper.toPaymentDtos(payments));
        body.put("totalPaid", paymentService.getTotalPaid(membership));
        body.put("auctions", dtoMapper.toAuctionDtos(auctions));
        body.put("bidRecommendations", bidRecommendations);
        body.put("hasOpenAuction", hasOpenAuction);
        body.put("mySettlements", dtoMapper.toSettlementDtos(mySettlements));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/chits/{chitId}/bid-calculator")
    public ResponseEntity<Map<String, Object>> bidCalculator(
            @PathVariable Long chitId,
            @RequestParam(required = false) BigDecimal bidAmount,
            @RequestParam(defaultValue = "1") Integer monthNumber) {
        Chit chit = chitService.findById(chitId);
        Map<String, Object> recs = bidCalculationService.calculateBidRecommendations(chit, monthNumber);
        if (bidAmount != null) {
            Map<String, BigDecimal> calc = bidCalculationService.calculateForBidAmount(chit, bidAmount);
            recs.put("inputBidCommission", calc.get("commission"));
            recs.put("inputBidPayout", calc.get("payout"));
        }
        return ResponseEntity.ok(recs);
    }

    @PostMapping("/memberships/{id}/accept-terms")
    public ResponseEntity<Map<String, String>> acceptTerms(@PathVariable Long id, Authentication auth) {
        ChitMembership membership = membershipRepository.findById(id).orElseThrow();
        User user = apiSupport.currentUser(auth);
        if (!membership.getUser().getId().equals(user.getId())) {
            throw new com.ygc.exception.AccessDeniedException("Unauthorized");
        }
        membershipRepository.save(membership);
        return ResponseEntity.ok(Map.of("message", "Terms accepted."));
    }

    @PostMapping(value = "/payments/submit", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> submitPayment(
            @RequestParam Long membershipId,
            @RequestParam Integer monthNumber,
            @RequestParam(required = false) MultipartFile screenshot,
            Authentication auth) throws Exception {
        paymentService.submitPayment(membershipId, screenshot, monthNumber, apiSupport.currentUser(auth));
        return ResponseEntity.ok(Map.of("message", "Payment submitted!"));
    }

    @PostMapping("/auctions/{id}/bid")
    public ResponseEntity<Map<String, String>> placeBid(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> body,
            Authentication auth) {
        auctionService.placeBid(id, body.get("bidAmount"), apiSupport.currentUser(auth));
        return ResponseEntity.ok(Map.of("message", "Bid placed successfully!"));
    }

    @PostMapping("/memberships/{id}/exit")
    public ResponseEntity<Map<String, String>> requestEarlyExit(@PathVariable Long id, Authentication auth) {
        settlementService.requestEarlyExit(id, apiSupport.currentUser(auth));
        return ResponseEntity.ok(Map.of("message", "Early exit request submitted."));
    }
}
