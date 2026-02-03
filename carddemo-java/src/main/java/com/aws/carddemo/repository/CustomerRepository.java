package com.aws.carddemo.repository;

import com.aws.carddemo.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByCustLastName(String lastName);

    List<Customer> findByCustAddrStateCd(String stateCd);

    List<Customer> findByCustAddrZip(String zip);

    Optional<Customer> findByCustSsn(Long ssn);
}
