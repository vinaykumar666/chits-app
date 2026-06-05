package com.ygc.security;

import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import com.ygc.service.LoginTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventListener {

    private final LoginTrackingService loginTrackingService;
    private final UserRepository userRepository;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        try {
            String email = event.getAuthentication().getName();
            userRepository.findByEmail(email).ifPresent(user -> {
                String[] ipUa = getIpAndUa();
                loginTrackingService.recordLogin(user, ipUa[0], ipUa[1], true, null);
            });
        } catch (Exception e) { log.debug("Login tracking error: {}", e.getMessage()); }
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        try {
            String email = event.getAuthentication().getName().toString();
            userRepository.findByEmail(email).ifPresent(user -> {
                String[] ipUa = getIpAndUa();
                loginTrackingService.recordLogin(user, ipUa[0], ipUa[1], false, "Bad credentials");
            });
        } catch (Exception e) { log.debug("Login failure tracking error: {}", e.getMessage()); }
    }

    private String[] getIpAndUa() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String xff = req.getHeader("X-Forwarded-For");
                String ip = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
                return new String[]{ip, req.getHeader("User-Agent")};
            }
        } catch (Exception ignored) {}
        return new String[]{"UNKNOWN", "UNKNOWN"};
    }
}
