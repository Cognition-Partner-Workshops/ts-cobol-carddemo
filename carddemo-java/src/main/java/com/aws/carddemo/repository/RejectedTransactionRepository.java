package com.aws.carddemo.repository;

import com.aws.carddemo.entity.RejectedTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RejectedTransactionRepository extends JpaRepository<RejectedTransaction, Long> {

    List<RejectedTransaction> findByRejectionCode(Integer rejectionCode);

    Page<RejectedTransaction> findByRejectionCode(Integer rejectionCode, Pageable pageable);

    @Query("SELECT r FROM RejectedTransaction r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    List<RejectedTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(r) FROM RejectedTransaction r WHERE r.rejectionCode = :code")
    long countByRejectionCode(@Param("code") Integer code);

    @Query("SELECT r.rejectionCode, COUNT(r) FROM RejectedTransaction r GROUP BY r.rejectionCode")
    List<Object[]> countByRejectionCodeGrouped();
}
