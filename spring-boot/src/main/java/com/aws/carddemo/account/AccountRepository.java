package com.aws.carddemo.account;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByCustomerId(Long customerId);

    Page<Account> findByCustomerId(Long customerId, Pageable pageable);

    List<Account> findByAccountStatus(String accountStatus);

    @Query("SELECT a FROM Account a JOIN FETCH a.customer WHERE a.id = :id")
    java.util.Optional<Account> findByIdWithCustomer(@Param("id") Long id);

    long countByCustomerId(Long customerId);
}
