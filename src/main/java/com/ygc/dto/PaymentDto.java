package com.ygc.dto;

import com.ygc.model.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentDto {
    private Long id;
    private Long membershipId;
    private String memberName;
    private String chitName;
    private BigDecimal amount;
    private BigDecimal lateFine;
    private BigDecimal totalAmount;
    private String adminRemarks;
    private String rejectionReason;
    private Payment.PaymentStatus status;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private LocalDateTime verifiedAt;
    private Integer monthNumber;
    private LocalDateTime createdAt;
}
