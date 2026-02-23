package com.carddemo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction response DTO replacing BMS map COTRN01 output fields.
 * Maps from Transaction entity (CVTRA05Y copybook).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

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
    private LocalDateTime originTimestamp;
    private LocalDateTime processTimestamp;
}
