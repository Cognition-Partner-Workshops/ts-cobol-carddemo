package com.cardemo.repository;

import com.cardemo.entity.TransactionCategoryBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionCategoryBalanceRepository extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalance.TransactionCategoryBalanceId> {
    List<TransactionCategoryBalance> findByTrancatAcctId(Long trancatAcctId);
    List<TransactionCategoryBalance> findAllByOrderByTrancatAcctIdAscTrancatTypeCdAscTrancatCdAsc();
    Optional<TransactionCategoryBalance> findByTrancatAcctIdAndTrancatTypeCdAndTrancatCd(
        Long trancatAcctId, String trancatTypeCd, Integer trancatCd);
}
