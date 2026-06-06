package com.ygc.repository;

import com.ygc.model.ChitMembership;
import com.ygc.model.Chit;
import com.ygc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChitMembershipRepository extends JpaRepository<ChitMembership, Long> {
    List<ChitMembership> findByUser(User user);
    List<ChitMembership> findByChit(Chit chit);
    Optional<ChitMembership> findByChitAndUser(Chit chit, User user);
    long countByChitAndStatusNot(Chit chit, ChitMembership.MembershipStatus status);
    boolean existsByChitAndUser(Chit chit, User user);

    // Issue 10A: Find existing membership including EXITED (rejected) ones for re-application logic
    @Query("SELECT m FROM ChitMembership m WHERE m.chit = :chit AND m.user = :user AND m.status != 'EXITED'")
    Optional<ChitMembership> findActiveOrPendingByChitAndUser(Chit chit, User user);

    @Query("SELECT COUNT(m) FROM ChitMembership m WHERE m.chit = :chit AND m.user = :user AND m.status = 'EXITED'")
    int countRejectionsByChitAndUser(Chit chit, User user);

    /**
     * FIX: Fetch memberships with chit eagerly loaded — avoids N+1 in member report.
     */
    @Query("SELECT m FROM ChitMembership m JOIN FETCH m.chit WHERE m.user = :user ORDER BY m.joinedAt DESC")
    List<ChitMembership> findByUserForReport(User user);
}
