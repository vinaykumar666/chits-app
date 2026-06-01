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