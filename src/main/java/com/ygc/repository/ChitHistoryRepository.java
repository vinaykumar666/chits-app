package com.ygc.repository;

import com.ygc.model.ChitHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChitHistoryRepository extends JpaRepository<ChitHistory, Long> {

    /** All history entries, newest first (for the admin closed-chits view) */
    List<ChitHistory> findAllByOrderByClosedAtDesc();

    /** Look up history by original chit ID */
    List<ChitHistory> findByOriginalChitId(Long originalChitId);
}
