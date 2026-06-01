package com.ygc.repository;

import com.ygc.model.Chit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChitRepository extends JpaRepository<Chit, Long> {
    List<Chit> findByStatus(Chit.ChitStatus status);

    @Query("SELECT c FROM Chit c WHERE c.status = 'OPEN' AND " +
            "(SELECT COUNT(m) FROM ChitMembership m WHERE m.chit = c AND m.status != 'EXITED') < c.totalMembers")
    List<Chit> findAvailableChits();
}