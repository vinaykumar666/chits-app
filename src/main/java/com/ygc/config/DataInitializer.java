package com.ygc.config;

import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${spring.mail.username)}")
    private String defaultAdminEmail;
    @Value("${spring.mail.password)}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("medipalli.vinaykumar@gmail.com")) {
            User admin = new User();
            admin.setEmail("medipalli.vinaykumar@gmail.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setFullName("YGC Administrator");
            admin.setPhone("8919508889");
            admin.setRole(User.Role.ADMIN);
            admin.setFirstLogin(false);
            admin.setActive(true);
            userRepository.save(admin);
            System.out.println("==============================================");
            System.out.println("Default Admin Created:");
            System.out.println("Email: medipalli.vinaykumar@gmail.com");
            System.out.println("Password: Admin@123");
            System.out.println("==============================================");
            System.out.println("username: " + defaultAdminEmail);
            System.out.println("password: " + defaultAdminPassword);
        }
    }
}