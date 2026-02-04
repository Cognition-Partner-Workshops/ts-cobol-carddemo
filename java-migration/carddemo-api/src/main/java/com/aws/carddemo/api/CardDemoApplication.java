package com.aws.carddemo.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aws.carddemo")
@EntityScan(basePackages = "com.aws.carddemo.domain.entity")
@EnableJpaRepositories(basePackages = "com.aws.carddemo.domain.repository")
public class CardDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDemoApplication.class, args);
    }
}
