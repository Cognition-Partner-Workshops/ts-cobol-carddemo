package com.aws.carddemo.billing.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        Long accountId,
        BigDecimal currentBalance,
        String accountStatus
) {}
