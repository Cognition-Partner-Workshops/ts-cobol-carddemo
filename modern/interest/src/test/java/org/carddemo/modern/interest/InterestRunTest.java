package org.carddemo.modern.interest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.carddemo.modern.codec.Layouts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class InterestRunTest {
    @TempDir Path temporary;
    private InterestRun run;
    private Path input;

    @BeforeEach
    void setUp() throws Exception {
        var datasource = new DriverManagerDataSource("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo");
        new JdbcTemplate(datasource).execute("CREATE SCHEMA IF NOT EXISTS interest_test");
        datasource.setUrl("jdbc:postgresql://db:5432/carddemo?currentSchema=interest_test");
        run = new InterestRun(new JdbcTemplate(datasource));
        input = Files.createDirectory(temporary.resolve("input"));
        try (var paths = Files.list(Path.of("../../fixtures/interest/i01-baseline/input"))) {
            for (var path : paths.toList()) Files.copy(path, input.resolve(path.getFileName()));
        }
    }

    @Test
    void halfEvenIncludesTiesAndNegativeBalances() {
        assertEquals(new BigDecimal("0.00"), InterestRun.monthlyInterest(new BigDecimal("1"), new BigDecimal("6")));
        assertEquals(new BigDecimal("0.02"), InterestRun.monthlyInterest(new BigDecimal("3"), new BigDecimal("6")));
        assertEquals(new BigDecimal("-0.02"), InterestRun.monthlyInterest(new BigDecimal("-3"), new BigDecimal("6")));
    }

    @Test
    void baselineUpdatesEveryAccountAndExportsRecords() throws Exception {
        var output = temporary.resolve("output");
        run.run(input, output);
        var accounts = Files.readAllLines(output.resolve("datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt"));
        assertEquals(2, accounts.size());
        for (var line : accounts) {
            var account = Layouts.ACCOUNT.decode(line);
            assertEquals(new BigDecimal("0.00"), account.get("ACCT-CURR-CYC-DEBIT"));
        }
        assertEquals(2, Files.readAllLines(output.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt")).size());
        assertTrue(Files.readString(output.resolve("sysout/INTCALC.STEP15.txt")).endsWith("END OF EXECUTION OF PROGRAM CBACT04C\n"));
        assertTrue(Files.readString(output.resolve("rc.json")).contains("\"maxrc\": 0"));
    }

    @Test
    void emptyBalancesProduceNoTransactions() throws Exception {
        Files.writeString(input.resolve("AWS.M2.CARDDEMO.TCATBALF.PS.txt"), "");
        var output = temporary.resolve("output");
        run.run(input, output);
        assertEquals(0, Files.size(output.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt")));
        assertEquals(2, Files.readAllLines(output.resolve("sysout/INTCALC.STEP15.txt")).size());
    }

    @Test
    void missingDisclosureFailsRatherThanInventingARate() throws Exception {
        Files.writeString(input.resolve("AWS.M2.CARDDEMO.DISCGRP.PS.txt"), "");
        assertThrows(IllegalStateException.class, () -> run.run(input, temporary.resolve("output")));
    }
}
