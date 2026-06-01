package com.ygc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "membership_id", nullable = false)
    private ChitMembership membership;

    @Column(nullable = false)
    private BigDecimal amount;

    private BigDecimal lateFine = BigDecimal.ZERO;
    private BigDecimal totalAmount;

    private String screenshotPath;
    private String adminRemarks;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    private LocalDate dueDate;
    private LocalDate paidDate;
    private LocalDateTime verifiedAt;

    @ManyToOne
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    private Integer monthNumber;

    public enum PaymentStatus {
        PENDING, APPROVED, REJECTED, OVERDUE
    }
}