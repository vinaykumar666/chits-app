package com.ygc.dto;

import com.ygc.model.Auction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AuctionDto {
    private Long id;
    private Long chitId;
    private String chitName;
    private Integer monthNumber;
    private LocalDate auctionDate;
    private Auction.AuctionStatus status;
    private UserDto winner;
    private BigDecimal winningBidAmount;
    private BigDecimal lumpSumPayout;
    private BigDecimal adminCommission;
    private boolean payoutReleased;
    private LocalDateTime createdAt;
}
