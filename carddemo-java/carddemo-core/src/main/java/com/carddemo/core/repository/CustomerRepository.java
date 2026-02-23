package com.carddemo.core.repository;

import com.carddemo.core.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Customer entity.
 * Replaces VSAM READ/WRITE/REWRITE/DELETE operations on CUSTDATA file.
 * VSAM key: CUST-ID (PIC 9(09))
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Page<Customer> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);

    Page<Customer> findBySsn(String ssn, Pageable pageable);
}
