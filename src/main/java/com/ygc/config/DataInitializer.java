package com.ygc.config;

import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@ygc.com")) {
            User admin = new User();
            admin.setEmail("admin@ygc.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setFullName("YGC Administrator");
            admin.setPhone("9999999999");
            admin.setRole(User.Role.ADMIN);
            admin.setFirstLogin(false);
            admin.setActive(true);
            userRepository.save(admin);
            System.out.println("==============================================");
            System.out.println("Default Admin Created:");
            System.out.println("Email: admin@ygc.com");
            System.out.println("Password: Admin@123");
            System.out.println("==============================================");
        }
    }
}