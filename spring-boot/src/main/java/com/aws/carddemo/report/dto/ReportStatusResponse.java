package com.aws.carddemo.report.dto;

import java.time.LocalDateTime;

public record ReportStatusResponse(
        String jobId,
        ReportJobStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public enum ReportJobStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}
