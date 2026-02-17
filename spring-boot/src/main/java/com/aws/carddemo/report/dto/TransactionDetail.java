package com.aws.carddemo.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDetail(
        Long transactionId,
        LocalDateTime timestamp,
        String transactionType,
        String typeDescription,
        String transactionCategory,
        String categoryDescription,
        String description,
        BigDecimal amount,
        String merchantName,
        String merchantCity
) {}
