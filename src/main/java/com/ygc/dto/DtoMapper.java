package com.ygc.dto;

import com.ygc.model.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DtoMapper {

    public UserDto toUserDto(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .firstLogin(user.isFirstLogin())
                .active(user.isActive())
                .termsAccepted(user.isTermsAccepted())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public ChitDto toChitDto(Chit chit) {
        return toChitDto(chit, null);
    }

    public ChitDto toChitDto(Chit chit, Integer memberCount) {
        if (chit == null) return null;
        return ChitDto.builder()
                .id(chit.getId())
                .name(chit.getName())
                .description(chit.getDescription())
                .monthlyAmount(chit.getMonthlyAmount())
                .totalMembers(chit.getTotalMembers())
                .durationMonths(chit.getDurationMonths())
                .totalChitValue(chit.getTotalChitValue())
                .adminCommissionPercentage(chit.getAdminCommissionPercentage())
                .minBidAmount(chit.getMinBidAmount())
                .maxBidAmount(chit.getMaxBidAmount())
                .startDate(chit.getStartDate())
                .endDate(chit.getEndDate())
                .status(chit.getStatus())
                .closingReason(chit.getClosingReason())
                .createdAt(chit.getCreatedAt())
                .memberCount(memberCount)
                .build();
    }

    public ChitMembershipDto toMembershipDto(ChitMembership m) {
        if (m == null) return null;
        return ChitMembershipDto.builder()
                .id(m.getId())
                .chit(toChitDto(m.getChit()))
                .user(toUserDto(m.getUser()))
                .status(m.getStatus())
                .hasWonAuction(m.isHasWonAuction())
                .termsAccepted(m.isTermsAccepted())
                .rejectionReason(m.getRejectionReason())
                .joinReason(m.getJoinReason())
                .agreementRead(m.isAgreementRead())
                .agreementAccepted(m.isAgreementAccepted())
                .infoProcessingAuthorized(m.isInfoProcessingAuthorized())
                .agreementAcceptedAt(m.getAgreementAcceptedAt())
                .agreementApprovedAt(m.getAgreementApprovedAt())
                .agreementNumber(m.getAgreementNumber())
                .joinedAt(m.getJoinedAt())
                .build();
    }

    public PaymentDto toPaymentDto(Payment p) {
        if (p == null) return null;
        ChitMembership membership = p.getMembership();
        return PaymentDto.builder()
                .id(p.getId())
                .membershipId(membership != null ? membership.getId() : null)
                .memberName(membership != null && membership.getUser() != null ? membership.getUser().getFullName() : null)
                .chitName(membership != null && membership.getChit() != null ? membership.getChit().getName() : null)
                .amount(p.getAmount())
                .lateFine(p.getLateFine())
                .totalAmount(p.getTotalAmount())
                .adminRemarks(p.getAdminRemarks())
                .rejectionReason(p.getRejectionReason())
                .status(p.getStatus())
                .dueDate(p.getDueDate())
                .paidDate(p.getPaidDate())
                .verifiedAt(p.getVerifiedAt())
                .monthNumber(p.getMonthNumber())
                .createdAt(p.getCreatedAt())
                .build();
    }

    public AuctionDto toAuctionDto(Auction a) {
        if (a == null) return null;
        Chit chit = a.getChit();
        return AuctionDto.builder()
                .id(a.getId())
                .chitId(chit != null ? chit.getId() : null)
                .chitName(chit != null ? chit.getName() : null)
                .monthNumber(a.getMonthNumber())
                .auctionDate(a.getAuctionDate())
                .status(a.getStatus())
                .winner(toUserDto(a.getWinner()))
                .winningBidAmount(a.getWinningBidAmount())
                .lumpSumPayout(a.getLumpSumPayout())
                .adminCommission(a.getAdminCommission())
                .payoutReleased(a.isPayoutReleased())
                .createdAt(a.getCreatedAt())
                .build();
    }

    public SettlementDto toSettlementDto(Settlement s) {
        if (s == null) return null;
        ChitMembership m = s.getMembership();
        return SettlementDto.builder()
                .id(s.getId())
                .membershipId(m != null ? m.getId() : null)
                .memberName(m != null && m.getUser() != null ? m.getUser().getFullName() : null)
                .chitName(m != null && m.getChit() != null ? m.getChit().getName() : null)
                .totalPaidAmount(s.getTotalPaidAmount())
                .pendingDues(s.getPendingDues())
                .lateFines(s.getLateFines())
                .deductionAmount(s.getDeductionAmount())
                .deductionPercentage(s.getDeductionPercentage())
                .finalSettlementAmount(s.getFinalSettlementAmount())
                .profitShare(s.getProfitShare())
                .type(s.getType())
                .status(s.getStatus())
                .adminRemarks(s.getAdminRemarks())
                .requestedAt(s.getRequestedAt())
                .processedAt(s.getProcessedAt())
                .build();
    }

    public AuditLogDto toAuditLogDto(AuditLog log) {
        if (log == null) return null;
        User user = log.getUser();
        return AuditLogDto.builder()
                .id(log.getId())
                .userEmail(user != null ? user.getEmail() : null)
                .userName(user != null ? user.getFullName() : null)
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .timestamp(log.getTimestamp())
                .build();
    }

    public CommissionLedgerDto toCommissionLedgerDto(CommissionLedger entry) {
        if (entry == null) return null;
        Chit chit = entry.getChit();
        return CommissionLedgerDto.builder()
                .id(entry.getId())
                .chitId(chit != null ? chit.getId() : null)
                .chitName(chit != null ? chit.getName() : null)
                .source(entry.getSource())
                .month(entry.getMonth())
                .commissionAmount(entry.getCommissionAmount())
                .commissionPercentage(entry.getCommissionPercentage())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    public ChitHistoryDto toChitHistoryDto(ChitHistory h) {
        if (h == null) return null;
        return ChitHistoryDto.builder()
                .id(h.getId())
                .chitName(h.getChitName())
                .finalStatus(h.getFinalStatus())
                .closingReason(h.getClosingReason())
                .closedAt(h.getClosedAt())
                .analysisPdfPath(h.getAnalysisPdfPath())
                .build();
    }

    public List<UserDto> toUserDtos(List<User> users) {
        return users.stream().map(this::toUserDto).toList();
    }

    public List<ChitDto> toChitDtos(List<Chit> chits) {
        return chits.stream().map(this::toChitDto).toList();
    }

    public List<ChitMembershipDto> toMembershipDtos(List<ChitMembership> memberships) {
        return memberships.stream().map(this::toMembershipDto).toList();
    }

    public List<PaymentDto> toPaymentDtos(List<Payment> payments) {
        return payments.stream().map(this::toPaymentDto).toList();
    }

    public List<AuctionDto> toAuctionDtos(List<Auction> auctions) {
        return auctions.stream().map(this::toAuctionDto).toList();
    }

    public List<SettlementDto> toSettlementDtos(List<Settlement> settlements) {
        return settlements.stream().map(this::toSettlementDto).toList();
    }

    public List<AuditLogDto> toAuditLogDtos(List<AuditLog> logs) {
        return logs.stream().map(this::toAuditLogDto).toList();
    }

    public List<CommissionLedgerDto> toCommissionLedgerDtos(List<CommissionLedger> entries) {
        return entries.stream().map(this::toCommissionLedgerDto).toList();
    }

    public List<ChitHistoryDto> toChitHistoryDtos(List<ChitHistory> histories) {
        return histories.stream().map(this::toChitHistoryDto).toList();
    }
}
