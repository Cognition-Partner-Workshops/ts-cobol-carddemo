package com.carddemo.batch.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionPostingResult {
    private String transactionId;
    private String accountId;
    private BigDecimal amount;
    private String previousStatus;
    private String newStatus;
    private LocalDateTime postedTimestamp;
    private String errorMessage;
}
