package com.carddemo.customer.repository;

import com.carddemo.common.entity.Customer;
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

    Optional<Customer> findBySsn(String ssn);

    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "c.ssn LIKE CONCAT('%', :searchTerm, '%')")
    Page<Customer> searchCustomers(@Param("searchTerm") String searchTerm, Pageable pageable);

    List<Customer> findByStateCode(String stateCode);

    @Query("SELECT c FROM Customer c WHERE c.ficoCreditScore >= :minScore AND c.ficoCreditScore <= :maxScore")
    List<Customer> findByFicoScoreRange(@Param("minScore") Integer minScore, @Param("maxScore") Integer maxScore);

    boolean existsBySsn(String ssn);
}
