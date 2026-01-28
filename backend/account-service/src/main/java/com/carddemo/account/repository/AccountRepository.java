package com.carddemo.account.repository;

import com.carddemo.common.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByActiveStatus(String activeStatus);

    Page<Account> findByActiveStatus(String activeStatus, Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.currentBalance > a.creditLimit")
    List<Account> findOverLimitAccounts();

    @Query("SELECT a FROM Account a WHERE a.expirationDate < :date")
    List<Account> findExpiredAccounts(@Param("date") LocalDate date);

    @Query("SELECT a FROM Account a WHERE a.expirationDate BETWEEN :startDate AND :endDate")
    List<Account> findAccountsExpiringBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT a FROM Account a WHERE a.groupId = :groupId")
    List<Account> findByGroupId(@Param("groupId") String groupId);

    @Query("SELECT SUM(a.currentBalance) FROM Account a WHERE a.activeStatus = 'Y'")
    BigDecimal getTotalActiveBalance();

    @Query("SELECT COUNT(a) FROM Account a WHERE a.activeStatus = 'Y'")
    Long countActiveAccounts();
}
