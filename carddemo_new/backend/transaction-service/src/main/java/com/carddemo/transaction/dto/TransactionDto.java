package com.carddemo.transaction.dto;

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
    private String transactionId;
    private String typeCode;
    private String typeDescription;
    private String categoryCode;
    private String categoryDescription;
    private String source;
    private String description;
    private BigDecimal amount;
    private String merchantId;
    private String merchantName;
    private String merchantCity;
    private String merchantZip;
    private String cardNumber;
    private String maskedCardNumber;
    private String accountId;
    private LocalDateTime originalTimestamp;
    private LocalDateTime processedTimestamp;
    private String status;
}
