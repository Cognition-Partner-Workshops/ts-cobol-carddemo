package com.aws.carddemo.repository;

import com.aws.carddemo.entity.AuthRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuthRequestRepository extends JpaRepository<AuthRequest, Long> {

    List<AuthRequest> findByCardNum(String cardNum);

    Page<AuthRequest> findByCardNum(String cardNum, Pageable pageable);

    List<AuthRequest> findByAuthStatus(String status);

    Page<AuthRequest> findByAuthStatus(String status, Pageable pageable);

    @Query("SELECT a FROM AuthRequest a WHERE a.requestTs BETWEEN :startDate AND :endDate")
    List<AuthRequest> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM AuthRequest a WHERE a.cardNum = :cardNum AND a.authStatus = :status")
    List<AuthRequest> findByCardNumAndStatus(@Param("cardNum") String cardNum, @Param("status") String status);

    @Query("SELECT COUNT(a) FROM AuthRequest a WHERE a.authStatus = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT a FROM AuthRequest a WHERE a.authStatus = 'PENDING' ORDER BY a.requestTs ASC")
    List<AuthRequest> findPendingRequests();
}
