package com.aws.carddemo.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportData(
        String reportType,
        LocalDate periodStart,
        LocalDate periodEnd,
        List<CardTransactionGroup> cardGroups,
        BigDecimal grandTotal
) {}
