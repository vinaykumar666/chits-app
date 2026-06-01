package com.ygc.service;

import com.ygc.model.Auction;
import com.ygc.model.Bid;
import com.ygc.model.Chit;
import com.ygc.repository.AuctionRepository;
import com.ygc.repository.BidRepository;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intelligent Bid Calculation Service.
 * Calculates recommended bid ranges, expected commissions and estimated customer payouts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BidCalculationService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final LoggingUtil loggingUtil;

    /**
     * Returns a map of bid recommendation details for a given chit and auction month.
     *
     * Keys:
     *  recommendedMinBid     - lower bound of suggested bid range
     *  recommendedMaxBid     - upper bound of suggested bid range
     *  absoluteMinBid        - hard floor (admin-configured or regulatory)
     *  absoluteMaxBid        - hard ceiling (admin-configured or total chit value)
     *  estimatedCommission   - commission at recommended mid-point bid
     *  estimatedPayout       - payout at recommended mid-point bid (bid - commission)
     *  historicalAvgBid      - average winning bid from past auctions in this chit
     *  currentMonthNumber    - the month number being calculated for
     *  totalChitValue        - total value of the chit fund
     *  monthlyAmount         - monthly contribution amount
     *  commissionPercentage  - admin commission %
     */
    public Map<String, Object> calculateBidRecommendations(Chit chit, Integer currentMonthNumber) {
        loggingUtil.debug("Calculating bid recommendations for chit: " + chit.getId()
                + ", month: " + currentMonthNumber, "BidCalculationService");

        Map<String, Object> result = new HashMap<>();

        BigDecimal totalChitValue = chit.getTotalChitValue() != null
                ? chit.getTotalChitValue()
                : chit.getMonthlyAmount().multiply(BigDecimal.valueOf(chit.getTotalMembers()));

        BigDecimal commissionPct = chit.getAdminCommissionPercentage();
        int totalMonths = chit.getDurationMonths();
        int monthsRemaining = Math.max(1, totalMonths - (currentMonthNumber - 1));

        // --- Absolute limits ---
        // Regulatory floor: bid must be at least 70% of total chit value (common RBI guideline)
        BigDecimal regulatoryFloor = totalChitValue.multiply(BigDecimal.valueOf(0.70))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal absoluteMin = chit.getMinBidAmount() != null
                ? chit.getMinBidAmount().max(regulatoryFloor)
                : regulatoryFloor;

        BigDecimal absoluteMax = chit.getMaxBidAmount() != null
                ? chit.getMaxBidAmount().min(totalChitValue)
                : totalChitValue;

        // --- Historical analysis ---
        List<Auction> pastAuctions = auctionRepository.findByChit(chit);
        BigDecimal historicalAvg = computeHistoricalAvgWinningBid(pastAuctions);

        // --- Smart recommended range ---
        // Early months: members have less urgency → bids cluster near maximum (closer to chit value)
        // Late months:  urgency rises for members who haven't won → bids drop (compete harder)
        double urgencyFactor = 1.0 - (0.20 * ((double)(currentMonthNumber - 1) / Math.max(1, totalMonths - 1)));
        BigDecimal urgencyMultiplier = BigDecimal.valueOf(urgencyFactor).setScale(4, RoundingMode.HALF_UP);

        BigDecimal midPoint = historicalAvg != null
                ? historicalAvg.multiply(urgencyMultiplier).setScale(2, RoundingMode.HALF_UP)
                : totalChitValue.multiply(urgencyMultiplier).setScale(2, RoundingMode.HALF_UP);

        // Recommended range: ±5% around urgency-adjusted midpoint, clipped to absolute limits
        BigDecimal spread = midPoint.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal recommendedMin = midPoint.subtract(spread).max(absoluteMin).setScale(2, RoundingMode.HALF_UP);
        BigDecimal recommendedMax = midPoint.add(spread).min(absoluteMax).setScale(2, RoundingMode.HALF_UP);

        // Ensure recommendedMin <= recommendedMax
        if (recommendedMin.compareTo(recommendedMax) > 0) {
            recommendedMin = absoluteMin;
            recommendedMax = absoluteMax;
        }

        // Commission and payout at mid-point
        BigDecimal midBid = recommendedMin.add(recommendedMax)
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal estimatedCommission = midBid.multiply(commissionPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal estimatedPayout = midBid.subtract(estimatedCommission);

        result.put("recommendedMinBid", recommendedMin);
        result.put("recommendedMaxBid", recommendedMax);
        result.put("absoluteMinBid", absoluteMin);
        result.put("absoluteMaxBid", absoluteMax);
        result.put("estimatedCommission", estimatedCommission);
        result.put("estimatedPayout", estimatedPayout);
        result.put("historicalAvgBid", historicalAvg);
        result.put("currentMonthNumber", currentMonthNumber);
        result.put("totalChitValue", totalChitValue);
        result.put("monthlyAmount", chit.getMonthlyAmount());
        result.put("commissionPercentage", commissionPct);
        result.put("monthsRemaining", monthsRemaining);

        loggingUtil.info("Bid recommendation: min=" + recommendedMin + " max=" + recommendedMax
                + " payout=" + estimatedPayout, "BidCalculationService");
        return result;
    }

    /**
     * Calculates commission and payout for any arbitrary bid amount.
     */
    public Map<String, BigDecimal> calculateForBidAmount(Chit chit, BigDecimal bidAmount) {
        BigDecimal commission = bidAmount.multiply(chit.getAdminCommissionPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal payout = bidAmount.subtract(commission);
        return Map.of("commission", commission, "payout", payout);
    }

    private BigDecimal computeHistoricalAvgWinningBid(List<Auction> auctions) {
        List<BigDecimal> winningBids = auctions.stream()
                .filter(a -> a.getWinningBidAmount() != null)
                .map(Auction::getWinningBidAmount)
                .toList();

        if (winningBids.isEmpty()) return null;

        BigDecimal sum = winningBids.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(winningBids.size()), 2, RoundingMode.HALF_UP);
    }
}
