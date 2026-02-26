package com.carddemo.validation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "validation.enabled=false",
    "validation.table-pairs[0].name=test",
    "validation.table-pairs[0].source-datasource=test-source",
    "validation.table-pairs[0].source-table=test_source_table",
    "validation.table-pairs[0].target-datasource=test-target",
    "validation.table-pairs[0].target-table=test_target_table",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class DataValidationServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts successfully.
    }
}
