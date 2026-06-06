package com.ygc.config;

import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "medipalli.vinaykumar@gmail.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setFullName("Vinay Kumar Medipalli");
            admin.setPhone("+918919508889");
            admin.setAddress("Hyderabad, Telangana");
            admin.setRole(User.Role.ADMIN);
            admin.setFirstLogin(false);
            admin.setActive(true);
            admin.setTermsAccepted(true);
            admin.setAadhaarVerified(false);
            admin.setPreferredLanguage("en");
            admin.setConsecutiveFailedLogins(0);
            admin.setAccountLocked(false);
            admin.setRiskScore(0);
            userRepository.save(admin);
            log.info("✅ Default admin created: {}", adminEmail);
        } else {
            log.info("Admin already exists: {}", adminEmail);
        }
    }
}
