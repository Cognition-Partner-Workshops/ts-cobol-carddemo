package com.carddemo.interest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** INTCALC (CBACT04C) on Spring Batch: monthly interest on transaction-category balances. */
@SpringBootApplication
public class InterestApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(InterestApplication.class, args)));
    }
}
