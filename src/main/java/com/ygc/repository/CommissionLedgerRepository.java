package com.ygc.repository;

import com.ygc.model.CommissionLedger;
import com.ygc.model.Chit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CommissionLedgerRepository extends JpaRepository<CommissionLedger, Long> {
    List<CommissionLedger> findByChit(Chit chit);

    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM CommissionLedger c WHERE c.chit = :chit")
    BigDecimal totalCommissionByChit(Chit chit);
}