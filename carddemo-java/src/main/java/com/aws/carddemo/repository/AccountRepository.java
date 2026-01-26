package com.aws.carddemo.repository;

import com.aws.carddemo.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByAcctActiveStatus(String status);

    Page<Account> findByAcctActiveStatus(String status, Pageable pageable);

    List<Account> findByAcctGroupId(String groupId);

    @Query("SELECT a FROM Account a WHERE a.acctExpirationDate < :date AND a.acctActiveStatus = 'Y'")
    List<Account> findExpiredAccounts(@Param("date") LocalDate date);

    @Query("SELECT a FROM Account a WHERE a.acctExpirationDate BETWEEN :startDate AND :endDate")
    List<Account> findAccountsExpiringBetween(@Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);

    @Query("SELECT a FROM Account a WHERE a.acctCurrBal > a.acctCreditLimit")
    List<Account> findOverlimitAccounts();

    @Query("SELECT a FROM Account a WHERE a.acctAddrZip = :zip")
    List<Account> findByZipCode(@Param("zip") String zip);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.acctActiveStatus = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT a FROM Account a JOIN FETCH a.cards WHERE a.acctId = :acctId")
    Optional<Account> findByIdWithCards(@Param("acctId") Long acctId);
}
