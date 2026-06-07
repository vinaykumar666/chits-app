package com.ygc.service;

import com.ygc.model.*;
import com.ygc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-like Risk Scoring Engine.
 *
 * Computes a 0-100 risk score per member based on:
 *  - Payment history (late %, overdue count)
 *  - Login patterns (failed logins, unusual IPs)
 *  - Membership stability (exits, rejections)
 *  - Financial behavior (fine accumulation)
 *
 * Risk Tiers:
 *  0-20  = LOW (green)   — Reliable member, on-time payments
 *  21-50 = MEDIUM (amber) — Occasional delays, watch list
 *  51-75 = HIGH (orange)  — Frequent late payments, potential defaulter
 *  76-100= CRITICAL (red) — Active defaulter, action required
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskScoreService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ChitMembershipRepository membershipRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * Calculate risk score for a single user.
     */
    public int calculateRiskScore(User user) {
        double score = 0;

        // 1. Payment behavior (50% weight)
        List<Payment> payments = paymentRepository.findByMembershipUser(user);
        if (!payments.isEmpty()) {
            long total = payments.size();
            long overdue = payments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.OVERDUE).count();
            long rejected = payments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.REJECTED).count();
            long late = payments.stream().filter(p -> p.getLateFine() != null && p.getLateFine().compareTo(BigDecimal.ZERO) > 0).count();

            double overdueRatio = (double) overdue / total;
            double rejectedRatio = (double) rejected / total;
            double lateRatio = (double) late / total;

            score += overdueRatio * 30; // Max 30 points
            score += rejectedRatio * 10; // Max 10 points
            score += lateRatio * 10;     // Max 10 points
        }

        // 2. Login security (20% weight)
        try {
            long failedLogins = loginHistoryRepository.countByUserAndSuccessFalseAndLoginAtAfter(
                    user, LocalDateTime.now().minusDays(30));
            score += Math.min(20, failedLogins * 4); // 5 fails = 20 points
        } catch (Exception e) {
            log.debug("Could not fetch login history for user {}: {}", user.getEmail(), e.getMessage());
        }

        // 3. Account flags (15% weight)
        if (user.isAccountLocked()) score += 15;
        if (user.getConsecutiveFailedLogins() >= 3) score += 5;

        // 4. Membership stability (15% weight)
        List<ChitMembership> memberships = membershipRepository.findByUser(user);
        long exited = memberships.stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.EXITED).count();
        if (!memberships.isEmpty()) {
            double exitRatio = (double) exited / memberships.size();
            score += exitRatio * 15;
        }

        return Math.min(100, (int) Math.round(score));
    }

    /**
     * Get risk tier label.
     */
    public static String getRiskTier(int score) {
        if (score <= 20) return "LOW";
        if (score <= 50) return "MEDIUM";
        if (score <= 75) return "HIGH";
        return "CRITICAL";
    }

    public static String getRiskColor(int score) {
        if (score <= 20) return "success";
        if (score <= 50) return "warning";
        if (score <= 75) return "orange";
        return "danger";
    }

    /**
     * Predict members likely to default in next 30 days.
     */
    public List<Map<String, Object>> predictDefaulters() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        List<User> members = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.MEMBER && u.isActive())
                .toList();

        for (User m : members) {
            int risk = calculateRiskScore(m);
            if (risk >= 50) {
                Map<String, Object> alert = new LinkedHashMap<>();
                alert.put("user", m);
                alert.put("riskScore", risk);
                alert.put("tier", getRiskTier(risk));
                alert.put("color", getRiskColor(risk));
                alerts.add(alert);
            }
        }
        alerts.sort((a, b) -> (int) b.get("riskScore") - (int) a.get("riskScore"));
        return alerts;
    }

    /**
     * Scheduled: Recalculate all risk scores nightly.
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    public void recalculateAllScores() {
        log.info("Recalculating risk scores and trust ratings for all members...");
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.MEMBER)
                .forEach(u -> {
                    int newScore = calculateRiskScore(u);
                    String newRating = calculateTrustRating(u, newScore);
                    if (u.getRiskScore() != newScore || !newRating.equals(u.getTrustRating())) {
                        u.setRiskScore(newScore);
                        u.setTrustRating(newRating);
                        userRepository.save(u);
                    }
                });
        log.info("Risk scores and trust ratings updated.");
    }

    /**
     * Trust Rating: PLATINUM / GOLD / SILVER / WATCHLIST
     * Based on: risk score + payment consistency + chit completion history
     */
    public String calculateTrustRating(User user, int riskScore) {
        List<ChitMembership> memberships = membershipRepository.findByUser(user);
        long completed = memberships.stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.SETTLED).count();
        long exited = memberships.stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.EXITED).count();
        long active = memberships.stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE).count();

        List<Payment> payments = paymentRepository.findByMembershipUser(user);
        long onTime = payments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.APPROVED
                        && (p.getLateFine() == null || p.getLateFine().compareTo(BigDecimal.ZERO) == 0))
                .count();
        long total = payments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.APPROVED).count();
        double onTimeRate = total > 0 ? (double) onTime / total : 0;

        // Scoring: 0-100 trust points
        int trustPoints = 50; // Base
        trustPoints -= riskScore / 2; // Lower risk = higher trust
        trustPoints += (int)(onTimeRate * 30); // Max 30 from on-time payments
        trustPoints += (int)(completed * 10); // 10 per completed chit
        trustPoints -= (int)(exited * 15); // -15 per exit
        trustPoints += (int)(active * 5); // 5 per active membership

        if (trustPoints >= 80) return "PLATINUM";
        if (trustPoints >= 60) return "GOLD";
        if (trustPoints >= 35) return "SILVER";
        return "WATCHLIST";
    }

    /**
     * Collection forecasting: predict next month's collection based on historical trend.
     */
    public Map<String, Object> forecastCollection(Chit chit) {
        Map<String, Object> forecast = new HashMap<>();
        List<ChitMembership> active = membershipRepository.findByChit(chit).stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE).toList();

        BigDecimal monthlyTarget = chit.getMonthlyAmount().multiply(BigDecimal.valueOf(active.size()));
        BigDecimal totalCollected = BigDecimal.ZERO;
        long totalPayments = 0;
        long onTimePayments = 0;

        for (ChitMembership m : active) {
            List<Payment> payments = paymentRepository.findByMembership(m);
            for (Payment p : payments) {
                if (p.getStatus() == Payment.PaymentStatus.APPROVED) {
                    totalCollected = totalCollected.add(p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO);
                    totalPayments++;
                    if (p.getLateFine() == null || p.getLateFine().compareTo(BigDecimal.ZERO) == 0) onTimePayments++;
                }
            }
        }

        double collectionRate = active.isEmpty() ? 0 : totalCollected.doubleValue() /
                (chit.getMonthlyAmount().doubleValue() * active.size() * Math.max(1, chit.getDurationMonths()));
        double onTimeRate = totalPayments > 0 ? (double) onTimePayments / totalPayments : 1;

        forecast.put("monthlyTarget", monthlyTarget);
        forecast.put("predictedCollection", monthlyTarget.multiply(BigDecimal.valueOf(collectionRate)).setScale(0, RoundingMode.HALF_UP));
        forecast.put("collectionRate", (int)(collectionRate * 100));
        forecast.put("onTimeRate", (int)(onTimeRate * 100));
        forecast.put("activeMembers", active.size());
        forecast.put("atRiskMembers", active.stream().filter(m -> m.getUser().getRiskScore() > 50).count());
        return forecast;
    }

    /**
     * Detect duplicate Aadhaar numbers across users.
     */
    public List<Map<String, String>> detectDuplicateAadhaar() {
        List<Map<String, String>> duplicates = new ArrayList<>();
        Map<String, List<User>> aadhaarMap = userRepository.findAll().stream()
                .filter(u -> u.getAadhaarNumber() != null && !u.getAadhaarNumber().isBlank())
                .collect(Collectors.groupingBy(User::getAadhaarNumber));
        aadhaarMap.forEach((aadhaar, users) -> {
            if (users.size() > 1) {
                users.forEach(u -> {
                    Map<String, String> entry = new HashMap<>();
                    entry.put("type", "AADHAAR");
                    entry.put("value", u.getMaskedAadhaar());
                    entry.put("userName", u.getFullName());
                    entry.put("email", u.getEmail());
                    duplicates.add(entry);
                });
            }
        });
        return duplicates;
    }

    /**
     * Detect duplicate phone numbers across users.
     */
    public List<Map<String, String>> detectDuplicatePhone() {
        List<Map<String, String>> duplicates = new ArrayList<>();
        Map<String, List<User>> phoneMap = userRepository.findAll().stream()
                .filter(u -> u.getPhone() != null && !u.getPhone().isBlank())
                .collect(Collectors.groupingBy(User::getPhone));
        phoneMap.forEach((phone, users) -> {
            if (users.size() > 1) {
                users.forEach(u -> {
                    Map<String, String> entry = new HashMap<>();
                    entry.put("type", "PHONE");
                    entry.put("value", phone);
                    entry.put("userName", u.getFullName());
                    entry.put("email", u.getEmail());
                    duplicates.add(entry);
                });
            }
        });
        return duplicates;
    }
}
