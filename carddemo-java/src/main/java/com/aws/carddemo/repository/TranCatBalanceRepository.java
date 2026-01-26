package com.aws.carddemo.repository;

import com.aws.carddemo.entity.TranCatBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TranCatBalanceRepository extends JpaRepository<TranCatBalance, TranCatBalance.TranCatBalanceId> {

    List<TranCatBalance> findByTruncatAcctId(Long acctId);

    @Query("SELECT t FROM TranCatBalance t WHERE t.trancatAcctId = :acctId AND t.trancatTypeCd = :typeCd AND t.trancatCd = :catCd")
    Optional<TranCatBalance> findByKey(@Param("acctId") Long acctId, 
                                        @Param("typeCd") String typeCd, 
                                        @Param("catCd") Integer catCd);

    @Query("SELECT SUM(t.tranCatBal) FROM TranCatBalance t WHERE t.trancatAcctId = :acctId")
    BigDecimal sumBalanceByAccount(@Param("acctId") Long acctId);

    @Query("SELECT t FROM TranCatBalance t WHERE t.trancatTypeCd = :typeCd")
    List<TranCatBalance> findByTypeCode(@Param("typeCd") String typeCd);
}
