package com.aws.carddemo.report.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CardTransactionGroup(
        String cardNumber,
        List<TransactionDetail> transactions,
        Map<String, BigDecimal> categoryTotals,
        BigDecimal cardTotal
) {}
