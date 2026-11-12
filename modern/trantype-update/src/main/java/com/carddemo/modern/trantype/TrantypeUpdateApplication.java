package com.carddemo.modern.trantype;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MNTTRDB2 (IKJEFT01 -> DSN RUN PROGRAM(COBTUPDT)) as a Spring Batch job on PostgreSQL.
 *
 * <pre>java -jar trantype-update.jar --case=fixtures/trantype-update/t01-add --out=work/parity/trantype-update/t01-add/java</pre>
 */
@SpringBootApplication
public class TrantypeUpdateApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(TrantypeUpdateApplication.class, args)));
    }
}
