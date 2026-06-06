package com.ygc.repository;

import com.ygc.model.LoginHistory;
import com.ygc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findTop20ByUserOrderByLoginAtDesc(User user);
    List<LoginHistory> findTop50ByOrderByLoginAtDesc();
    long countByUserAndSuccessFalseAndLoginAtAfter(User user, LocalDateTime after);
    List<LoginHistory> findByIpAddressAndUserNot(String ip, User user);
    boolean existsByUserAndIpAddress(User user, String ip);
    void deleteByUser(User user);
}
