package com.carddemo.batch.repository;

import com.carddemo.batch.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByActiveStatus(String activeStatus);
    
    @Query("SELECT a FROM Account a WHERE a.activeStatus = 'Y' AND a.currentBalance > 0")
    List<Account> findAccountsWithBalance();
    
    @Query("SELECT a FROM Account a WHERE a.activeStatus = 'Y'")
    List<Account> findActiveAccounts();
}
