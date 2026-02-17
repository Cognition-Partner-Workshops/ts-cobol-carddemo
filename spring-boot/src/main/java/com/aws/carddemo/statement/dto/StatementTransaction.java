package com.aws.carddemo.statement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StatementTransaction(
        Long transactionId,
        LocalDateTime timestamp,
        String cardNumber,
        String transactionType,
        String description,
        BigDecimal amount
) {}
