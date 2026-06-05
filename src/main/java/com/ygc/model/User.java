package com.ygc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
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

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean firstLogin = true;

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean active = true;

    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean termsAccepted = false;

    // ── Fraud Prevention Fields ────────────────────────────────────────────
    @Column(unique = true)
    private String aadhaarNumber;

    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean aadhaarVerified = false;

    @ColumnDefault("'en'")
    @Column(nullable = false)
    private String preferredLanguage = "en";

    @Column(columnDefinition = "TEXT")
    private String deviceFingerprints;

    private String lastLoginIp;
    private String lastLoginDevice;
    private LocalDateTime lastLoginAt;

    @ColumnDefault("0")
    @Column(nullable = false)
    private int consecutiveFailedLogins = 0;

    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean accountLocked = false;

    @ColumnDefault("0")
    @Column(nullable = false)
    private int riskScore = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    public enum Role { ADMIN, MEMBER }

    public String getMaskedAadhaar() {
        if (aadhaarNumber == null || aadhaarNumber.length() < 4) return "Not provided";
        return "XXXX-XXXX-" + aadhaarNumber.substring(aadhaarNumber.length() - 4);
    }
}
