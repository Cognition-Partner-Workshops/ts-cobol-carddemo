package com.aws.carddemo.dto;

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
public class TransactionDto {

    @NotBlank(message = "Transaction ID is required")
    @Size(max = 16, message = "Transaction ID must be at most 16 characters")
    private String tranId;

    @NotBlank(message = "Transaction type code is required")
    @Size(max = 2, message = "Transaction type code must be at most 2 characters")
    private String tranTypeCd;

    @NotNull(message = "Transaction category code is required")
    @Digits(integer = 4, fraction = 0, message = "Transaction category code must be at most 4 digits")
    private Integer tranCatCd;

    @Size(max = 10, message = "Transaction source must be at most 10 characters")
    private String tranSource;

    @Size(max = 100, message = "Transaction description must be at most 100 characters")
    private String tranDesc;

    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "-999999999.99", message = "Transaction amount out of range")
    @DecimalMax(value = "999999999.99", message = "Transaction amount out of range")
    private BigDecimal tranAmt;

    @Digits(integer = 9, fraction = 0, message = "Merchant ID must be at most 9 digits")
    private Long tranMerchantId;

    @Size(max = 50, message = "Merchant name must be at most 50 characters")
    private String tranMerchantName;

    @Size(max = 50, message = "Merchant city must be at most 50 characters")
    private String tranMerchantCity;

    @Size(max = 10, message = "Merchant ZIP must be at most 10 characters")
    private String tranMerchantZip;

    @NotBlank(message = "Card number is required")
    @Size(max = 16, message = "Card number must be at most 16 characters")
    private String tranCardNum;

    @NotNull(message = "Original timestamp is required")
    private LocalDateTime tranOrigTs;

    private LocalDateTime tranProcTs;

    private LocalDateTime createdAt;

    private boolean credit;
    private boolean debit;
}
