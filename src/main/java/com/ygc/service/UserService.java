package com.ygc.service;

import com.ygc.audit.AuditService;
import com.ygc.model.User;
import com.ygc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional
    public User registerUser(String email, String fullName, String phone, String address) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }
        String tempPassword = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address);
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setFirstLogin(true);
        User saved = userRepository.save(user);
        emailService.sendRegistrationConfirmation(email, fullName, tempPassword);
        auditService.log(saved, "REGISTER", "User", saved.getId(), "User registered: " + email);
        return saved;
    }

    @Transactional
    public void changePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFirstLogin(false);
        userRepository.save(user);
        auditService.log(user, "CHANGE_PASSWORD", "User", user.getId(), "Password changed");
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public List<User> findAllMembers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.MEMBER)
                .toList();
    }
}