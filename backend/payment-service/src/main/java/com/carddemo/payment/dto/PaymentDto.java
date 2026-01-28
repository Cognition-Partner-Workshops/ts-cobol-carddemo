package com.carddemo.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentDto {
    private String paymentId;
    private Long accountId;
    private BigDecimal amount;
    private String paymentMethod;
    private String sourceAccountMasked;
    private String confirmationNumber;
    private String status;
    private LocalDateTime scheduledDate;
    private LocalDateTime processedDate;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
