package com.carddemo.repository;

import com.carddemo.entity.AuthorizationSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorizationSummaryRepository extends JpaRepository<AuthorizationSummary, Long> {

    Optional<AuthorizationSummary> findByCardNum(String cardNum);

    List<AuthorizationSummary> findByAcctId(Long acctId);
}
