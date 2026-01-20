package com.carddemo.payment.dto;

import jakarta.validation.constraints.*;
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
public class PaymentRequest {
    @NotBlank(message = "Account ID is required")
    @Size(min = 11, max = 11, message = "Account ID must be 11 digits")
    private String accountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Payment source is required")
    private String paymentSource;

    @Size(max = 20, message = "Source account must be at most 20 characters")
    private String sourceAccount;

    private LocalDateTime scheduledDate;
}
