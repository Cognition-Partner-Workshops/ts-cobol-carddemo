package com.carddemo.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Transaction create request DTO replacing BMS map COTRN02 input fields.
 * Used for COTRN02C (Transaction Add) → POST /api/transactions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionCreateRequest {

    @NotBlank(message = "Card number is required")
    @Size(max = 16, message = "Card number must not exceed 16 characters")
    private String cardNumber;

    @NotBlank(message = "Transaction type code is required")
    @Size(max = 2, message = "Type code must not exceed 2 characters")
    private String typeCode;

    @NotNull(message = "Category code is required")
    private Integer categoryCode;

    @Size(max = 10, message = "Source must not exceed 10 characters")
    private String source;

    @Size(max = 100, message = "Description must not exceed 100 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @Size(max = 15, message = "Merchant ID must not exceed 15 characters")
    private String merchantId;

    @Size(max = 40, message = "Merchant name must not exceed 40 characters")
    private String merchantName;

    @Size(max = 30, message = "Merchant city must not exceed 30 characters")
    private String merchantCity;

    @Size(max = 10, message = "Merchant ZIP must not exceed 10 characters")
    private String merchantZip;
}
