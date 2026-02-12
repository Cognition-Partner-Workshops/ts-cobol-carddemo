package com.carddemo.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch configuration for batch job processing.
 * Will replace JCL batch jobs:
 * - CBACT01C (Account batch processing)
 * - CBACT02C (Card batch processing)
 * - CBTRN01C (Transaction batch posting)
 * - CBTRN02C (Interest calculation)
 * - CBSTM03A/B (Statement generation)
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {
}
