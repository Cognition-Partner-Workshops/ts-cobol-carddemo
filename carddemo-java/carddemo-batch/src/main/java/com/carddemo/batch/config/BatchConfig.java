package com.carddemo.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch global configuration.
 * Replaces JES2/JES3 job scheduling and JCL EXEC statements.
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    // Spring Batch auto-configuration handles JobRepository, JobLauncher, etc.
    // Custom configurations (e.g., custom TaskExecutor) can be added here in Phase 3.
}
