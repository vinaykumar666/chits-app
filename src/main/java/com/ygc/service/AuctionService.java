package com.ygc.service;

import com.ygc.audit.AuditService;
import com.ygc.exception.EntityNotFoundException;
import com.ygc.model.*;
import com.ygc.repository.*;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ChitMembershipRepository membershipRepository;
    private final CommissionLedgerRepository commissionLedgerRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final LoggingUtil loggingUtil;

    @Transactional
    public Auction createAuction(Long chitId, Integer monthNumber, LocalDate auctionDate,
                                 ChitRepository chitRepository, User admin) {
        loggingUtil.transactionStart("createAuction", "AuctionService");
        try {
            loggingUtil.debug("Creating auction for chit: " + chitId + ", month: " + monthNumber, "AuctionService.createAuction");

            Chit chit = chitRepository.findById(chitId)
                    .orElseThrow(() -> {
                        loggingUtil.warn("Chit not found: " + chitId, "AuctionService.createAuction");
                        return new EntityNotFoundException("Chit not found");
                    });

            Auction auction = new Auction();
            auction.setChit(chit);
            auction.setMonthNumber(monthNumber);
            auction.setAuctionDate(auctionDate);
            auction.setStatus(Auction.AuctionStatus.ANNOUNCED);

            loggingUtil.databaseOperation("INSERT", "Auction", "AuctionService.createAuction");
            Auction saved = auctionRepository.save(auction);

            List<ChitMembership> members = membershipRepository.findByChit(chit)
                    .stream().filter(m -> m.getStatus() == ChitMembership.MembershipStatus.ACTIVE).toList();

            loggingUtil.info("Sending auction announcements to " + members.size() + " members", "AuctionService.createAuction");
            for (ChitMembership m : members) {
                try {
                    emailService.sendAuctionAnnouncement(
                            m.getUser().getEmail(), m.getUser().getFullName(),
                            chit.getName(), auctionDate.toString(), monthNumber);
                } catch (Exception e) {
                    loggingUtil.error("Failed to send announcement to: " + m.getUser().getEmail(), "AuctionService.createAuction", e);
                }
            }

            auditService.log(admin, "CREATE_AUCTION", "Auction", saved.getId(),
                    "Auction announced for month " + monthNumber);
            loggingUtil.transactionComplete("createAuction", "AuctionService");
            loggingUtil.userAction(admin.getEmail(), "CREATE_AUCTION", "AuctionService.createAuction");

            return saved;
        } catch (Exception e) {
            loggingUtil.transactionFailed("createAuction", "AuctionService", e);
            throw e;
        }
    }

    @Transactional
    public void openAuction(Long auctionId, User admin) {
        loggingUtil.transactionStart("openAuction", "AuctionService");
        try {
            loggingUtil.debug("Opening auction: " + auctionId, "AuctionService.openAuction");

            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> {
                        loggingUtil.warn("Auction not found: " + auctionId, "AuctionService.openAuction");
                        return new EntityNotFoundException("Auction not found");
                    });

            auction.setStatus(Auction.AuctionStatus.OPEN);
            loggingUtil.databaseOperation("UPDATE", "Auction", "AuctionService.openAuction");
            auctionRepository.save(auction);

            auditService.log(admin, "OPEN_AUCTION", "Auction", auctionId, "Auction opened for bidding");
            loggingUtil.transactionComplete("openAuction", "AuctionService");
            loggingUtil.userAction(admin.getEmail(), "OPEN_AUCTION", "AuctionService.openAuction");

        } catch (Exception e) {
            loggingUtil.transactionFailed("openAuction", "AuctionService", e);
            throw e;
        }
    }

    @Transactional
    public Bid placeBid(Long auctionId, BigDecimal bidAmount, User bidder) {
        loggingUtil.transactionStart("placeBid", "AuctionService");
        try {
            loggingUtil.debug("Placing bid for auction: " + auctionId + ", amount: " + bidAmount, "AuctionService.placeBid");

            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> {
                        loggingUtil.warn("Auction not found: " + auctionId, "AuctionService.placeBid");
                        return new EntityNotFoundException("Auction not found");
                    });

            if (auction.getStatus() != Auction.AuctionStatus.OPEN) {
                loggingUtil.businessRuleViolation("AUCTION_NOT_OPEN", "AuctionService.placeBid",
                    "Status: " + auction.getStatus());
                throw new IllegalStateException("Auction is not open for bidding");
            }

            Chit chit = auction.getChit();
            if (chit.getMinBidAmount() != null && bidAmount.compareTo(chit.getMinBidAmount()) < 0) {
                loggingUtil.businessRuleViolation("BID_BELOW_MINIMUM", "AuctionService.placeBid",
                    "Bid: " + bidAmount + ", Min: " + chit.getMinBidAmount());
                throw new IllegalArgumentException("Bid below minimum: ₹" + chit.getMinBidAmount());
            }
            if (chit.getMaxBidAmount() != null && bidAmount.compareTo(chit.getMaxBidAmount()) > 0) {
                loggingUtil.businessRuleViolation("BID_ABOVE_MAXIMUM", "AuctionService.placeBid",
                    "Bid: " + bidAmount + ", Max: " + chit.getMaxBidAmount());
                throw new IllegalArgumentException("Bid above maximum: ₹" + chit.getMaxBidAmount());
            }

            ChitMembership membership = membershipRepository.findByChitAndUser(chit, bidder)
                    .orElseThrow(() -> {
                        loggingUtil.warn("Bidder not a member of chit: " + bidder.getEmail(), "AuctionService.placeBid");
                        return new EntityNotFoundException("You are not a member of this chit");
                    });

            if (membership.isHasWonAuction()) {
                loggingUtil.businessRuleViolation("ALREADY_WON", "AuctionService.placeBid",
                    "User: " + bidder.getEmail());
                throw new IllegalStateException("You have already won an auction in this chit");
            }

            Bid bid = bidRepository.findByAuctionAndBidder(auction, bidder).orElse(new Bid());
            bid.setAuction(auction);
            bid.setBidder(bidder);
            bid.setBidAmount(bidAmount);

            loggingUtil.databaseOperation("INSERT_OR_UPDATE", "Bid", "AuctionService.placeBid");
            Bid saved = bidRepository.save(bid);

            auditService.log(bidder, "PLACE_BID", "Bid", saved.getId(), "Bid placed: ₹" + bidAmount);
            loggingUtil.transactionComplete("placeBid", "AuctionService");
            loggingUtil.userAction(bidder.getEmail(), "PLACE_BID", "AuctionService.placeBid");

            return saved;
        } catch (Exception e) {
            loggingUtil.transactionFailed("placeBid", "AuctionService", e);
            throw e;
        }
    }

    @Transactional
    public void closeAuction(Long auctionId, User admin) {
        loggingUtil.transactionStart("closeAuction", "AuctionService");
        try {
            loggingUtil.debug("Closing auction: " + auctionId, "AuctionService.closeAuction");

            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> {
                        loggingUtil.warn("Auction not found: " + auctionId, "AuctionService.closeAuction");
                        return new EntityNotFoundException("Auction not found");
                    });

            List<Bid> bids = bidRepository.findByAuction(auction);
            if (bids.isEmpty()) {
                loggingUtil.businessRuleViolation("NO_BIDS", "AuctionService.closeAuction",
                    "Auction: " + auctionId);
                throw new IllegalStateException("No bids placed yet");
            }

            Bid winningBid = bids.stream()
                    .min(Comparator.comparing(Bid::getBidAmount))
                    .orElseThrow();

            loggingUtil.info("Winning bid: " + winningBid.getBidAmount() + " by " + winningBid.getBidder().getEmail(),
                "AuctionService.closeAuction");

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

            loggingUtil.databaseOperation("UPDATE", "Auction", "AuctionService.closeAuction");
            auctionRepository.save(auction);

            membershipRepository.findByChitAndUser(chit, winningBid.getBidder())
                    .ifPresent(m -> {
                        m.setHasWonAuction(true);
                        membershipRepository.save(m);
                        loggingUtil.info("Membership updated for winner", "AuctionService.closeAuction");
                    });

            CommissionLedger cl = new CommissionLedger();
            cl.setChit(chit);
            cl.setCommissionAmount(commission);
            cl.setCommissionPercentage(chit.getAdminCommissionPercentage());
            cl.setSource("AUCTION");
            cl.setMonth(LocalDate.now());

            loggingUtil.databaseOperation("INSERT", "CommissionLedger", "AuctionService.closeAuction");
            commissionLedgerRepository.save(cl);

            auditService.log(admin, "CLOSE_AUCTION", "Auction", auctionId,
                    "Winner: " + winningBid.getBidder().getEmail() + " | Payout: ₹" + payout);
            loggingUtil.transactionComplete("closeAuction", "AuctionService");
            loggingUtil.userAction(admin.getEmail(), "CLOSE_AUCTION", "AuctionService.closeAuction");

        } catch (Exception e) {
            loggingUtil.transactionFailed("closeAuction", "AuctionService", e);
            throw e;
        }
    }

    @Transactional
    public void releasePayout(Long auctionId, User admin) {
        loggingUtil.transactionStart("releasePayout", "AuctionService");
        try {
            loggingUtil.debug("Releasing payout for auction: " + auctionId, "AuctionService.releasePayout");

            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> {
                        loggingUtil.warn("Auction not found: " + auctionId, "AuctionService.releasePayout");
                        return new EntityNotFoundException("Auction not found");
                    });

            auction.setPayoutReleased(true);
            auction.setStatus(Auction.AuctionStatus.COMPLETED);

            loggingUtil.databaseOperation("UPDATE", "Auction", "AuctionService.releasePayout");
            auctionRepository.save(auction);

            auditService.log(admin, "RELEASE_PAYOUT", "Auction", auctionId,
                    "Payout released: ₹" + auction.getLumpSumPayout());
            loggingUtil.transactionComplete("releasePayout", "AuctionService");
            loggingUtil.userAction(admin.getEmail(), "RELEASE_PAYOUT", "AuctionService.releasePayout");

        } catch (Exception e) {
            loggingUtil.transactionFailed("releasePayout", "AuctionService", e);
            throw e;
        }
    }

    public List<Auction> getAuctionsByChit(Chit chit) {
        try {
            loggingUtil.debug("Fetching auctions for chit: " + chit.getId(), "AuctionService.getAuctionsByChit");
            loggingUtil.databaseOperation("SELECT", "Auction", "AuctionService.getAuctionsByChit");

            List<Auction> auctions = auctionRepository.findByChit(chit);
            loggingUtil.info("Retrieved " + auctions.size() + " auctions", "AuctionService.getAuctionsByChit");

            return auctions;
        } catch (Exception e) {
            loggingUtil.error("Error fetching auctions", "AuctionService.getAuctionsByChit", e);
            throw e;
        }
    }

    public List<Auction> getOpenAuctions() {
        try {
            loggingUtil.debug("Fetching open auctions", "AuctionService.getOpenAuctions");
            loggingUtil.databaseOperation("SELECT", "Auction", "AuctionService.getOpenAuctions");

            List<Auction> auctions = auctionRepository.findByStatus(Auction.AuctionStatus.OPEN);
            loggingUtil.info("Retrieved " + auctions.size() + " open auctions", "AuctionService.getOpenAuctions");

            return auctions;
        } catch (Exception e) {
            loggingUtil.error("Error fetching open auctions", "AuctionService.getOpenAuctions", e);
            throw e;
        }
    }

    public List<Auction> getAllAuctions() {
        try {
            loggingUtil.debug("Fetching all auctions", "AuctionService.getAllAuctions");
            loggingUtil.databaseOperation("SELECT", "Auction", "AuctionService.getAllAuctions");

            List<Auction> auctions = auctionRepository.findAll();
            loggingUtil.info("Retrieved " + auctions.size() + " total auctions", "AuctionService.getAllAuctions");

            return auctions;
        } catch (Exception e) {
            loggingUtil.error("Error fetching all auctions", "AuctionService.getAllAuctions", e);
            throw e;
        }
    }
}

