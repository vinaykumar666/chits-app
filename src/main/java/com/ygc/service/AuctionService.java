package com.ygc.service;

import com.ygc.audit.AuditService;
import com.ygc.model.*;
import com.ygc.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ChitMembershipRepository membershipRepository;
    private final CommissionLedgerRepository commissionLedgerRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional
    public Auction createAuction(Long chitId, Integer monthNumber, LocalDate auctionDate,
                                 ChitRepository chitRepository, User admin) {
        Chit chit = chitRepository.findById(chitId)
                .orElseThrow(() -> new IllegalArgumentException("Chit not found"));

        Auction auction = new Auction();
        auction.setChit(chit);
        auction.setMonthNumber(monthNumber);
        auction.setAuctionDate(auctionDate);
        auction.setStatus(Auction.AuctionStatus.ANNOUNCED);
        Auction saved = auctionRepository.save(auction);

        List<ChitMembership> members = membershipRepository.findByChit(chit)
                .stream().filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE).toList();
        for (ChitMembership m : members) {
            emailService.sendAuctionAnnouncement(
                    m.getUser().getEmail(), m.getUser().getFullName(),
                    chit.getName(), auctionDate.toString(), monthNumber);
        }
        auditService.log(admin, "CREATE_AUCTION", "Auction", saved.getId(),
                "Auction announced for month " + monthNumber);
        return saved;
    }

    @Transactional
    public void openAuction(Long auctionId, User admin) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));
        auction.setStatus(Auction.AuctionStatus.OPEN);
        auctionRepository.save(auction);
        auditService.log(admin, "OPEN_AUCTION", "Auction", auctionId, "Auction opened for bidding");
    }

    @Transactional
    public Bid placeBid(Long auctionId, BigDecimal bidAmount, User bidder) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

        if (auction.getStatus() != Auction.AuctionStatus.OPEN) {
            throw new IllegalStateException("Auction is not open for bidding");
        }

        Chit chit = auction.getChit();
        if (chit.getMinBidAmount() != null && bidAmount.compareTo(chit.getMinBidAmount()) < 0) {
            throw new IllegalArgumentException("Bid below minimum: \u20b9" + chit.getMinBidAmount());
        }
        if (chit.getMaxBidAmount() != null && bidAmount.compareTo(chit.getMaxBidAmount()) > 0) {
            throw new IllegalArgumentException("Bid above maximum: \u20b9" + chit.getMaxBidAmount());
        }

        ChitMembership membership = membershipRepository.findByChitAndUser(chit, bidder)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this chit"));
        if (membership.isHasWonAuction()) {
            throw new IllegalStateException("You have already won an auction in this chit");
        }

        Bid bid = bidRepository.findByAuctionAndBidder(auction, bidder).orElse(new Bid());
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setBidAmount(bidAmount);
        Bid saved = bidRepository.save(bid);
        auditService.log(bidder, "PLACE_BID", "Bid", saved.getId(), "Bid placed: \u20b9" + bidAmount);
        return saved;
    }

    @Transactional
    public void closeAuction(Long auctionId, User admin) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

        List<Bid> bids = bidRepository.findByAuction(auction);
        if (bids.isEmpty()) {
            throw new IllegalStateException("No bids placed yet");
        }

        Bid winningBid = bids.stream()
                .min(Comparator.comparing(Bid::getBidAmount))
                .orElseThrow();

        winningBid.setWinning(true);
        bidRepository.save(winningBid);

        Chit chit = auction.getChit();
        BigDecimal commission = winningBid.getBidAmount()
                .multiply(chit.getAdminCommissionPercentage())
                .divide(BigDecimal.valueOf(100));
        BigDecimal payout = winningBid.getBidAmount().subtract(commission);

        auction.setWinner(winningBid.getBidder());
        auction.setWinningBidAmount(winningBid.getBidAmount());
        auction.setLumpSumPayout(payout);
        auction.setAdminCommission(commission);
        auction.setStatus(Auction.AuctionStatus.CLOSED);
        auctionRepository.save(auction);

        membershipRepository.findByChitAndUser(chit, winningBid.getBidder())
                .ifPresent(m -> { m.setHasWonAuction(true); membershipRepository.save(m); });

        CommissionLedger cl = new CommissionLedger();
        cl.setChit(chit);
        cl.setCommissionAmount(commission);
        cl.setCommissionPercentage(chit.getAdminCommissionPercentage());
        cl.setSource("AUCTION");
        cl.setMonth(LocalDate.now());
        commissionLedgerRepository.save(cl);

        auditService.log(admin, "CLOSE_AUCTION", "Auction", auctionId,
                "Winner: " + winningBid.getBidder().getEmail() + " | Payout: \u20b9" + payout);
    }

    @Transactional
    public void releasePayout(Long auctionId, User admin) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));
        auction.setPayoutReleased(true);
        auction.setStatus(Auction.AuctionStatus.COMPLETED);
        auctionRepository.save(auction);
        auditService.log(admin, "RELEASE_PAYOUT", "Auction", auctionId,
                "Payout released: \u20b9" + auction.getLumpSumPayout());
    }

    public List<Auction> getAuctionsByChit(Chit chit) {
        return auctionRepository.findByChit(chit);
    }

    public List<Auction> getOpenAuctions() {
        return auctionRepository.findByStatus(Auction.AuctionStatus.OPEN);
    }

    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }
}
