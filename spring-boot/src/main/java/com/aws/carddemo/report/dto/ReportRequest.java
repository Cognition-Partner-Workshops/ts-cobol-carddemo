package com.aws.carddemo.report.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ReportRequest(
        @NotNull ReportType reportType,
        Integer month,
        Integer year,
        LocalDate startDate,
        LocalDate endDate
) {
    public enum ReportType {
        MONTHLY, YEARLY, CUSTOM
    }
}
