package com.aws.cardemo.persistence.repository;

import com.aws.cardemo.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    List<Customer> findByLastName(String lastName);

    List<Customer> findByStateCode(String stateCode);

    Optional<Customer> findBySsn(String ssn);

    @Query("SELECT c FROM Customer c WHERE c.firstName LIKE %:name% OR c.lastName LIKE %:name%")
    List<Customer> searchByName(@Param("name") String name);

    @Query("SELECT c FROM Customer c WHERE c.ficoCreditScore >= :minScore")
    List<Customer> findByMinimumCreditScore(@Param("minScore") Integer minScore);
}
