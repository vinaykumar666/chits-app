package com.ygc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "early_exit_requests")
@Data
@NoArgsConstructor
public class EarlyExitRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "membership_id", nullable = false)
    private ChitMembership membership;

    @Enumerated(EnumType.STRING)
    private ExitStatus status = ExitStatus.REQUESTED;

    private String reason;

    /** Total amount member has paid so far */
    private BigDecimal totalPaid = BigDecimal.ZERO;

    /** Penalty = totalPaid * penaltyPercentage / 100 */
    private BigDecimal penaltyAmount = BigDecimal.ZERO;
    private BigDecimal penaltyPercentage = new BigDecimal("2");

    /** Dividends earned by not winning auctions */
    private BigDecimal dividendsEarned = BigDecimal.ZERO;

    /** Final refund = totalPaid - penaltyAmount + dividendsEarned */
    private BigDecimal refundAmount = BigDecimal.ZERO;

    private String adminRemarks;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    /** Replacement member (if any) who takes over the slot */
    @ManyToOne
    @JoinColumn(name = "replacement_member_id")
    private User replacementMember;

    private LocalDateTime requestedAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;
    private LocalDateTime settledAt;

    public enum ExitStatus {
        REQUESTED, UNDER_REVIEW, APPROVED, REJECTED, SETTLED
    }
}
