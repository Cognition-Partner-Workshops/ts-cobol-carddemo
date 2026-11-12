package com.carddemo.modern.posting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** POSTTRAN and TRANREPT on Spring Batch and PostgreSQL. */
@SpringBootApplication
public class PostingApplication {
    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(PostingApplication.class, args)));
    }
}
