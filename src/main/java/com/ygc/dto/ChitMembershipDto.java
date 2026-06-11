package com.ygc.dto;

import com.ygc.model.ChitMembership;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChitMembershipDto {
    private Long id;
    private ChitDto chit;
    private UserDto user;
    private ChitMembership.MembershipStatus status;
    private boolean hasWonAuction;
    private boolean termsAccepted;
    private String rejectionReason;
    private boolean agreementRead;
    private boolean agreementAccepted;
    private boolean infoProcessingAuthorized;
    private LocalDateTime agreementAcceptedAt;
    private LocalDateTime agreementApprovedAt;
    private String agreementNumber;
    private LocalDateTime joinedAt;
}
