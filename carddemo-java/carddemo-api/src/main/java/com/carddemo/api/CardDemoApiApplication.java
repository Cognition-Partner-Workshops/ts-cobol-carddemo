package com.carddemo.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * CardDemo API Application entry point.
 * Spring Boot 3.x application replacing the CICS region for online transactions.
 *
 * Scans:
 *   - com.carddemo.api: Controllers, services, DTOs, security
 *   - com.carddemo.core.domain: JPA entities
 *   - com.carddemo.core.repository: Spring Data JPA repositories
 */
@SpringBootApplication(scanBasePackages = {
        "com.carddemo.api",
        "com.carddemo.core"
})
@EntityScan(basePackages = "com.carddemo.core.domain")
@EnableJpaRepositories(basePackages = "com.carddemo.core.repository")
public class CardDemoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDemoApiApplication.class, args);
    }
}
