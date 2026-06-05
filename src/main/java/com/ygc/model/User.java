package com.ygc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    private String phone;
    private String address;

    @Enumerated(EnumType.STRING)
    private Role role = Role.MEMBER;

    private boolean firstLogin = true;
    private boolean active = true;
    private boolean termsAccepted = false;

    // ── Fraud Prevention Fields ────────────────────────────────────────────
    @Column(unique = true)
    private String aadhaarNumber; // masked: XXXX-XXXX-1234

    private boolean aadhaarVerified = false;

    private String preferredLanguage = "en"; // en, hi, te, ta, kn, ml

    @Column(columnDefinition = "CLOB")
    private String deviceFingerprints; // JSON array of known device hashes

    private String lastLoginIp;
    private String lastLoginDevice;
    private LocalDateTime lastLoginAt;
    private int consecutiveFailedLogins = 0;
    private boolean accountLocked = false;

    /** Risk score 0-100: 0=safe, 100=high risk. Updated by RiskScoreService. */
    private int riskScore = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    public enum Role { ADMIN, MEMBER }

    /** Returns masked Aadhaar for display: XXXX-XXXX-1234 */
    public String getMaskedAadhaar() {
        if (aadhaarNumber == null || aadhaarNumber.length() < 4) return "Not provided";
        return "XXXX-XXXX-" + aadhaarNumber.substring(aadhaarNumber.length() - 4);
    }
}
