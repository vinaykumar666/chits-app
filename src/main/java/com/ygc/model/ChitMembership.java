package com.ygc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "chit_memberships")
@Data
@NoArgsConstructor
public class ChitMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chit_id", nullable = false)
    private Chit chit;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private MembershipStatus status = MembershipStatus.PENDING;

    private boolean hasWonAuction = false;
    private boolean termsAccepted = false;
    private String certificatePath;

    // Rejection reason (set by admin on any rejection)
    private String rejectionReason;

    // Agreement workflow fields
    private boolean agreementRead = false;
    private boolean agreementAccepted = false;
    private boolean infoProcessingAuthorized = false;
    private LocalDateTime agreementAcceptedAt;
    private LocalDateTime agreementApprovedAt;
    private String agreementNumber;
    private String agreementPdfPath;

    private LocalDateTime joinedAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum MembershipStatus {
        PENDING, ACTIVE, SETTLED, EXITED
    }
}
