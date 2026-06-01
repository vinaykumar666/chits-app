package com.ygc.repository;

import com.ygc.model.Settlement;
import com.ygc.model.ChitMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByMembership(ChitMembership membership);
    List<Settlement> findByStatus(Settlement.SettlementStatus status);
}