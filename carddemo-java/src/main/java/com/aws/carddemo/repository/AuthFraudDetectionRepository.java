package com.aws.carddemo.repository;

import com.aws.carddemo.entity.AuthFraudDetection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AuthFraudDetectionRepository extends JpaRepository<AuthFraudDetection, Long> {

    List<AuthFraudDetection> findByCardNum(String cardNum);

    Page<AuthFraudDetection> findByCardNum(String cardNum, Pageable pageable);

    List<AuthFraudDetection> findByIsFraud(Boolean isFraud);

    List<AuthFraudDetection> findByReviewed(Boolean reviewed);

    @Query("SELECT f FROM AuthFraudDetection f WHERE f.reviewed = false ORDER BY f.createdAt ASC")
    List<AuthFraudDetection> findUnreviewedCases();

    @Query("SELECT f FROM AuthFraudDetection f WHERE f.fraudScore >= :minScore")
    List<AuthFraudDetection> findHighRiskCases(@Param("minScore") BigDecimal minScore);

    @Query("SELECT COUNT(f) FROM AuthFraudDetection f WHERE f.isFraud = true")
    long countFraudCases();

    @Query("SELECT COUNT(f) FROM AuthFraudDetection f WHERE f.reviewed = false")
    long countUnreviewedCases();
}
