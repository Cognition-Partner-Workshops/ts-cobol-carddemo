package com.aws.carddemo.statement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record StatementResponse(
        String statementId,
        Long accountId,
        String customerName,
        String customerAddress,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        BigDecimal openingBalance,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        BigDecimal closingBalance,
        List<StatementTransaction> transactions,
        Map<String, BigDecimal> categoryBreakdown
) {}
