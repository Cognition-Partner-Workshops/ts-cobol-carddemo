package com.aws.cardemo.persistence.repository;

import com.aws.cardemo.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findByAccountStatus(String status);

    List<Account> findByGroupId(String groupId);

    @Query("SELECT a FROM Account a WHERE a.currentBalance > a.creditLimit")
    List<Account> findAccountsOverCreditLimit();

    @Query("SELECT a FROM Account a WHERE a.accountId = :accountId AND a.accountStatus = 'A'")
    Optional<Account> findActiveAccountById(@Param("accountId") String accountId);
}
