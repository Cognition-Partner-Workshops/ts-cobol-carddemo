package com.aws.carddemo.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillPaymentResponse(
        Long accountId,
        BigDecimal amountPaid,
        BigDecimal newBalance,
        Long transactionId,
        LocalDateTime timestamp
) {}
