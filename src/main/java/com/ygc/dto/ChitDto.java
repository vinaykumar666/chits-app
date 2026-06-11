package com.ygc.dto;

import com.ygc.model.Chit;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ChitDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal monthlyAmount;
    private Integer totalMembers;
    private Integer durationMonths;
    private BigDecimal totalChitValue;
    private BigDecimal adminCommissionPercentage;
    private BigDecimal minBidAmount;
    private BigDecimal maxBidAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Chit.ChitStatus status;
    private String closingReason;
    private LocalDateTime createdAt;
    private Integer memberCount;
}
