package com.carddemo.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * CardDemo Batch Application entry point.
 * Spring Batch application replacing JCL batch jobs.
 *
 * JCL → Spring Batch mapping:
 *   POSTTRAN (CBTRN02C) → transactionPostingJob
 *   INTCALC  (CBACT04C) → interestCalculationJob
 *   CREASTMT (CBSTM03A) → statementGenerationJob
 *   TRANREPT (CBTRN03C) → transactionReportJob
 */
@SpringBootApplication(scanBasePackages = {
        "com.carddemo.batch",
        "com.carddemo.core"
})
@EntityScan(basePackages = "com.carddemo.core.domain")
@EnableJpaRepositories(basePackages = "com.carddemo.core.repository")
public class CardDemoBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDemoBatchApplication.class, args);
    }
}
