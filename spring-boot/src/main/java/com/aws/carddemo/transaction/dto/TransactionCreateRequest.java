package com.aws.carddemo.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransactionCreateRequest(
        @NotBlank(message = "Card number is required")
        @Size(min = 16, max = 16, message = "Card number must be 16 digits")
        String cardNumber,

        @NotBlank(message = "Type code is required")
        @Size(min = 1, max = 2, message = "Type code must be 1-2 characters")
        String typeCode,

        @NotBlank(message = "Category code is required")
        @Size(min = 1, max = 4, message = "Category code must be 1-4 characters")
        String categoryCode,

        @NotBlank(message = "Transaction source is required")
        @Size(max = 10)
        String source,

        @Size(max = 100)
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        LocalDateTime originationDate,

        LocalDateTime processingDate,

        @Size(max = 20)
        String merchantId,

        @Size(max = 50)
        String merchantName,

        @Size(max = 30)
        String merchantCity,

        @Size(max = 10)
        String merchantZip,

        @NotNull(message = "Confirmation is required")
        Boolean confirmed
) {
}
