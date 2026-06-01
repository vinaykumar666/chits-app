package com.ygc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
@Data
@NoArgsConstructor
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "membership_id", nullable = false)
    private ChitMembership membership;

    private BigDecimal totalPaidAmount;
    private BigDecimal pendingDues;
    private BigDecimal lateFines;
    private BigDecimal deductionAmount;
    private BigDecimal deductionPercentage;
    private BigDecimal finalSettlementAmount;
    private BigDecimal profitShare;

    @Enumerated(EnumType.STRING)
    private SettlementType type;

    @Enumerated(EnumType.STRING)
    private SettlementStatus status = SettlementStatus.PENDING;

    private String adminRemarks;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime requestedAt = LocalDateTime.now();
    private LocalDateTime processedAt;

    public enum SettlementType {
        EARLY_EXIT, MATURITY
    }

    public enum SettlementStatus {
        PENDING, APPROVED, REJECTED, COMPLETED
    }
}