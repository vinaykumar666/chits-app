package com.ygc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chits")
@Data
@NoArgsConstructor
public class Chit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal monthlyAmount;

    @Column(nullable = false)
    private Integer totalMembers;

    @Column(nullable = false)
    private Integer durationMonths;

    private BigDecimal totalChitValue;

    @Column(nullable = false)
    private BigDecimal adminCommissionPercentage;

    private BigDecimal minBidAmount;
    private BigDecimal maxBidAmount;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ChitStatus status = ChitStatus.OPEN;

    private String qrCodePath;

    // Set by admin when chit is CANCELLED or COMPLETED with a reason
    private String closingReason;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "chit", cascade = CascadeType.ALL)
    private List<ChitMembership> memberships;

    @OneToMany(mappedBy = "chit", cascade = CascadeType.ALL)
    private List<Auction> auctions;

    public enum ChitStatus {
        OPEN, ACTIVE, COMPLETED, CANCELLED
    }
}