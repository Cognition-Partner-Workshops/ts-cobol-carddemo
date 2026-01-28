package com.carddemo.batch.job;

import com.carddemo.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/api/v1/batch")
@Tag(name = "Batch Jobs", description = "Batch job execution endpoints")
public class BatchJobController {

    private final JobLauncher jobLauncher;
    private final Job interestCalculationJob;
    private final Job transactionPostingJob;

    public BatchJobController(JobLauncher jobLauncher,
                              Job interestCalculationJob,
                              Job transactionPostingJob) {
        this.jobLauncher = jobLauncher;
        this.interestCalculationJob = interestCalculationJob;
        this.transactionPostingJob = transactionPostingJob;
    }

    @PostMapping("/interest-calculation")
    @Operation(summary = "Run interest calculation job", description = "Calculate and apply interest to all active accounts")
    public ResponseEntity<ApiResponse<String>> runInterestCalculationJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addDate("runDate", new Date())
                    .toJobParameters();
            jobLauncher.run(interestCalculationJob, params);
            return ResponseEntity.ok(ApiResponse.success("Interest calculation job started successfully", "Job started"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to start interest calculation job: " + e.getMessage()));
        }
    }

    @PostMapping("/transaction-posting")
    @Operation(summary = "Run transaction posting job", description = "Post all pending transactions")
    public ResponseEntity<ApiResponse<String>> runTransactionPostingJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addDate("runDate", new Date())
                    .toJobParameters();
            jobLauncher.run(transactionPostingJob, params);
            return ResponseEntity.ok(ApiResponse.success("Transaction posting job started successfully", "Job started"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to start transaction posting job: " + e.getMessage()));
        }
    }
}
