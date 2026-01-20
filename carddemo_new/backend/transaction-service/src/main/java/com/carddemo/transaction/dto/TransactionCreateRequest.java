package com.carddemo.transaction.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreateRequest {
    @NotBlank(message = "Card number is required")
    @Size(min = 16, max = 16, message = "Card number must be 16 digits")
    private String cardNumber;

    @NotBlank(message = "Transaction type is required")
    @Size(min = 2, max = 2, message = "Type code must be 2 characters")
    private String typeCode;

    @NotBlank(message = "Category code is required")
    @Size(min = 4, max = 4, message = "Category code must be 4 characters")
    private String categoryCode;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 100, message = "Description must be at most 100 characters")
    private String description;

    @Size(max = 9, message = "Merchant ID must be at most 9 characters")
    private String merchantId;

    @Size(max = 50, message = "Merchant name must be at most 50 characters")
    private String merchantName;

    @Size(max = 50, message = "Merchant city must be at most 50 characters")
    private String merchantCity;

    @Size(max = 10, message = "Merchant zip must be at most 10 characters")
    private String merchantZip;
}
