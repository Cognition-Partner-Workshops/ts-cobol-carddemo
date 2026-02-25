package com.cardemo.repository;

import com.cardemo.entity.AuthSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthSummaryRepository extends JpaRepository<AuthSummary, Long> {
    List<AuthSummary> findByCardNum(String cardNum);
    Optional<AuthSummary> findFirstByCardNum(String cardNum);
}
