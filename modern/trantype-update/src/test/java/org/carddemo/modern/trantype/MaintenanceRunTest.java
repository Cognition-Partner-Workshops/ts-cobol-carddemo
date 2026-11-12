package org.carddemo.modern.trantype;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import static org.junit.jupiter.api.Assertions.*;

class MaintenanceRunTest {
    @TempDir Path temporary;
    JdbcTemplate jdbc;
    MaintenanceRun run;
    Path input;
    Path output;

    @BeforeEach
    void setup() throws Exception {
        var datasource = new DriverManagerDataSource("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo");
        new JdbcTemplate(datasource).execute("CREATE SCHEMA IF NOT EXISTS trantype_update_test");
        datasource.setUrl("jdbc:postgresql://db:5432/carddemo?currentSchema=trantype_update_test");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(datasource);
        jdbc = new JdbcTemplate(datasource);
        run = new MaintenanceRun(jdbc);
        input = Files.createDirectory(temporary.resolve("input"));
        output = temporary.resolve("output");
        Files.createDirectory(input.resolve("db"));
        Files.copy(Path.of("../../fixtures/trantype-update/t01-add/input/db/seed.sql"), input.resolve("db/seed.sql"));
    }

    @Test
    void appliesOperationsInFileOrderAndExportsRecorderContract() throws Exception {
        Files.writeString(input.resolve("INPFILE.txt"), "A04Fee\nU02New payment\nD03\n*00Comment\n");
        assertEquals(0, run.run(input, output));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM transaction_type", Integer.class));
        assertEquals("New payment", jdbc.queryForObject("SELECT tr_description FROM transaction_type WHERE tr_type='02'", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM transaction_type WHERE tr_type='03'", Integer.class));
        assertEquals("tr_type,tr_description\n01,Purchase\n02,New payment\n04,Fee\n",
                Files.readString(output.resolve("db/carddemo.transaction_type.csv")));
        assertEquals("trc_type_code,trc_type_category,trc_cat_data\n",
                Files.readString(output.resolve("db/carddemo.transaction_type_categor.csv")));
        assertEquals("{\n \"MNTTRDB2\": {\n  \"maxrc\": 0,\n  \"steps\": {\n   \"STEP1\": 0\n  }\n }\n}",
                Files.readString(output.resolve("rc.json")));
        assertTrue(Files.readString(output.resolve("sysout/MNTTRDB2.STEP1.txt")).contains("IGNORING COMMENTED LINE"));
    }

    @Test
    void emptyFilePreservesSeedAndProducesNoProcessingLines() throws Exception {
        Files.writeString(input.resolve("INPFILE.txt"), "");
        assertEquals(0, run.run(input, output));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM transaction_type", Integer.class));
        assertEquals("DSNRUN: DSN ATTACHED, RUNNING COBTUPDT\nOPEN FILE OK\nDSNRUN: COMMIT AT END OF TASK\n",
                Files.readString(output.resolve("sysout/MNTTRDB2.STEP1.txt")));
    }

    @Test
    void duplicateIsReportedAndSubsequentRecordsStillRun() throws Exception {
        Files.writeString(input.resolve("INPFILE.txt"), "A01Duplicate\nA04Fee\n");
        assertEquals(4, run.run(input, output));
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM transaction_type", Integer.class));
        assertTrue(Files.readString(output.resolve("sysout/MNTTRDB2.STEP1.txt")).contains("duplicate key"));
        assertTrue(Files.readString(output.resolve("rc.json")).contains("\"maxrc\": 4"));
    }

    @Test
    void missingTargetsAndInvalidOperationsAreErrors() throws Exception {
        Files.writeString(input.resolve("INPFILE.txt"), "D09\nU09Missing\nX01Invalid\n");
        assertEquals(4, run.run(input, output));
        var lines = Files.readString(output.resolve("sysout/MNTTRDB2.STEP1.txt"));
        assertTrue(lines.contains("No records found."));
        assertTrue(lines.contains("ERROR: TYPE NOT VALID"));
    }

    @Test
    void missingOrOversizedInputIsFatal() throws Exception {
        assertEquals(12, run.run(input, output));
        assertTrue(Files.readString(output.resolve("rc.json")).contains("\"STEP1\": 12"));
        Files.writeString(input.resolve("INPFILE.txt"), "A".repeat(54));
        assertEquals(12, run.run(input, output));
        assertTrue(Files.readString(output.resolve("sysout/MNTTRDB2.STEP1.txt")).contains("exceeds 53 bytes"));
    }
}
