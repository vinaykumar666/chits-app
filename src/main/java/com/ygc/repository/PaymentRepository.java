package com.ygc.repository;

import com.ygc.model.Payment;
import com.ygc.model.ChitMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByMembership(ChitMembership membership);
    List<Payment> findByStatus(Payment.PaymentStatus status);
    List<Payment> findByMembershipAndStatus(ChitMembership membership, Payment.PaymentStatus status);

    /** Used by PaymentReminderScheduler: payments due on a specific date */
    List<Payment> findByDueDateAndStatus(LocalDate dueDate, Payment.PaymentStatus status);

    /** Used by PaymentReminderScheduler: overdue pending payments */
    List<Payment> findByStatusAndDueDateBefore(Payment.PaymentStatus status, LocalDate date);

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Payment p WHERE p.membership = :membership AND p.status = 'APPROVED'")
    BigDecimal sumApprovedPaymentsByMembership(ChitMembership membership);

    @Query("SELECT COALESCE(SUM(p.lateFine), 0) FROM Payment p WHERE p.membership = :membership")
    BigDecimal sumLateFinesByMembership(ChitMembership membership);

    /**
     * FIX: Single-query fetch for reports — avoids N+1 on membership → user / chit / verifiedBy.
     * Always use this instead of findAll() in report export.
     */
    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.membership m " +
           "JOIN FETCH m.user u " +
           "JOIN FETCH m.chit c " +
           "LEFT JOIN FETCH p.verifiedBy " +
           "ORDER BY p.id")
    List<Payment> findAllForReport();

    /**
     * FIX: Fetch payments for a specific membership without N+1 on related entities.
     */
    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.membership m " +
           "JOIN FETCH m.user u " +
           "JOIN FETCH m.chit c " +
           "WHERE m = :membership " +
           "ORDER BY p.monthNumber")
    List<Payment> findByMembershipForReport(ChitMembership membership);
}
