package com.aws.carddemo.report;

import java.time.LocalDateTime;

import com.aws.carddemo.report.dto.ReportData;
import com.aws.carddemo.report.dto.ReportStatusResponse.ReportJobStatus;

public class ReportJob {

    private final String jobId;
    private volatile ReportJobStatus status;
    private final LocalDateTime createdAt;
    private volatile LocalDateTime completedAt;
    private volatile ReportData reportData;
    private volatile String errorMessage;

    public ReportJob(String jobId) {
        this.jobId = jobId;
        this.status = ReportJobStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public String getJobId() {
        return jobId;
    }

    public ReportJobStatus getStatus() {
        return status;
    }

    public void setStatus(ReportJobStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public ReportData getReportData() {
        return reportData;
    }

    public void setReportData(ReportData reportData) {
        this.reportData = reportData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
