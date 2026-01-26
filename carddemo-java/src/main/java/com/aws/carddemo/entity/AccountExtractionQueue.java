package com.aws.carddemo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_extraction_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountExtractionQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acct_id", nullable = false)
    private Account account;

    @Column(name = "request_type", nullable = false, length = 20)
    private String requestType;

    @Column(name = "request_status", nullable = false, length = 20)
    @Builder.Default
    private String requestStatus = "PENDING";

    @Column(name = "request_data", columnDefinition = "jsonb")
    private String requestData;

    @Column(name = "response_data", columnDefinition = "jsonb")
    private String responseData;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    public static final String TYPE_FULL_EXTRACT = "FULL_EXTRACT";
    public static final String TYPE_BALANCE_INQUIRY = "BALANCE_INQUIRY";
    public static final String TYPE_STATEMENT = "STATEMENT";
}
