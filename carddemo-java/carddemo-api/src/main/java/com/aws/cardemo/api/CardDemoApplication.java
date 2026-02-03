package com.aws.cardemo.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aws.cardemo")
@EntityScan(basePackages = "com.aws.cardemo.domain.entity")
@EnableJpaRepositories(basePackages = "com.aws.cardemo.persistence.repository")
public class CardDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDemoApplication.class, args);
    }
}
