package com.carddemo.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private String paymentId;
    private String accountId;
    private BigDecimal amount;
    private String paymentSource;
    private String sourceAccount;
    private String confirmationNumber;
    private String status;
    private LocalDateTime scheduledDate;
    private LocalDateTime processedDate;
    private LocalDateTime createdAt;
}
