package com.carddemo.payment.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(ACH|DEBIT|CHECK|CASH)$", message = "Payment method must be ACH, DEBIT, CHECK, or CASH")
    private String paymentMethod;

    @Size(max = 20, message = "Source account must be at most 20 characters")
    private String sourceAccount;

    @Size(min = 9, max = 9, message = "Routing number must be exactly 9 digits")
    private String routingNumber;

    private LocalDateTime scheduledDate;

    @Size(max = 255, message = "Notes must be at most 255 characters")
    private String notes;
}
