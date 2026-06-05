package com.ygc.repository;

import com.ygc.model.EarlyExitRequest;
import com.ygc.model.ChitMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EarlyExitRequestRepository extends JpaRepository<EarlyExitRequest, Long> {
    Optional<EarlyExitRequest> findByMembership(ChitMembership membership);
    List<EarlyExitRequest> findByStatus(EarlyExitRequest.ExitStatus status);
    boolean existsByMembershipAndStatusIn(ChitMembership membership,
            java.util.Collection<EarlyExitRequest.ExitStatus> statuses);

    @Query("SELECT e FROM EarlyExitRequest e " +
           "JOIN FETCH e.membership m JOIN FETCH m.user JOIN FETCH m.chit " +
           "ORDER BY e.requestedAt DESC")
    List<EarlyExitRequest> findAllWithDetails();
}
