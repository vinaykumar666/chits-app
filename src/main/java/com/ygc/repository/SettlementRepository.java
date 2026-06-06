package com.ygc.repository;

import com.ygc.model.Settlement;
import com.ygc.model.ChitMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByMembership(ChitMembership membership);
    List<Settlement> findByStatus(Settlement.SettlementStatus status);
    List<Settlement> findByMembershipUserId(Long userId);
    List<Settlement> findByMembershipId(Long membershipId);

    /**
     * FIX: Single-query fetch for reports — avoids N+1 on membership → user / chit.
     */
    @Query("SELECT s FROM Settlement s " +
           "JOIN FETCH s.membership m " +
           "JOIN FETCH m.user u " +
           "JOIN FETCH m.chit c " +
           "ORDER BY s.id")
    List<Settlement> findAllForReport();
}
