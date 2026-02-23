package com.carddemo.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CardDemo Integration module entry point.
 * Provides JMS/messaging capabilities replacing IBM MQ integration.
 *
 * This module can run standalone or be included as a dependency in the API module.
 */
@SpringBootApplication(scanBasePackages = "com.carddemo.integration")
public class CardDemoIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDemoIntegrationApplication.class, args);
    }
}
