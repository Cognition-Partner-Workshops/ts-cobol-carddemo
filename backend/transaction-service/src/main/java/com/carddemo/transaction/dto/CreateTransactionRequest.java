package com.carddemo.transaction.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequest {

    @NotBlank(message = "Card number is required")
    @Size(min = 16, max = 16, message = "Card number must be 16 digits")
    private String cardNumber;

    @NotBlank(message = "Transaction type code is required")
    @Size(max = 2, message = "Transaction type code must be at most 2 characters")
    private String transactionTypeCode;

    @NotNull(message = "Transaction category code is required")
    private Integer transactionCategoryCode;

    @Size(max = 10, message = "Transaction source must be at most 10 characters")
    private String transactionSource;

    @Size(max = 100, message = "Description must be at most 100 characters")
    private String description;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private Long merchantId;

    @Size(max = 50, message = "Merchant name must be at most 50 characters")
    private String merchantName;

    @Size(max = 50, message = "Merchant city must be at most 50 characters")
    private String merchantCity;

    @Size(max = 10, message = "Merchant zip must be at most 10 characters")
    private String merchantZip;
}
