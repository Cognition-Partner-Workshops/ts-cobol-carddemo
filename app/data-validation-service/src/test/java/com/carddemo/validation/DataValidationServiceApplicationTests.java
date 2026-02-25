package com.carddemo.validation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.postgres.url=jdbc:h2:mem:testdb",
    "spring.datasource.postgres.driver-class-name=org.h2.Driver",
    "spring.datasource.legacy.url=jdbc:h2:mem:legacydb",
    "spring.datasource.legacy.driver-class-name=org.h2.Driver",
    "validation.mainframe-output-dir=/tmp/test-mainframe",
    "validation.api-base-url=http://localhost:9999"
})
class DataValidationServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the application context loads successfully
    }
}
