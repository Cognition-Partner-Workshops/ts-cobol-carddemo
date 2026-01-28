package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDto {
    private String transactionId;
    private String transactionTypeCode;
    private String transactionTypeDescription;
    private Integer transactionCategoryCode;
    private String transactionCategoryDescription;
    private String transactionSource;
    private String description;
    private BigDecimal amount;
    private Long merchantId;
    private String merchantName;
    private String merchantCity;
    private String merchantZip;
    private String cardNumber;
    private String maskedCardNumber;
    private LocalDateTime originationTimestamp;
    private LocalDateTime processingTimestamp;
    private LocalDateTime createdAt;
}
