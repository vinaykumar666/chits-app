package com.ygc.repository;

import com.ygc.model.Auction;
import com.ygc.model.Chit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findByChit(Chit chit);
    Optional<Auction> findByChitAndMonthNumber(Chit chit, Integer monthNumber);
    List<Auction> findByStatus(Auction.AuctionStatus status);
}