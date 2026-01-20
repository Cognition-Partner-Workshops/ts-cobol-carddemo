package com.carddemo.integration.service;

import com.carddemo.integration.dto.IntegrationMessageDto;
import com.carddemo.integration.entity.IntegrationMessage;
import com.carddemo.integration.repository.IntegrationMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    
    private final IntegrationMessageRepository messageRepository;
    
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    
    public List<IntegrationMessage> getAllMessages() {
        return messageRepository.findAll();
    }
    
    public Optional<IntegrationMessage> getMessageById(String messageId) {
        return messageRepository.findByMessageId(messageId);
    }
    
    public List<IntegrationMessage> getMessagesByCorrelation(String correlationId) {
        return messageRepository.findByCorrelationId(correlationId);
    }
    
    public List<IntegrationMessage> getMessagesByStatus(String status) {
        return messageRepository.findByStatus(status);
    }
    
    public List<IntegrationMessage> getPendingMessages() {
        return messageRepository.findPendingMessages();
    }
    
    public List<IntegrationMessage> getRetryableMessages() {
        return messageRepository.findRetryableMessages();
    }
    
    public List<IntegrationMessage> getRecentMessages(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return messageRepository.findMessagesSince(since);
    }
    
    @Transactional
    public IntegrationMessage sendMessage(IntegrationMessageDto dto) {
        IntegrationMessage message = new IntegrationMessage();
        message.setMessageId(generateMessageId());
        message.setCorrelationId(dto.getCorrelationId() != null ? dto.getCorrelationId() : message.getMessageId());
        message.setSourceSystem(dto.getSourceSystem());
        message.setTargetSystem(dto.getTargetSystem());
        message.setMessageType(dto.getMessageType());
        message.setDirection("OUTBOUND");
        message.setPayload(dto.getPayload());
        message.setStatus(STATUS_PENDING);
        message.setRetryCount(0);
        message.setMaxRetries(dto.getMaxRetries() != null ? dto.getMaxRetries() : 3);
        message.setCreatedAt(LocalDateTime.now());
        
        IntegrationMessage saved = messageRepository.save(message);
        log.info("Created outbound message: {} to {}", saved.getMessageId(), saved.getTargetSystem());
        
        // Trigger async processing
        processMessageAsync(saved.getMessageId());
        
        return saved;
    }
    
    @Transactional
    public IntegrationMessage receiveMessage(IntegrationMessageDto dto) {
        IntegrationMessage message = new IntegrationMessage();
        message.setMessageId(generateMessageId());
        message.setCorrelationId(dto.getCorrelationId());
        message.setSourceSystem(dto.getSourceSystem());
        message.setTargetSystem("CARDDEMO");
        message.setMessageType(dto.getMessageType());
        message.setDirection("INBOUND");
        message.setPayload(dto.getPayload());
        message.setStatus(STATUS_PENDING);
        message.setRetryCount(0);
        message.setMaxRetries(dto.getMaxRetries() != null ? dto.getMaxRetries() : 3);
        message.setCreatedAt(LocalDateTime.now());
        
        IntegrationMessage saved = messageRepository.save(message);
        log.info("Received inbound message: {} from {}", saved.getMessageId(), saved.getSourceSystem());
        
        // Trigger async processing
        processMessageAsync(saved.getMessageId());
        
        return saved;
    }
    
    @Async
    public void processMessageAsync(String messageId) {
        try {
            Thread.sleep(100); // Small delay to allow transaction to commit
            processMessage(messageId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Message processing interrupted: {}", messageId);
        }
    }
    
    @Transactional
    public Optional<IntegrationMessage> processMessage(String messageId) {
        Optional<IntegrationMessage> messageOpt = messageRepository.findByMessageId(messageId);
        if (messageOpt.isEmpty()) {
            return Optional.empty();
        }
        
        IntegrationMessage message = messageOpt.get();
        
        if (!STATUS_PENDING.equals(message.getStatus()) && !STATUS_FAILED.equals(message.getStatus())) {
            log.warn("Message {} is not in processable state: {}", messageId, message.getStatus());
            return Optional.of(message);
        }
        
        message.setStatus(STATUS_PROCESSING);
        message.setProcessedAt(LocalDateTime.now());
        messageRepository.save(message);
        
        try {
            // Simulate message processing (in real implementation, would route to appropriate handler)
            log.info("Processing message: {} type={}", messageId, message.getMessageType());
            
            // For demo purposes, mark as completed
            message.setStatus(STATUS_COMPLETED);
            message.setCompletedAt(LocalDateTime.now());
            log.info("Message {} processed successfully", messageId);
            
        } catch (Exception e) {
            message.setRetryCount(message.getRetryCount() + 1);
            message.setErrorMessage(e.getMessage());
            
            if (message.getRetryCount() >= message.getMaxRetries()) {
                message.setStatus(STATUS_FAILED);
                log.error("Message {} failed after {} retries: {}", messageId, message.getRetryCount(), e.getMessage());
            } else {
                message.setStatus(STATUS_PENDING);
                log.warn("Message {} failed, will retry ({}/{}): {}", 
                    messageId, message.getRetryCount(), message.getMaxRetries(), e.getMessage());
            }
        }
        
        return Optional.of(messageRepository.save(message));
    }
    
    @Transactional
    public Optional<IntegrationMessage> retryMessage(String messageId) {
        Optional<IntegrationMessage> messageOpt = messageRepository.findByMessageId(messageId);
        if (messageOpt.isEmpty()) {
            return Optional.empty();
        }
        
        IntegrationMessage message = messageOpt.get();
        message.setStatus(STATUS_PENDING);
        message.setErrorMessage(null);
        messageRepository.save(message);
        
        log.info("Message {} queued for retry", messageId);
        processMessageAsync(messageId);
        
        return Optional.of(message);
    }
    
    private String generateMessageId() {
        return "MSG" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
