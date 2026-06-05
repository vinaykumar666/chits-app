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
            long late = payments.stream().filter(p -> p.getLateFine().compareTo(BigDecimal.ZERO) > 0).count();

            double overdueRatio = (double) overdue / total;
            double rejectedRatio = (double) rejected / total;
            double lateRatio = (double) late / total;

            score += overdueRatio * 30; // Max 30 points
            score += rejectedRatio * 10; // Max 10 points
            score += lateRatio * 10;     // Max 10 points
        }

        // 2. Login security (20% weight)
        long failedLogins = loginHistoryRepository.countByUserAndSuccessFalseAndLoginAtAfter(
                user, LocalDateTime.now().minusDays(30));
        score += Math.min(20, failedLogins * 4); // 5 fails = 20 points

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
        log.info("Recalculating risk scores for all members...");
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.MEMBER)
                .forEach(u -> {
                    int newScore = calculateRiskScore(u);
                    if (u.getRiskScore() != newScore) {
                        u.setRiskScore(newScore);
                        userRepository.save(u);
                    }
                });
        log.info("Risk scores updated.");
    }
}
