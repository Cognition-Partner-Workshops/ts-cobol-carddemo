package com.carddemo.modern.interest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * INTCALC (CBACT04C) as a Spring Batch job on PostgreSQL.
 *
 * <pre>java -jar interest.jar --case=fixtures/interest/i01-baseline --out=work/parity/interest/i01-baseline/java</pre>
 */
@SpringBootApplication
public class InterestApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(InterestApplication.class, args)));
    }
}
