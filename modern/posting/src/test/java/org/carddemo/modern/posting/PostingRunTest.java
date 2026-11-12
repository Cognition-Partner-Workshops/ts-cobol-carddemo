package org.carddemo.modern.posting;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import org.carddemo.modern.codec.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class PostingRunTest {
    @TempDir Path temporary;
    JdbcTemplate jdbc;
    PostingRun run;
    @BeforeEach void setup() {
        var ds = new DriverManagerDataSource("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo");
        new JdbcTemplate(ds).execute("CREATE SCHEMA IF NOT EXISTS posting_test");
        ds.setUrl("jdbc:postgresql://db:5432/carddemo?currentSchema=posting_test");
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new org.springframework.core.io.ClassPathResource("schema.sql")).execute(ds);
        jdbc = new JdbcTemplate(ds); run = new PostingRun(jdbc);
    }
    Path fixture(String name) { return Path.of("../../fixtures/posting", name, "input"); }
    int execute(Path input, Path output) throws Exception { return run.run(input, output, "2022-07-05 10:00:00.00", "2022-01-01", "2022-07-06"); }
    @Test void eofRepeatsLastAmountAndDisplaysPreAdditionPageTotal() throws Exception {
        Path out = temporary.resolve("out"); assertEquals(0, execute(fixture("p01-baseline-mixed"), out));
        var report = Files.readAllLines(out.resolve("datasets/" + PostingRun.PREFIX + "TRANREPT.G0001V00.txt"));
        assertTrue(report.get(10).endsWith("-         26.25"));
        assertTrue(report.get(12).endsWith("-         26.25"));
        var sysout = Files.readAllLines(out.resolve("sysout/TRANREPT.STEP10R.txt"));
        assertTrue(sysout.contains("TRAN-AMT 00000001575-"));
        assertTrue(sysout.contains("WS-PAGE-TOTAL00000001050-"));
    }
    @Test void emptyDayHasNoDetailOrTotalLines() throws Exception {
        Path out = temporary.resolve("out"); assertEquals(0, execute(fixture("p07-empty-day"), out));
        assertEquals("", Files.readString(out.resolve("datasets/" + PostingRun.PREFIX + "TRANREPT.G0001V00.txt")));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM posted_transaction", Integer.class));
    }
    @Test void missingReportLookupProducesDiagnosticsAndRcTwelve() throws Exception {
        var input = temporary.resolve("input"); Files.createDirectory(input);
        try (var paths = Files.list(fixture("p01-baseline-mixed"))) {
            for (var p : paths.toList()) Files.copy(p, input.resolve(p.getFileName()));
        }
        Files.delete(input.resolve(PostingRun.PREFIX + "TRANTYPE.PS.txt"));
        var output = temporary.resolve("out"); assertEquals(12, execute(input, output));
        var sysout = Files.readString(output.resolve("sysout/TRANREPT.STEP10R.txt"));
        assertTrue(sysout.contains("INVALID TRANSACTION TYPE : 01\nFILE STATUS IS: NNNN0023\nABENDING PROGRAM\nCEE3ABD: USER ABEND U+0000000999"));
        assertTrue(Files.readString(output.resolve("rc.json")).contains("\"STEP10R\": 12"));
    }
}
