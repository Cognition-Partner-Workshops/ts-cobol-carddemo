package com.carddemo.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/batch")
public class BatchJobController {

    private final JobLauncher jobLauncher;
    private final Map<String, Job> jobRegistry;

    public BatchJobController(JobLauncher jobLauncher,
                              @Qualifier("transactionPostingBatchJob") Job transactionPostingJob,
                              @Qualifier("interestCalculationBatchJob") Job interestCalculationJob,
                              @Qualifier("statementGenerationBatchJob") Job statementGenerationJob,
                              @Qualifier("transactionBackupBatchJob") Job transactionBackupJob,
                              @Qualifier("authorizationPurgeBatchJob") Job authorizationPurgeJob) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = new LinkedHashMap<>();
        jobRegistry.put("TransactionPostingJob", transactionPostingJob);
        jobRegistry.put("InterestCalculationJob", interestCalculationJob);
        jobRegistry.put("StatementGenerationJob", statementGenerationJob);
        jobRegistry.put("TransactionBackupJob", transactionBackupJob);
        jobRegistry.put("AuthorizationPurgeJob", authorizationPurgeJob);
    }

    @PostMapping("/{jobName}")
    public ResponseEntity<Map<String, String>> runJob(@PathVariable String jobName) {
        Job job = jobRegistry.get(jobName);
        if (job == null) {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("status", "ERROR");
            error.put("message", "Job not found: " + jobName);
            return ResponseEntity.badRequest().body(error);
        }

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(job, params);

            Map<String, String> result = new LinkedHashMap<>();
            result.put("status", "STARTED");
            result.put("job", jobName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
