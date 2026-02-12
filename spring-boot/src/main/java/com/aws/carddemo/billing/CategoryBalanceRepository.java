package com.aws.carddemo.billing;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryBalanceRepository extends JpaRepository<CategoryBalance, CategoryBalanceId> {

    List<CategoryBalance> findByAccountId(Long accountId);
}
