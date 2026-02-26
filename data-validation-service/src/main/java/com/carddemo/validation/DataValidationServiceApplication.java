package com.carddemo.validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.carddemo.validation.config.ValidationProperties;

/**
 * Entry point for the CardDemo Data Validation Service.
 *
 * <p>This microservice validates migrated CardDemo data by comparing
 * DB2/legacy sources against Postgres targets, mainframe output files
 * against Postgres data, and mainframe output files against microservice
 * API output.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(ValidationProperties.class)
public class DataValidationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataValidationServiceApplication.class, args);
    }
}
