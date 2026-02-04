package com.aws.carddemo.domain.repository;

import com.aws.carddemo.domain.entity.CardCrossReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardCrossReferenceRepository extends JpaRepository<CardCrossReference, String> {

    List<CardCrossReference> findByCustomerCustomerId(Long customerId);

    Page<CardCrossReference> findByCustomerCustomerId(Long customerId, Pageable pageable);

    List<CardCrossReference> findByAccountAccountId(Long accountId);

    Page<CardCrossReference> findByAccountAccountId(Long accountId, Pageable pageable);

    @Query("SELECT xref FROM CardCrossReference xref WHERE xref.customer.customerId = :customerId AND xref.account.accountId = :accountId")
    List<CardCrossReference> findByCustomerAndAccount(@Param("customerId") Long customerId, 
                                                       @Param("accountId") Long accountId);

    @Query("SELECT COUNT(xref) FROM CardCrossReference xref WHERE xref.customer.customerId = :customerId")
    long countByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT COUNT(xref) FROM CardCrossReference xref WHERE xref.account.accountId = :accountId")
    long countByAccountId(@Param("accountId") Long accountId);

    boolean existsByCardNumber(String cardNumber);
}
