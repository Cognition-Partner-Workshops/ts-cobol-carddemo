package com.carddemo.integration.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "integration_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", length = 50, unique = true)
    private String messageId;

    @Column(name = "correlation_id", length = 50)
    private String correlationId;

    @Column(name = "source_system", length = 20)
    private String sourceSystem;

    @Column(name = "target_system", length = 20)
    private String targetSystem;

    @Column(name = "message_type", length = 30)
    private String messageType;

    @Column(name = "direction", length = 10)
    private String direction;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
