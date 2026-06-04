package com.ygc.service;

import com.ygc.model.Auction;
import com.ygc.model.Chit;
import com.ygc.repository.AuctionRepository;
import com.ygc.repository.BidRepository;
import com.ygc.util.LoggingUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidCalculationServiceTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private BidRepository bidRepository;
    @Mock private LoggingUtil loggingUtil;

    @InjectMocks
    private BidCalculationService bidCalculationService;

    private Chit testChit;

    @BeforeEach
    void setUp() {
        testChit = new Chit();
        testChit.setId(1L);
        testChit.setName("Gold Chit");
        testChit.setMonthlyAmount(new BigDecimal("5000"));
        testChit.setTotalMembers(10);
        testChit.setDurationMonths(12);
        testChit.setTotalChitValue(new BigDecimal("50000"));
        testChit.setAdminCommissionPercentage(new BigDecimal("5"));
        testChit.setStartDate(LocalDate.now().plusDays(30));
    }

    @Test
    @DisplayName("should calculate recommendations with all required keys")
    void shouldCalculateRecommendations() {
        when(auctionRepository.findByChit(testChit)).thenReturn(List.of());

        Map<String, Object> result = bidCalculationService.calculateBidRecommendations(testChit, 1);

        assertThat(result).containsKeys(
                "recommendedMinBid", "recommendedMaxBid",
                "absoluteMinBid", "absoluteMaxBid",
                "estimatedCommission", "estimatedPayout",
                "historicalAvgBid", "currentMonthNumber",
                "totalChitValue", "monthlyAmount",
                "commissionPercentage", "monthsRemaining");
    }

    @Test
    @DisplayName("recommended min should not be below absolute min")
    void minShouldRespectAbsoluteFloor() {
        when(auctionRepository.findByChit(testChit)).thenReturn(List.of());

        Map<String, Object> result = bidCalculationService.calculateBidRecommendations(testChit, 1);

        BigDecimal recMin = (BigDecimal) result.get("recommendedMinBid");
        BigDecimal absMin = (BigDecimal) result.get("absoluteMinBid");
        assertThat(recMin).isGreaterThanOrEqualTo(absMin);
    }

    @Test
    @DisplayName("recommended max should not exceed absolute max")
    void maxShouldRespectAbsoluteCeiling() {
        when(auctionRepository.findByChit(testChit)).thenReturn(List.of());

        Map<String, Object> result = bidCalculationService.calculateBidRecommendations(testChit, 1);

        BigDecimal recMax = (BigDecimal) result.get("recommendedMaxBid");
        BigDecimal absMax = (BigDecimal) result.get("absoluteMaxBid");
        assertThat(recMax).isLessThanOrEqualTo(absMax);
    }

    @Test
    @DisplayName("should use historical average when past auctions exist")
    void shouldUseHistoricalAverage() {
        Auction pastAuction = new Auction();
        pastAuction.setWinningBidAmount(new BigDecimal("45000"));
        when(auctionRepository.findByChit(testChit)).thenReturn(List.of(pastAuction));

        Map<String, Object> result = bidCalculationService.calculateBidRecommendations(testChit, 2);

        assertThat(result.get("historicalAvgBid")).isNotNull();
        assertThat((BigDecimal) result.get("historicalAvgBid"))
                .isEqualByComparingTo("45000");
    }

    @Test
    @DisplayName("should respect admin-configured min/max bid amounts")
    void shouldRespectConfiguredLimits() {
        testChit.setMinBidAmount(new BigDecimal("40000"));
        testChit.setMaxBidAmount(new BigDecimal("48000"));
        when(auctionRepository.findByChit(testChit)).thenReturn(List.of());

        Map<String, Object> result = bidCalculationService.calculateBidRecommendations(testChit, 1);

        BigDecimal absMax = (BigDecimal) result.get("absoluteMaxBid");
        assertThat(absMax).isLessThanOrEqualTo(new BigDecimal("48000"));
    }

    @Test
    @DisplayName("calculateForBidAmount should compute commission and payout correctly")
    void shouldCalculateForBidAmount() {
        Map<String, BigDecimal> result = bidCalculationService.calculateForBidAmount(testChit, new BigDecimal("45000"));

        assertThat(result.get("commission")).isEqualByComparingTo("2250.00");
        assertThat(result.get("payout")).isEqualByComparingTo("42750.00");
    }

    @Test
    @DisplayName("later months should have lower urgency multiplier")
    void laterMonthsShouldHaveLowerBids() {
        when(auctionRepository.findByChit(testChit)).thenReturn(List.of());

        Map<String, Object> earlyResult = bidCalculationService.calculateBidRecommendations(testChit, 1);
        Map<String, Object> lateResult = bidCalculationService.calculateBidRecommendations(testChit, 11);

        BigDecimal earlyMax = (BigDecimal) earlyResult.get("recommendedMaxBid");
        BigDecimal lateMax = (BigDecimal) lateResult.get("recommendedMaxBid");

        // Later months should generally have lower recommended bids
        assertThat(lateMax).isLessThanOrEqualTo(earlyMax);
    }
}
