package com.carddemo.repository;

import com.carddemo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for Customer entities.
 * Replaces VSAM KSDS I/O operations on CUSTFILE.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
