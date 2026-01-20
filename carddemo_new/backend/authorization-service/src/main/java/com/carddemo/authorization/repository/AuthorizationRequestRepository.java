package com.carddemo.authorization.repository;

import com.carddemo.authorization.entity.AuthorizationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorizationRequestRepository extends JpaRepository<AuthorizationRequest, Long> {
    Optional<AuthorizationRequest> findByAuthId(String authId);
    
    List<AuthorizationRequest> findByCardNumber(String cardNumber);
    
    List<AuthorizationRequest> findByAccountId(String accountId);
    
    List<AuthorizationRequest> findByStatus(String status);
    
    @Query("SELECT a FROM AuthorizationRequest a WHERE a.cardNumber = :cardNumber AND a.requestTimestamp >= :since")
    List<AuthorizationRequest> findByCardNumberSince(@Param("cardNumber") String cardNumber, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(a) FROM AuthorizationRequest a WHERE a.cardNumber = :cardNumber AND a.requestTimestamp >= :since AND a.status = 'APPROVED'")
    Long countApprovedSince(@Param("cardNumber") String cardNumber, @Param("since") LocalDateTime since);
    
    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM AuthorizationRequest a WHERE a.cardNumber = :cardNumber AND a.requestTimestamp >= :since AND a.status = 'APPROVED'")
    java.math.BigDecimal sumApprovedAmountSince(@Param("cardNumber") String cardNumber, @Param("since") LocalDateTime since);
}
