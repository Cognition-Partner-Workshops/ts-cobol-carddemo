package com.carddemo.account.repository;

import com.carddemo.account.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    Page<Customer> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);
    Page<Customer> findBySsn(String ssn, Pageable pageable);
}
