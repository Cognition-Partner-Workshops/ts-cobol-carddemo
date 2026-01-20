package com.carddemo.account.repository;

import com.carddemo.account.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByCustomerId(String customerId);
    Page<Account> findByActiveStatus(String activeStatus, Pageable pageable);
    Page<Account> findByCustomerIdContaining(String customerId, Pageable pageable);
}
