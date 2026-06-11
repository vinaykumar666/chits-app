package com.ygc.dto;

import com.ygc.model.Settlement;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SettlementDto {
    private Long id;
    private Long membershipId;
    private String memberName;
    private String chitName;
    private BigDecimal totalPaidAmount;
    private BigDecimal pendingDues;
    private BigDecimal lateFines;
    private BigDecimal deductionAmount;
    private BigDecimal deductionPercentage;
    private BigDecimal finalSettlementAmount;
    private BigDecimal profitShare;
    private Settlement.SettlementType type;
    private Settlement.SettlementStatus status;
    private String adminRemarks;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
}
