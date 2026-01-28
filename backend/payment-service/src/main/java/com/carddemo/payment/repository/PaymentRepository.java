package com.carddemo.payment.repository;

import com.carddemo.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByAccountId(Long accountId);

    Page<Payment> findByAccountId(Long accountId, Pageable pageable);

    List<Payment> findByStatus(String status);

    @Query("SELECT p FROM Payment p WHERE p.scheduledDate <= :date AND p.status = 'SCHEDULED'")
    List<Payment> findPaymentsDueForProcessing(@Param("date") LocalDateTime date);

    @Query("SELECT p FROM Payment p WHERE p.accountId = :accountId AND p.processedDate BETWEEN :startDate AND :endDate")
    List<Payment> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.accountId = :accountId AND p.status = 'COMPLETED'")
    BigDecimal sumCompletedPaymentsByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    Long countByStatus(@Param("status") String status);
}
