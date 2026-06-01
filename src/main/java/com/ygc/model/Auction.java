package com.ygc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "auctions")
@Data
@NoArgsConstructor
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chit_id", nullable = false)
    private Chit chit;

    private Integer monthNumber;
    private LocalDate auctionDate;

    @Enumerated(EnumType.STRING)
    private AuctionStatus status = AuctionStatus.ANNOUNCED;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    private BigDecimal winningBidAmount;
    private BigDecimal lumpSumPayout;
    private BigDecimal adminCommission;
    private boolean payoutReleased = false;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL)
    private List<Bid> bids;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum AuctionStatus {
        ANNOUNCED, OPEN, CLOSED, COMPLETED
    }
}