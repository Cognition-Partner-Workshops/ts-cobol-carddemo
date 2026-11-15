package com.carddemo.interest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Launches interestCalcJob. Job parameters are the non-option arguments:
 * {@code parmDate=2022071800 runId=<new per launch> inputDir=<dir> outputDir=<dir> format=fixture}.
 */
@SpringBootApplication
public class InterestApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(InterestApplication.class, args)));
    }
}
