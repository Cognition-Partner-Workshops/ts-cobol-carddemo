package com.aws.carddemo.repository;

import com.aws.carddemo.entity.CardXref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardXrefRepository extends JpaRepository<CardXref, String> {

    List<CardXref> findByCustomerCustId(Long custId);

    List<CardXref> findByAccountAcctId(Long acctId);

    @Query("SELECT x FROM CardXref x JOIN FETCH x.card JOIN FETCH x.customer JOIN FETCH x.account WHERE x.xrefCardNum = :cardNum")
    Optional<CardXref> findByCardNumWithDetails(@Param("cardNum") String cardNum);

    @Query("SELECT x FROM CardXref x JOIN FETCH x.card WHERE x.customer.custId = :custId")
    List<CardXref> findByCustomerIdWithCards(@Param("custId") Long custId);

    @Query("SELECT x FROM CardXref x JOIN FETCH x.card WHERE x.account.acctId = :acctId")
    List<CardXref> findByAccountIdWithCards(@Param("acctId") Long acctId);

    boolean existsByXrefCardNum(String cardNum);
}
