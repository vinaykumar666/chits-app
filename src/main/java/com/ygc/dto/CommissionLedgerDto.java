package com.ygc.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CommissionLedgerDto {
    private Long id;
    private Long chitId;
    private String chitName;
    private String source;
    private java.time.LocalDate month;
    private BigDecimal commissionAmount;
    private BigDecimal commissionPercentage;
    private LocalDateTime createdAt;
}
