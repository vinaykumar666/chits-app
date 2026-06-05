package com.ygc.service;

import com.ygc.model.Auction;
import com.ygc.model.Bid;
import com.ygc.model.Chit;
import com.ygc.model.ChitMembership;
import com.ygc.repository.AuctionRepository;
import com.ygc.repository.BidRepository;
import com.ygc.repository.ChitMembershipRepository;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Game-Theory–Based Intelligent Bid Engine.
 *
 * Core mechanics of chit fund auctions:
 *  - LOWEST bidder wins → they sacrifice maximum discount for immediate liquidity.
 *  - Non-winners BENEFIT: the discount (totalChitValue - winningBid) is distributed
 *    as dividends, so patience = profit.
 *  - Nash equilibrium: rational bidders balance urgency vs. dividend accumulation.
 *
 * This engine calculates:
 *  1. Smart bid range using urgency decay, competition density, and historical trends
 *  2. Member profit projections (what they earn by NOT winning)
 *  3. Win probability estimation per bid amount
 *  4. Dividend forecast for remaining months
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BidCalculationService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ChitMembershipRepository membershipRepository;
    private final LoggingUtil loggingUtil;

    // ── Main recommendation engine ──────────────────────────────────────────

    public Map<String, Object> calculateBidRecommendations(Chit chit, Integer currentMonthNumber) {
        loggingUtil.debug("Calculating bid recommendations for chit:" + chit.getId()
                + " month:" + currentMonthNumber, "BidCalc");

        Map<String, Object> result = new LinkedHashMap<>();

        BigDecimal totalChitValue = chit.getTotalChitValue() != null
                ? chit.getTotalChitValue()
                : chit.getMonthlyAmount().multiply(BigDecimal.valueOf(chit.getTotalMembers()));

        BigDecimal commissionPct = chit.getAdminCommissionPercentage();
        int totalMonths = chit.getDurationMonths();
        int monthsRemaining = Math.max(1, totalMonths - (currentMonthNumber - 1));

        // ── Historical analysis ─────────────────────────────────────────────
        List<Auction> pastAuctions = auctionRepository.findByChit(chit);
        List<BigDecimal> winningBids = pastAuctions.stream()
                .filter(a -> a.getWinningBidAmount() != null)
                .sorted(Comparator.comparingInt(a -> a.getMonthNumber() != null ? a.getMonthNumber() : 0))
                .map(Auction::getWinningBidAmount)
                .toList();

        BigDecimal historicalAvg = computeAvg(winningBids);
        BigDecimal historicalMin = winningBids.isEmpty() ? null
                : winningBids.stream().min(Comparator.naturalOrder()).orElse(null);
        BigDecimal trend = computeTrend(winningBids); // +ve = bids rising, -ve = falling

        // ── Absolute limits ─────────────────────────────────────────────────
        // RBI guideline: minimum 70% of chit value
        BigDecimal regulatoryFloor = totalChitValue.multiply(bd(0.70)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal absoluteMin = chit.getMinBidAmount() != null
                ? chit.getMinBidAmount().max(regulatoryFloor) : regulatoryFloor;
        BigDecimal absoluteMax = chit.getMaxBidAmount() != null
                ? chit.getMaxBidAmount().min(totalChitValue) : totalChitValue;

        // ── Competition density ─────────────────────────────────────────────
        int totalMembers = chit.getTotalMembers();
        int winnersCount = (int) pastAuctions.stream()
                .filter(a -> a.getWinningBidAmount() != null).count();
        int eligibleBidders = Math.max(1, totalMembers - winnersCount);

        // Higher competition (more eligible) → bids drop faster (members compete harder)
        double competitionFactor = Math.min(1.0, (double) eligibleBidders / totalMembers);

        // ── Urgency decay model ─────────────────────────────────────────────
        // Early months: low urgency → bids close to chit value (people wait)
        // Late months: high urgency → bids drop (remaining members compete)
        double progressRatio = (double)(currentMonthNumber - 1) / Math.max(1, totalMonths - 1);
        // Sigmoid-like urgency curve (gradual start, steep mid, plateau end)
        double urgency = 1.0 / (1.0 + Math.exp(-6.0 * (progressRatio - 0.5)));
        // Discount increases with urgency and competition
        double maxDiscountRate = 0.25; // maximum 25% discount from chit value
        double discountRate = maxDiscountRate * urgency * competitionFactor;

        // ── Smart recommended range ─────────────────────────────────────────
        BigDecimal baselineHigh = totalChitValue.multiply(bd(1.0 - discountRate * 0.6))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal baselineLow = totalChitValue.multiply(bd(1.0 - discountRate * 1.2))
                .setScale(2, RoundingMode.HALF_UP);

        // Adjust with trend: if bids are falling, shift range down; if rising, shift up
        if (trend != null) {
            BigDecimal trendAdj = trend.multiply(bd(0.3)).setScale(2, RoundingMode.HALF_UP);
            baselineHigh = baselineHigh.add(trendAdj);
            baselineLow = baselineLow.add(trendAdj);
        }

        // Clip to absolute limits
        BigDecimal recommendedMin = baselineLow.max(absoluteMin).setScale(2, RoundingMode.HALF_UP);
        BigDecimal recommendedMax = baselineHigh.min(absoluteMax).setScale(2, RoundingMode.HALF_UP);
        if (recommendedMin.compareTo(recommendedMax) > 0) {
            recommendedMin = absoluteMin;
            recommendedMax = absoluteMax;
        }

        // ── Commission & payout at midpoint ─────────────────────────────────
        BigDecimal midBid = recommendedMin.add(recommendedMax).divide(bd(2), 2, RoundingMode.HALF_UP);
        BigDecimal estimatedCommission = midBid.multiply(commissionPct)
                .divide(bd(100), 2, RoundingMode.HALF_UP);
        BigDecimal estimatedPayout = midBid.subtract(estimatedCommission);

        // ── Dividend forecast for non-winners ───────────────────────────────
        // If a member does NOT win this month, what dividend they earn
        BigDecimal expectedDiscount = totalChitValue.subtract(midBid);
        BigDecimal dividendPerMember = expectedDiscount.subtract(estimatedCommission)
                .divide(bd(Math.max(1, totalMembers - 1)), 2, RoundingMode.HALF_UP);

        // Cumulative projected dividends if they stay till the end
        BigDecimal projectedTotalDividend = dividendPerMember.multiply(bd(monthsRemaining));

        // ── Win probability estimation ──────────────────────────────────────
        // Simple model: 1/eligibleBidders per month, higher if you bid lower
        double baseWinProb = 1.0 / eligibleBidders;
        double aggressiveBidProb = Math.min(0.95, baseWinProb * 2.5); // bidding at recommended min
        double conservativeBidProb = Math.max(0.02, baseWinProb * 0.4); // bidding at recommended max

        // ── Strategy advice ─────────────────────────────────────────────────
        String strategy;
        if (currentMonthNumber <= totalMonths * 0.3) {
            strategy = "PATIENT — Early months. Non-winners earn dividends. Bid high (close to chit value) unless you need funds urgently. Patience builds profit.";
        } else if (currentMonthNumber <= totalMonths * 0.7) {
            strategy = "BALANCED — Mid-cycle. Competition peaks. If you need funds, bid in the recommended range. Otherwise, accumulate dividends.";
        } else {
            strategy = "AGGRESSIVE — Late months. Fewer eligible bidders remain. Good opportunity to win with a moderate bid. Your accumulated dividends provide a safety net.";
        }

        // ── Populate result ─────────────────────────────────────────────────
        result.put("recommendedMinBid", recommendedMin);
        result.put("recommendedMaxBid", recommendedMax);
        result.put("absoluteMinBid", absoluteMin);
        result.put("absoluteMaxBid", absoluteMax);
        result.put("estimatedCommission", estimatedCommission);
        result.put("estimatedPayout", estimatedPayout);
        result.put("historicalAvgBid", historicalAvg);
        result.put("historicalMinBid", historicalMin);
        result.put("bidTrend", trend);
        result.put("currentMonthNumber", currentMonthNumber);
        result.put("totalChitValue", totalChitValue);
        result.put("monthlyAmount", chit.getMonthlyAmount());
        result.put("commissionPercentage", commissionPct);
        result.put("monthsRemaining", monthsRemaining);
        result.put("eligibleBidders", eligibleBidders);
        result.put("competitionFactor", BigDecimal.valueOf(competitionFactor).setScale(2, RoundingMode.HALF_UP));
        result.put("urgencyScore", BigDecimal.valueOf(urgency * 100).setScale(1, RoundingMode.HALF_UP));
        result.put("dividendPerMember", dividendPerMember);
        result.put("projectedTotalDividend", projectedTotalDividend);
        result.put("winProbLow", BigDecimal.valueOf(aggressiveBidProb * 100).setScale(1, RoundingMode.HALF_UP));
        result.put("winProbHigh", BigDecimal.valueOf(conservativeBidProb * 100).setScale(1, RoundingMode.HALF_UP));
        result.put("strategy", strategy);

        return result;
    }

    // ── For-amount calculator ────────────────────────────────────────────────
    public Map<String, BigDecimal> calculateForBidAmount(Chit chit, BigDecimal bidAmount) {
        BigDecimal commission = bidAmount.multiply(chit.getAdminCommissionPercentage())
                .divide(bd(100), 2, RoundingMode.HALF_UP);
        BigDecimal payout = bidAmount.subtract(commission);
        BigDecimal totalChitValue = chit.getTotalChitValue() != null
                ? chit.getTotalChitValue()
                : chit.getMonthlyAmount().multiply(BigDecimal.valueOf(chit.getTotalMembers()));
        BigDecimal discount = totalChitValue.subtract(bidAmount);
        BigDecimal discountPercent = discount.multiply(bd(100))
                .divide(totalChitValue, 2, RoundingMode.HALF_UP);
        return Map.of("commission", commission, "payout", payout,
                "discount", discount, "discountPercent", discountPercent);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private BigDecimal computeAvg(List<BigDecimal> values) {
        if (values.isEmpty()) return null;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(bd(values.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Computes a simple linear trend. Positive = bids increasing, negative = decreasing.
     */
    private BigDecimal computeTrend(List<BigDecimal> bids) {
        if (bids.size() < 2) return null;
        // Simple: (last - first) / count — gives average change per period
        BigDecimal first = bids.get(0);
        BigDecimal last = bids.get(bids.size() - 1);
        return last.subtract(first).divide(bd(bids.size()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal bd(double val) { return BigDecimal.valueOf(val); }
    private static BigDecimal bd(int val) { return BigDecimal.valueOf(val); }
}
