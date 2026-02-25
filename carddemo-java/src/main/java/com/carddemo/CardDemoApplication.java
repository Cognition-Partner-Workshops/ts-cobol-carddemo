package com.carddemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CardDemo Application - Migrated from COBOL/CICS/VSAM mainframe to Java 17 + Spring Boot.
 *
 * This application provides credit card management functionality including:
 * - Account management (view, update)
 * - Credit card management (list, view, update)
 * - Transaction processing (list, view, add)
 * - Bill payment
 * - User administration
 * - Batch processing (statement generation, interest calculation, etc.)
 * - Authorization processing via MQ
 */
@SpringBootApplication
@EnableScheduling
public class CardDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDemoApplication.class, args);
    }
}
