package com.carddemo.payment.repository;

import com.carddemo.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    Page<Payment> findByAccountId(String accountId, Pageable pageable);
    List<Payment> findByAccountIdAndStatus(String accountId, String status);
    List<Payment> findByStatus(String status);
}
