package com.carddemo.integration.repository;

import com.carddemo.integration.entity.IntegrationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationMessageRepository extends JpaRepository<IntegrationMessage, Long> {
    Optional<IntegrationMessage> findByMessageId(String messageId);
    
    List<IntegrationMessage> findByCorrelationId(String correlationId);
    
    List<IntegrationMessage> findBySourceSystem(String sourceSystem);
    
    List<IntegrationMessage> findByTargetSystem(String targetSystem);
    
    List<IntegrationMessage> findByStatus(String status);
    
    List<IntegrationMessage> findByMessageType(String messageType);
    
    @Query("SELECT m FROM IntegrationMessage m WHERE m.status = 'PENDING' AND m.retryCount < m.maxRetries ORDER BY m.createdAt ASC")
    List<IntegrationMessage> findPendingMessages();
    
    @Query("SELECT m FROM IntegrationMessage m WHERE m.status = 'FAILED' AND m.retryCount < m.maxRetries ORDER BY m.createdAt ASC")
    List<IntegrationMessage> findRetryableMessages();
    
    @Query("SELECT m FROM IntegrationMessage m WHERE m.createdAt >= :since ORDER BY m.createdAt DESC")
    List<IntegrationMessage> findMessagesSince(@Param("since") LocalDateTime since);
}
