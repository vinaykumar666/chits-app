package com.ygc.service;

import com.ygc.model.Auction;
import com.ygc.model.Chit;
import com.ygc.repository.AuctionRepository;
import com.ygc.repository.BidRepository;
import com.ygc.repository.ChitMembershipRepository;
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
    @Mock private ChitMembershipRepository membershipRepository;
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
    @DisplayName("should return all required recommendation keys")
    void shouldReturnAllKeys() {
        when(auctionRepository.findByChit(testChit)).thenReturn(List.of());
        Map<String, Object> result = bidCalculationService.calculateBidRecommendations(testChit, 1);

        assertThat(result).containsKeys(
                "recommendedMinBid", "recommendedMaxBid",
                "absoluteMinBid", "absoluteMaxBid",
                "estimatedCommission", "estimatedPayout",
                "historicalAvgBid", "currentMonthNumber",
                "totalChitValue", "monthlyAmount",
                "commissionPercentage", "monthsRemaining",
                "eligibleBidders", "urgencyScore",
                "dividendPerMember", "strategy",
                "winProbLow", "winProbHigh");
    }

    @Test
    @DisplayName("recommended min should not be below absolute min")
    void minShouldRespectFloor() {
        when(auctionRepository.findByChit(testChit)).thenReturn(List.of());
        Map<String, Object> result = bidCalculationService.calculateBidRecommendations(testChit, 1);
        assertThat((BigDecimal) result.get("recommendedMinBid"))
                .isGreaterThanOrEqualTo((BigDecimal) result.get("absoluteMinBid"));
    }

    @Test
    @DisplayName("recommended max should not exceed absolute max")
    void maxShouldRespectCeiling() {
        when(auctionRepository.findByChit(testChit)).thenReturn(List.of());
        Map<String, Object> result = bidCalculationService.calculateBidRecommendations(testChit, 1);
        assertThat((BigDecimal) result.get("recommendedMaxBid"))
                .isLessThanOrEqualTo((BigDecimal) result.get("absoluteMaxBid"));
    }

    @Test
    @DisplayName("should use historical average when past auctions exist")
    void shouldUseHistoricalAvg() {
        Auction a1 = new Auction(); a1.setWinningBidAmount(new BigDecimal("45000")); a1.setMonthNumber(1);
        Auction a2 = new Auction(); a2.setWinningBidAmount(new BigDecimal("43000")); a2.setMonthNumber(2);
        when(auctionRepository.findByChit(testChit)).thenReturn(List.of(a1, a2));

        Map<String, Object> result = bidCalculationService.calculateBidRecommendations(testChit, 3);
        assertThat((BigDecimal) result.get("historicalAvgBid")).isEqualByComparingTo("44000");
    }

    @Test
    @DisplayName("later months should have higher urgency score")
    void laterMonthsShouldHaveHigherUrgency() {
        when(auctionRepository.findByChit(testChit)).thenReturn(List.of());
        Map<String, Object> early = bidCalculationService.calculateBidRecommendations(testChit, 1);
        Map<String, Object> late = bidCalculationService.calculateBidRecommendations(testChit, 11);

        BigDecimal earlyUrgency = (BigDecimal) early.get("urgencyScore");
        BigDecimal lateUrgency = (BigDecimal) late.get("urgencyScore");
        assertThat(lateUrgency).isGreaterThan(earlyUrgency);
    }

    @Test
    @DisplayName("calculateForBidAmount should compute commission, payout, and discount")
    void shouldCalculateForBidAmount() {
        Map<String, BigDecimal> result = bidCalculationService.calculateForBidAmount(testChit, new BigDecimal("45000"));
        assertThat(result.get("commission")).isEqualByComparingTo("2250.00");
        assertThat(result.get("payout")).isEqualByComparingTo("42750.00");
        assertThat(result.get("discount")).isEqualByComparingTo("5000");
        assertThat(result.get("discountPercent")).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("strategy should change with month progression")
    void strategyShouldChangeOverTime() {
        when(auctionRepository.findByChit(testChit)).thenReturn(List.of());
        String earlyStrategy = (String) bidCalculationService.calculateBidRecommendations(testChit, 1).get("strategy");
        String lateStrategy = (String) bidCalculationService.calculateBidRecommendations(testChit, 11).get("strategy");
        assertThat(earlyStrategy).startsWith("PATIENT");
        assertThat(lateStrategy).startsWith("AGGRESSIVE");
    }

    @Test
    @DisplayName("dividendPerMember should be positive when discount exists")
    void dividendShouldBePositive() {
        when(auctionRepository.findByChit(testChit)).thenReturn(List.of());
        Map<String, Object> result = bidCalculationService.calculateBidRecommendations(testChit, 6);
        assertThat((BigDecimal) result.get("dividendPerMember")).isPositive();
    }
}
