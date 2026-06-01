package com.ygc.repository;

import com.ygc.model.Payment;
import com.ygc.model.ChitMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByMembership(ChitMembership membership);
    List<Payment> findByStatus(Payment.PaymentStatus status);
    List<Payment> findByMembershipAndStatus(ChitMembership membership, Payment.PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Payment p WHERE p.membership = :membership AND p.status = 'APPROVED'")
    BigDecimal sumApprovedPaymentsByMembership(ChitMembership membership);

    @Query("SELECT COALESCE(SUM(p.lateFine), 0) FROM Payment p WHERE p.membership = :membership")
    BigDecimal sumLateFinesByMembership(ChitMembership membership);
}