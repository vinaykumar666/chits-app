package com.ygc.repository;

import com.ygc.model.ChitMembership;
import com.ygc.model.Chit;
import com.ygc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
}