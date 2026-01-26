package com.aws.carddemo.repository;

import com.aws.carddemo.entity.InterestCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InterestCalculationRepository extends JpaRepository<InterestCalculation, Long> {

    List<InterestCalculation> findByAccountAcctId(Long acctId);

    List<InterestCalculation> findByCalcDate(LocalDate calcDate);

    @Query("SELECT i FROM InterestCalculation i WHERE i.account.acctId = :acctId AND i.calcDate BETWEEN :startDate AND :endDate")
    List<InterestCalculation> findByAccountAndDateRange(@Param("acctId") Long acctId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(i.interestAmount) FROM InterestCalculation i WHERE i.account.acctId = :acctId")
    BigDecimal sumInterestByAccount(@Param("acctId") Long acctId);

    @Query("SELECT SUM(i.interestAmount) FROM InterestCalculation i WHERE i.calcDate = :date")
    BigDecimal sumInterestByDate(@Param("date") LocalDate date);
}
