package com.aws.carddemo.domain.repository;

import com.aws.carddemo.domain.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findBySsn(Long ssn);

    List<Customer> findByLastNameIgnoreCase(String lastName);

    List<Customer> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);

    Page<Customer> findByStateCode(String stateCode, Pageable pageable);

    Page<Customer> findByZipCode(String zipCode, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.ficoCreditScore >= :minScore AND c.ficoCreditScore <= :maxScore")
    Page<Customer> findByFicoScoreRange(@Param("minScore") Integer minScore, 
                                         @Param("maxScore") Integer maxScore, 
                                         Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Customer> searchByName(@Param("name") String name, Pageable pageable);

    @Query("SELECT c FROM Customer c JOIN c.cardCrossReferences xref WHERE xref.cardNumber = :cardNumber")
    Optional<Customer> findByCardNumber(@Param("cardNumber") String cardNumber);

    @Query("SELECT c FROM Customer c JOIN c.cardCrossReferences xref WHERE xref.account.accountId = :accountId")
    List<Customer> findByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.primaryCardHolder = true")
    long countPrimaryCardHolders();
}
