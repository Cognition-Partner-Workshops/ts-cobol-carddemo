package com.aws.carddemo.domain.repository;

import com.aws.carddemo.domain.entity.Account;
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

    List<Account> findByGroupId(String groupId);

    Page<Account> findByGroupId(String groupId, Pageable pageable);

    List<Account> findByZipCode(String zipCode);

    @Query("SELECT a FROM Account a WHERE a.currentBalance > a.creditLimit")
    List<Account> findOverLimitAccounts();

    @Query("SELECT a FROM Account a WHERE a.expirationDate <= :date AND a.activeStatus = 'Y'")
    List<Account> findExpiringAccounts(@Param("date") LocalDate date);

    @Query("SELECT a FROM Account a WHERE a.currentBalance >= :minBalance AND a.currentBalance <= :maxBalance")
    Page<Account> findByBalanceRange(@Param("minBalance") BigDecimal minBalance,
                                      @Param("maxBalance") BigDecimal maxBalance,
                                      Pageable pageable);

    @Query("SELECT a FROM Account a JOIN a.cards c WHERE c.cardNumber = :cardNumber")
    Account findByCardNumber(@Param("cardNumber") String cardNumber);

    @Query("SELECT SUM(a.currentBalance) FROM Account a WHERE a.activeStatus = 'Y'")
    BigDecimal getTotalActiveBalance();

    @Query("SELECT SUM(a.creditLimit) FROM Account a WHERE a.activeStatus = 'Y'")
    BigDecimal getTotalCreditLimit();

    @Query("SELECT COUNT(a) FROM Account a WHERE a.activeStatus = 'Y'")
    long countActiveAccounts();

    @Query("SELECT a FROM Account a WHERE a.openDate BETWEEN :startDate AND :endDate")
    List<Account> findAccountsOpenedBetween(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
}
