package com.aws.carddemo.report;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;
import com.aws.carddemo.report.dto.ReportData;
import com.aws.carddemo.report.dto.ReportRequest;
import com.aws.carddemo.report.dto.ReportStatusResponse;
import com.aws.carddemo.report.dto.ReportStatusResponse.ReportJobStatus;

@Service
public class ReportService {

    private final ReportGenerator reportGenerator;
    private final ConcurrentHashMap<String, ReportJob> jobStore = new ConcurrentHashMap<>();

    public ReportService(ReportGenerator reportGenerator) {
        this.reportGenerator = reportGenerator;
    }

    public String submitReport(ReportRequest request) {
        validateRequest(request);
        String jobId = UUID.randomUUID().toString();
        ReportJob job = new ReportJob(jobId);
        jobStore.put(jobId, job);
        reportGenerator.generateReportAsync(job, request);
        return jobId;
    }

    public ReportStatusResponse getJobStatus(String jobId) {
        ReportJob job = jobStore.get(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("Report job not found with id: " + jobId);
        }
        return new ReportStatusResponse(job.getJobId(), job.getStatus(), job.getCreatedAt(), job.getCompletedAt());
    }

    public ReportData getReportData(String jobId) {
        ReportJob job = jobStore.get(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("Report job not found with id: " + jobId);
        }
        if (job.getStatus() != ReportJobStatus.COMPLETED) {
            throw new ValidationException("Report is not yet completed. Current status: " + job.getStatus());
        }
        return job.getReportData();
    }

    private void validateRequest(ReportRequest request) {
        switch (request.reportType()) {
            case MONTHLY -> {
                if (request.month() == null || request.year() == null) {
                    throw new ValidationException("Month and year are required for MONTHLY reports");
                }
                if (request.month() < 1 || request.month() > 12) {
                    throw new ValidationException("Month must be between 1 and 12");
                }
            }
            case YEARLY -> {
                if (request.year() == null) {
                    throw new ValidationException("Year is required for YEARLY reports");
                }
            }
            case CUSTOM -> {
                if (request.startDate() == null || request.endDate() == null) {
                    throw new ValidationException("Start date and end date are required for CUSTOM reports");
                }
                if (request.startDate().isAfter(request.endDate())) {
                    throw new ValidationException("Start date must be before or equal to end date");
                }
            }
        }
    }

    ConcurrentHashMap<String, ReportJob> getJobStore() {
        return jobStore;
    }
}
