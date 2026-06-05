package com.ygc.repository;

import com.ygc.model.Auction;
import com.ygc.model.Chit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findByChit(Chit chit);
    Optional<Auction> findByChitAndMonthNumber(Chit chit, Integer monthNumber);
    List<Auction> findByStatus(Auction.AuctionStatus status);

    /**
     * FIX: Eager-fetch bids, chit, and winner in one query.
     * Prevents LazyInitializationException when open-in-view=false.
     */
    @Query("SELECT DISTINCT a FROM Auction a " +
           "LEFT JOIN FETCH a.bids " +
           "LEFT JOIN FETCH a.chit " +
           "LEFT JOIN FETCH a.winner " +
           "ORDER BY a.createdAt DESC")
    List<Auction> findAllWithBidsAndChit();

    @Query("SELECT DISTINCT a FROM Auction a " +
           "LEFT JOIN FETCH a.bids " +
           "LEFT JOIN FETCH a.chit " +
           "LEFT JOIN FETCH a.winner " +
           "WHERE a.status = :status")
    List<Auction> findByStatusWithBidsAndChit(Auction.AuctionStatus status);
}
