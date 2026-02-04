package com.aws.carddemo.service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDTO {
    private String transactionId;
    private String transactionTypeCode;
    private Integer transactionCategoryCode;
    private String transactionSource;
    private String description;
    private BigDecimal amount;
    private Long merchantId;
    private String merchantName;
    private String merchantCity;
    private String merchantZip;
    private String cardNumber;
    private String maskedCardNumber;
    private LocalDateTime originTimestamp;
    private LocalDateTime processTimestamp;
}
