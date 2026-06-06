package com.ygc.service;

import com.ygc.audit.AuditService;
import com.ygc.exception.DuplicateResourceException;
import com.ygc.exception.EntityNotFoundException;
import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;
    private final LoggingUtil loggingUtil;

    @Transactional
    public User registerUser(String email, String fullName, String phone, String address) {
        return registerUser(email, fullName, phone, address, null);
    }

    public User registerUser(String email, String fullName, String phone, String address, String aadhaarNumber) {
        loggingUtil.transactionStart("registerUser", "UserService");
        try {
            loggingUtil.debug("Registering user with email: " + email, "UserService.registerUser");

            if (userRepository.existsByEmail(email)) {
                loggingUtil.businessRuleViolation("DUPLICATE_EMAIL", "UserService.registerUser",
                    "Email already registered: " + email);
                throw new DuplicateResourceException("Email already registered: " + email);
            }

            String tempPassword = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            User user = new User();
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setAddress(address);
            if (aadhaarNumber != null && !aadhaarNumber.isBlank()) {
                user.setAadhaarNumber(aadhaarNumber.replaceAll("\\s", ""));
            }
            user.setPassword(passwordEncoder.encode(tempPassword));
            user.setFirstLogin(true);

            loggingUtil.databaseOperation("INSERT", "User", "UserService.registerUser");
            User saved = userRepository.save(user);

            try {
                emailService.sendRegistrationConfirmation(email, fullName, tempPassword);
            } catch (Exception e) {
                loggingUtil.error("Failed to send registration email", "UserService.registerUser", e);
                // Non-critical, continue with registration
            }

            auditService.log(saved, "REGISTER", "User", saved.getId(), "User registered: " + email);
            loggingUtil.transactionComplete("registerUser", "UserService");
            loggingUtil.userAction(email, "REGISTRATION", "UserService.registerUser");

            return saved;
        } catch (Exception e) {
            loggingUtil.transactionFailed("registerUser", "UserService", e);
            throw e;
        }
    }

    @Transactional
    public void changePassword(User user, String newPassword) {
        loggingUtil.transactionStart("changePassword", "UserService");
        try {
            loggingUtil.debug("Changing password for user: " + user.getEmail(), "UserService.changePassword");

            user.setPassword(passwordEncoder.encode(newPassword));
            user.setFirstLogin(false);

            loggingUtil.databaseOperation("UPDATE", "User", "UserService.changePassword");
            userRepository.save(user);

            auditService.log(user, "CHANGE_PASSWORD", "User", user.getId(), "Password changed");
            loggingUtil.transactionComplete("changePassword", "UserService");
            loggingUtil.userAction(user.getEmail(), "CHANGE_PASSWORD", "UserService.changePassword");

        } catch (Exception e) {
            loggingUtil.transactionFailed("changePassword", "UserService", e);
            throw e;
        }
    }

    public User findByEmail(String email) {
        try {
            loggingUtil.debug("Finding user by email: " + email, "UserService.findByEmail");
            loggingUtil.databaseOperation("SELECT", "User", "UserService.findByEmail");

            return userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        loggingUtil.warn("User not found for email: " + email, "UserService.findByEmail");
                        return new EntityNotFoundException("User not found: " + email);
                    });
        } catch (Exception e) {
            loggingUtil.error("Error finding user by email", "UserService.findByEmail", e);
            throw e;
        }
    }

    public User findById(Long id) {
        try {
            loggingUtil.debug("Finding user by id: " + id, "UserService.findById");
            loggingUtil.databaseOperation("SELECT", "User", "UserService.findById");

            return userRepository.findById(id)
                    .orElseThrow(() -> {
                        loggingUtil.warn("User not found for id: " + id, "UserService.findById");
                        return new EntityNotFoundException("User not found: " + id);
                    });
        } catch (Exception e) {
            loggingUtil.error("Error finding user by id", "UserService.findById", e);
            throw e;
        }
    }

    public List<User> findAllMembers() {
        try {
            loggingUtil.debug("Fetching all members", "UserService.findAllMembers");
            loggingUtil.databaseOperation("SELECT", "User", "UserService.findAllMembers");

            List<User> members = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.Role.MEMBER)
                    .toList();

            loggingUtil.info("Retrieved " + members.size() + " members", "UserService.findAllMembers");
            return members;
        } catch (Exception e) {
            loggingUtil.error("Error fetching all members", "UserService.findAllMembers", e);
            throw e;
        }
    }
}