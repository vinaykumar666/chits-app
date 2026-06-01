package com.ygc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_ledger")
@Data
@NoArgsConstructor
public class CommissionLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chit_id", nullable = false)
    private Chit chit;

    private BigDecimal commissionAmount;
    private BigDecimal commissionPercentage;
    private String source; // MONTHLY_COLLECTION or AUCTION
    @Column(name = "ledger_month")
    private LocalDate month;

    private LocalDateTime createdAt = LocalDateTime.now();
}