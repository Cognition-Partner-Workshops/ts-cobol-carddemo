package com.aws.carddemo.api.controller;

import com.aws.carddemo.batch.scheduler.BatchScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Tag(name = "Batch Jobs", description = "Batch job management endpoints - replaces Control-M/CA7 scheduler")
public class BatchController {

    private final BatchScheduler batchScheduler;

    @PostMapping("/jobs/{jobName}/run")
    @Operation(summary = "Manually trigger a batch job")
    public ResponseEntity<String> runJob(@PathVariable String jobName) {
        try {
            batchScheduler.runJobManually(jobName);
            return ResponseEntity.ok("Job " + jobName + " started successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to start job: " + e.getMessage());
        }
    }

    @PostMapping("/cycles/daily")
    @Operation(summary = "Manually trigger daily batch cycle")
    public ResponseEntity<String> runDailyCycle() {
        try {
            batchScheduler.runDailyBatchCycle();
            return ResponseEntity.ok("Daily batch cycle started successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to start daily cycle: " + e.getMessage());
        }
    }

    @PostMapping("/cycles/monthly")
    @Operation(summary = "Manually trigger monthly batch cycle")
    public ResponseEntity<String> runMonthlyCycle() {
        try {
            batchScheduler.runMonthlyBatchCycle();
            return ResponseEntity.ok("Monthly batch cycle started successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to start monthly cycle: " + e.getMessage());
        }
    }
}
