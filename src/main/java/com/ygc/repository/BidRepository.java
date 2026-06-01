package com.ygc.repository;

import com.ygc.model.Bid;
import com.ygc.model.Auction;
import com.ygc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByAuction(Auction auction);
    Optional<Bid> findByAuctionAndBidder(Auction auction, User bidder);
    Optional<Bid> findByAuctionAndIsWinning(Auction auction, boolean isWinning);
}