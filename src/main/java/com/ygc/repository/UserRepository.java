package com.ygc.repository;

import com.ygc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // Fraud detection: duplicate Aadhaar / phone
    java.util.List<User> findByAadhaarNumber(String aadhaarNumber);
    java.util.List<User> findByPhone(String phone);
    long countByAadhaarNumberAndIdNot(String aadhaarNumber, Long excludeId);
    long countByPhoneAndIdNot(String phone, Long excludeId);

    // Password expiry
    java.util.List<User> findByPasswordExpiresAtBeforeAndActiveTrue(java.time.LocalDateTime now);

    // Trust rating
    java.util.List<User> findByRoleAndActiveTrue(User.Role role);
}