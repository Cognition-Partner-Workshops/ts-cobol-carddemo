package com.carddemo.validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the CardDemo Data Validation Service.
 *
 * <p>This microservice validates migrated CardDemo data by comparing:
 * <ul>
 *   <li>DB2/legacy data vs PostgreSQL data (row counts, checksums, sample diffs)</li>
 *   <li>Mainframe output files vs PostgreSQL data</li>
 *   <li>Mainframe output files vs microservice API output</li>
 * </ul>
 */
@SpringBootApplication
public class DataValidationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataValidationServiceApplication.class, args);
    }
}
