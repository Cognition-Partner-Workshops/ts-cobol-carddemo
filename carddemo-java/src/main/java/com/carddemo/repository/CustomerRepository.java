package com.carddemo.repository;

import com.carddemo.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c WHERE UPPER(c.lastName) LIKE UPPER(CONCAT('%', :name, '%'))")
    Page<Customer> findByLastNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.ssn = :ssn")
    List<Customer> findBySsn(@Param("ssn") Long ssn);
}
