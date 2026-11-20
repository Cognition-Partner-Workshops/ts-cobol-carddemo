package com.carddemo.posting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PostingApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(PostingApplication.class, args)));
    }
}
