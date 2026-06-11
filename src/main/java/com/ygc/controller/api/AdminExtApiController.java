package com.ygc.controller.api;

import com.ygc.audit.AuditService;
import com.ygc.dto.DtoMapper;
import com.ygc.dto.UserDto;
import com.ygc.model.*;
import com.ygc.repository.*;
import com.ygc.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST endpoints for admin specialty pages (early exits, risk, fraud, security, documents).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminExtApiController {

    private final ApiSupport apiSupport;
    private final DtoMapper dtoMapper;
    private final EarlyExitService earlyExitService;
    private final RiskScoreService riskScoreService;
    private final LoginTrackingService loginTrackingService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ChitMembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final AuditService auditService;

    @GetMapping("/early-exits")
    public ResponseEntity<Map<String, Object>> earlyExits() {
        List<Map<String, Object>> requests = earlyExitService.getAllRequests().stream()
                .map(this::toEarlyExitMap)
                .toList();
        return ResponseEntity.ok(Map.of("requests", requests));
    }

    @PostMapping("/early-exits/{id}/process")
    public ResponseEntity<Map<String, String>> processEarlyExit(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String remarks = body.get("remarks") != null ? body.get("remarks").toString() : "";
        earlyExitService.processExit(id, approved, remarks, apiSupport.currentUser(auth), null);
        return ResponseEntity.ok(Map.of("message", (approved ? "Approved" : "Rejected") + " early exit request"));
    }

    @GetMapping("/risk-dashboard")
    public ResponseEntity<Map<String, Object>> riskDashboard() {
        List<Map<String, Object>> alerts = riskScoreService.predictDefaulters().stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    User u = (User) a.get("user");
                    m.put("user", dtoMapper.toUserDto(u));
                    m.put("riskScore", a.get("riskScore"));
                    m.put("tier", a.get("tier"));
                    m.put("color", a.get("color"));
                    return m;
                }).toList();
        List<Map<String, Object>> recentLogins = loginTrackingService.getRecentLogins().stream()
                .map(this::toLoginHistoryMap)
                .toList();
        return ResponseEntity.ok(Map.of("alerts", alerts, "recentLogins", recentLogins));
    }

    @GetMapping("/fraud-detection")
    public ResponseEntity<Map<String, Object>> fraudDetection() {
        List<Map<String, Object>> highRisk = riskScoreService.predictDefaulters().stream()
                .map(a -> {
                    User u = (User) a.get("user");
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("user", dtoMapper.toUserDto(u));
                    m.put("riskScore", a.get("riskScore"));
                    m.put("tier", a.get("tier"));
                    return m;
                }).toList();
        List<UserDto> watchlist = userRepository.findAll().stream()
                .filter(u -> "WATCHLIST".equals(u.getTrustRating()))
                .map(dtoMapper::toUserDto)
                .toList();
        return ResponseEntity.ok(Map.of(
                "duplicateAadhaar", riskScoreService.detectDuplicateAadhaar(),
                "duplicatePhone", riskScoreService.detectDuplicatePhone(),
                "highRiskMembers", highRisk,
                "watchlistMembers", watchlist));
    }

    @GetMapping("/login-tracking")
    public ResponseEntity<Map<String, Object>> loginTracking() {
        List<LoginHistory> allLogins = loginTrackingService.getRecentLogins();
        List<User> locked = userRepository.findAll().stream().filter(User::isAccountLocked).toList();
        List<User> failedUsers = userRepository.findAll().stream()
                .filter(u -> u.getConsecutiveFailedLogins() > 0)
                .sorted(Comparator.comparingInt(User::getConsecutiveFailedLogins).reversed())
                .toList();
        List<User> members = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.MEMBER && u.isActive())
                .toList();
        long aadhaarVerified = members.stream().filter(User::isAadhaarVerified).count();
        long totalLogins = allLogins.size();
        long failedLogins = allLogins.stream().filter(l -> !l.isSuccess()).count();
        long successLogins = totalLogins - failedLogins;
        int successRate = totalLogins > 0 ? (int) (successLogins * 100 / totalLogins) : 100;
        long uniqueIPs = allLogins.stream().map(LoginHistory::getIpAddress)
                .filter(Objects::nonNull).distinct().count();

        Map<String, Set<String>> userIpMap = new HashMap<>();
        for (LoginHistory l : allLogins) {
            if (l.getUser() != null && l.getIpAddress() != null) {
                userIpMap.computeIfAbsent(l.getUser().getEmail(), k -> new HashSet<>())
                        .add(l.getIpAddress());
            }
        }
        List<String> suspiciousUsers = userIpMap.entrySet().stream()
                .filter(e -> e.getValue().size() >= 3)
                .map(Map.Entry::getKey)
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("recentLogins", allLogins.stream().map(this::toLoginHistoryMap).toList());
        body.put("lockedAccounts", locked.stream().map(dtoMapper::toUserDto).toList());
        body.put("failedLoginUsers", failedUsers.stream().map(dtoMapper::toUserDto).toList());
        body.put("allMembers", members.stream().map(this::toSecurityUserMap).toList());
        body.put("aadhaarVerified", aadhaarVerified);
        body.put("aadhaarPending", members.size() - aadhaarVerified);
        body.put("totalLogins", totalLogins);
        body.put("failedLogins", failedLogins);
        body.put("successRate", successRate);
        body.put("uniqueIPs", uniqueIPs);
        body.put("aadhaarCompliancePct",
                members.isEmpty() ? 100 : (int) (aadhaarVerified * 100 / members.size()));
        body.put("suspiciousUsers", suspiciousUsers);
        body.put("suspiciousCount", suspiciousUsers.size());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/members/{id}/toggle-aadhaar")
    public ResponseEntity<Map<String, String>> toggleAadhaar(@PathVariable Long id, Authentication auth) {
        User member = userService.findById(id);
        member.setAadhaarVerified(!member.isAadhaarVerified());
        userRepository.save(member);
        String status = member.isAadhaarVerified() ? "verified" : "unverified";
        auditService.log(apiSupport.currentUser(auth), "AADHAAR_" + status.toUpperCase(),
                "User", id, "Aadhaar " + status + " for: " + member.getFullName());
        return ResponseEntity.ok(Map.of("message", "Aadhaar " + status + " for " + member.getFullName()));
    }

    @PostMapping("/members/{id}/reset-login-counter")
    public ResponseEntity<Map<String, String>> resetLoginCounter(@PathVariable Long id, Authentication auth) {
        User member = userService.findById(id);
        member.setConsecutiveFailedLogins(0);
        member.setAccountLocked(false);
        userRepository.save(member);
        auditService.log(apiSupport.currentUser(auth), "RESET_LOGIN_COUNTER",
                "User", id, "Reset failed login counter for: " + member.getFullName());
        return ResponseEntity.ok(Map.of("message", "Account unlocked for " + member.getFullName()));
    }

    @GetMapping("/members/{id}/profile")
    public ResponseEntity<Map<String, Object>> memberProfile(@PathVariable Long id) {
        User member = userService.findById(id);
        List<ChitMembership> memberships = membershipRepository.findByUser(member);
        List<Payment> allPayments = paymentRepository.findByMembershipUser(member);
        long totalPayments = allPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.APPROVED).count();
        long onTime = allPayments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.APPROVED
                && (p.getLateFine() == null || p.getLateFine().compareTo(BigDecimal.ZERO) == 0)).count();
        long overdue = allPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.OVERDUE).count();
        int risk = riskScoreService.calculateRiskScore(member);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("member", toSecurityUserMap(member));
        body.put("memberships", dtoMapper.toMembershipDtos(memberships));
        body.put("activeChits", memberships.stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE).count());
        body.put("completedChits", memberships.stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.SETTLED).count());
        body.put("exitedChits", memberships.stream()
                .filter(m -> m.getStatus() == ChitMembership.MembershipStatus.EXITED).count());
        body.put("totalPayments", totalPayments);
        body.put("onTimePayments", onTime);
        body.put("overduePayments", overdue);
        body.put("paymentScore", totalPayments > 0 ? (int) (onTime * 100 / totalPayments) : 100);
        body.put("riskScore", risk);
        body.put("trustRating", riskScoreService.calculateTrustRating(member, risk));
        body.put("loginHistory", loginTrackingService.getUserLogins(member).stream()
                .map(this::toLoginHistoryMap).toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/documents")
    public ResponseEntity<Map<String, Object>> documents() {
        List<Map<String, Object>> agreements = membershipRepository.findAll().stream()
                .filter(m -> m.getAgreementPdfPath() != null)
                .map(m -> Map.<String, Object>of(
                        "membershipId", m.getId(),
                        "memberName", m.getUser().getFullName(),
                        "chitName", m.getChit().getName(),
                        "path", m.getAgreementPdfPath(),
                        "agreementNumber", m.getAgreementNumber() != null ? m.getAgreementNumber() : ""))
                .collect(Collectors.toList());
        List<Map<String, Object>> certificates = membershipRepository.findAll().stream()
                .filter(m -> m.getCertificatePath() != null)
                .map(m -> Map.<String, Object>of(
                        "membershipId", m.getId(),
                        "memberName", m.getUser().getFullName(),
                        "chitName", m.getChit().getName(),
                        "path", m.getCertificatePath()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "agreements", agreements,
                "certificates", certificates,
                "settlements", dtoMapper.toSettlementDtos(settlementRepository.findAll())));
    }

    @GetMapping("/payments/{id}/screenshot")
    public ResponseEntity<byte[]> paymentScreenshot(@PathVariable Long id) throws Exception {
        Payment payment = paymentRepository.findById(id).orElseThrow();
        if (payment.getScreenshotPath() == null || "N/A".equals(payment.getScreenshotPath())) {
            return ResponseEntity.notFound().build();
        }
        java.nio.file.Path path = java.nio.file.Paths.get(payment.getScreenshotPath());
        if (!java.nio.file.Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = java.nio.file.Files.readAllBytes(path);
        String contentType = java.nio.file.Files.probeContentType(path);
        if (contentType == null) contentType = "image/png";
        return ResponseEntity.ok().header("Content-Type", contentType).body(bytes);
    }

    private Map<String, Object> toEarlyExitMap(EarlyExitRequest req) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", req.getId());
        m.put("status", req.getStatus() != null ? req.getStatus().name() : null);
        m.put("reason", req.getReason());
        m.put("totalPaid", req.getTotalPaid());
        m.put("penaltyAmount", req.getPenaltyAmount());
        m.put("penaltyPercentage", req.getPenaltyPercentage());
        m.put("dividendsEarned", req.getDividendsEarned());
        m.put("refundAmount", req.getRefundAmount());
        m.put("adminRemarks", req.getAdminRemarks());
        m.put("requestedAt", req.getRequestedAt());
        m.put("reviewedAt", req.getReviewedAt());
        if (req.getMembership() != null) {
            m.put("membershipId", req.getMembership().getId());
            if (req.getMembership().getUser() != null) {
                m.put("memberName", req.getMembership().getUser().getFullName());
                m.put("memberEmail", req.getMembership().getUser().getEmail());
            }
            if (req.getMembership().getChit() != null) {
                m.put("chitName", req.getMembership().getChit().getName());
            }
        }
        return m;
    }

    private Map<String, Object> toLoginHistoryMap(LoginHistory lh) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", lh.getId());
        m.put("success", lh.isSuccess());
        m.put("ipAddress", lh.getIpAddress());
        m.put("userAgent", lh.getUserAgent());
        m.put("failureReason", lh.getFailureReason());
        m.put("loginAt", lh.getLoginAt());
        if (lh.getUser() != null) {
            m.put("userEmail", lh.getUser().getEmail());
            m.put("userName", lh.getUser().getFullName());
        }
        return m;
    }

    private Map<String, Object> toSecurityUserMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("fullName", u.getFullName());
        m.put("phone", u.getPhone());
        m.put("active", u.isActive());
        m.put("aadhaarVerified", u.isAadhaarVerified());
        m.put("accountLocked", u.isAccountLocked());
        m.put("consecutiveFailedLogins", u.getConsecutiveFailedLogins());
        m.put("lastLoginIp", u.getLastLoginIp());
        m.put("lastLoginAt", u.getLastLoginAt());
        m.put("riskScore", u.getRiskScore());
        m.put("trustRating", u.getTrustRating());
        return m;
    }
}
