package com.carddemo.transaction.repository;

import com.carddemo.transaction.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface AccountRepository extends JpaRepository<Account, BigDecimal> {
}
