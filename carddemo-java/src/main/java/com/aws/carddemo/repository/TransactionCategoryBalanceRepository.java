package com.aws.carddemo.repository;

import com.aws.carddemo.model.TransactionCategoryBalance;
import com.aws.carddemo.model.TransactionCategoryBalance.TransactionCategoryBalanceKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionCategoryBalanceRepository extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalanceKey> {

    List<TransactionCategoryBalance> findByIdTrancatAcctId(Long acctId);
}
