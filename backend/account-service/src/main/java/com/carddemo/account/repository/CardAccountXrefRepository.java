package com.carddemo.account.repository;

import com.carddemo.common.entity.CardAccountXref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardAccountXrefRepository extends JpaRepository<CardAccountXref, CardAccountXref.CardAccountXrefId> {

    List<CardAccountXref> findByCustomerId(Long customerId);

    List<CardAccountXref> findByAccountId(Long accountId);

    List<CardAccountXref> findByCardNumber(String cardNumber);

    @Query("SELECT x FROM CardAccountXref x WHERE x.customerId = :customerId AND x.accountId = :accountId")
    List<CardAccountXref> findByCustomerIdAndAccountId(@Param("customerId") Long customerId, @Param("accountId") Long accountId);

    boolean existsByCustomerIdAndAccountId(Long customerId, Long accountId);
}
