package com.ygc.service;

import com.ygc.model.LoginHistory;
import com.ygc.model.User;
import com.ygc.repository.LoginHistoryRepository;
import com.ygc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginTrackingService {

    private final LoginHistoryRepository loginHistoryRepo;
    private final UserRepository userRepo;

    public void recordLogin(User user, String ip, String userAgent, boolean success, String failReason) {
        LoginHistory lh = new LoginHistory(user, ip, userAgent, success);
        lh.setFailureReason(failReason);
        loginHistoryRepo.save(lh);

        if (success) {
            user.setLastLoginIp(ip);
            user.setLastLoginDevice(extractDevice(userAgent));
            user.setLastLoginAt(LocalDateTime.now());
            user.setConsecutiveFailedLogins(0);
            userRepo.save(user);
        } else {
            user.setConsecutiveFailedLogins(user.getConsecutiveFailedLogins() + 1);
            if (user.getConsecutiveFailedLogins() >= 5) {
                user.setAccountLocked(true);
                log.warn("Account locked for user {} after {} failed attempts", user.getEmail(), user.getConsecutiveFailedLogins());
            }
            userRepo.save(user);
        }
    }

    /** Detect if same IP used by different users (possible fraud) */
    public List<LoginHistory> detectSharedIp(User user) {
        if (user.getLastLoginIp() == null) return List.of();
        return loginHistoryRepo.findByIpAddressAndUserNot(user.getLastLoginIp(), user);
    }

    public List<LoginHistory> getRecentLogins() {
        return loginHistoryRepo.findTop50ByOrderByLoginAtDesc();
    }

    public List<LoginHistory> getUserLogins(User user) {
        return loginHistoryRepo.findTop20ByUserOrderByLoginAtDesc(user);
    }

    private String extractDevice(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("iPhone")) return "iPhone";
        if (ua.contains("iPad")) return "iPad";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac")) return "Mac";
        if (ua.contains("Linux")) return "Linux";
        return "Other";
    }
}
