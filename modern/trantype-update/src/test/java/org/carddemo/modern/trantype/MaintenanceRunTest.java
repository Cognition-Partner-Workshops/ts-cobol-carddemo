package org.carddemo.modern.trantype;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        assertEquals("New payment" + " ".repeat(39), jdbc.queryForObject("SELECT tr_description FROM transaction_type WHERE tr_type='02'", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM transaction_type WHERE tr_type='03'", Integer.class));
        assertEquals("tr_type,tr_description\n01,Purchase\n02,New payment" + " ".repeat(39) + "\n04,Fee" + " ".repeat(47) + "\n",
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
    void duplicateAbortsTheUnitOfWorkAndLaterSqlStillRunsInTheAbortedTransaction() throws Exception {
        Files.writeString(input.resolve("INPFILE.txt"), "A04Before error\nA01Duplicate\nA05After error\n");
        assertEquals(4, run.run(input, output));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM transaction_type", Integer.class));
        assertEquals(List.of("DSNRUN: DSN ATTACHED, RUNNING COBTUPDT", "OPEN FILE OK",
                "PROCESSING   A04" + String.format("%-50s", "Before error"), "ADDING RECORD", "RECORD INSERTED SUCCESSFULLY",
                "PROCESSING   A01" + String.format("%-50s", "Duplicate"), "ADDING RECORD",
                "Error accessing: TRANSACTION_TYPE table. SQLCODE: -403" + " ".repeat(26),
                "PROCESSING   A05" + String.format("%-50s", "After error"), "ADDING RECORD",
                "Error accessing: TRANSACTION_TYPE table. SQLCODE: -400" + " ".repeat(26), "DSNRUN: COMMIT AT END OF TASK"),
                Files.readAllLines(output.resolve("sysout/MNTTRDB2.STEP1.txt")));
        assertEquals("tr_type,tr_description\n01,Purchase\n02,Payment\n03,Credit\n",
                Files.readString(output.resolve("db/carddemo.transaction_type.csv")));
        assertEquals("{\n \"MNTTRDB2\": {\n  \"maxrc\": 4,\n  \"steps\": {\n   \"STEP1\": 4\n  }\n }\n}",
                Files.readString(output.resolve("rc.json")));
    }

    @Test
    void sqlstateMappingMatchesOcesqlAndCobolEditedReceiver() {
        assertEquals("Error accessing: TRANSACTION_TYPE table. SQLCODE: -403" + " ".repeat(26),
                MaintenanceRun.sqlErrorMessage("23505"));
        assertEquals(80, MaintenanceRun.sqlErrorMessage("23503").length());
        assertTrue(MaintenanceRun.sqlErrorMessage("23503").contains("SQLCODE: -400"));
    }

    @Test
    void missingDeleteAndUpdateRetainTheLegacySuccessDefect() throws Exception {
        Files.writeString(input.resolve("INPFILE.txt"), "D09\nU09Missing\n");
        assertEquals(0, run.run(input, output));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM transaction_type", Integer.class));
        var lines = Files.readString(output.resolve("sysout/MNTTRDB2.STEP1.txt"));
        assertTrue(lines.contains("RECORD DELETED SUCCESSFULLY"));
        assertTrue(lines.contains("RECORD UPDATED SUCCESSFULLY"));
        assertFalse(lines.contains("No records found."));
        assertTrue(Files.readString(output.resolve("rc.json")).contains("\"maxrc\": 0"));
    }

    @Test
    void invalidOperationsAreErrors() throws Exception {
        Files.writeString(input.resolve("INPFILE.txt"), "X01Invalid\n");
        assertEquals(4, run.run(input, output));
        var lines = Files.readAllLines(output.resolve("sysout/MNTTRDB2.STEP1.txt"));
        assertEquals("ERROR: TYPE NOT VALID" + " ".repeat(59), lines.get(3));
        assertTrue(Files.readString(output.resolve("rc.json")).contains("\"STEP1\": 4"));
    }

    @Test
    void missingInputMaterializesAsAnEmptyDataset() throws Exception {
        assertEquals(0, run.run(input, output));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM transaction_type", Integer.class));
        assertEquals("DSNRUN: DSN ATTACHED, RUNNING COBTUPDT\nOPEN FILE OK\nDSNRUN: COMMIT AT END OF TASK\n",
                Files.readString(output.resolve("sysout/MNTTRDB2.STEP1.txt")));
        assertTrue(Files.readString(output.resolve("rc.json")).contains("\"STEP1\": 0"));
    }

    @Test
    void oversizedInputIsClippedToTheMaterializedRecordLength() throws Exception {
        Files.writeString(input.resolve("INPFILE.txt"), "A04" + "C".repeat(60) + "\n*01Next record\n");
        assertEquals(0, run.run(input, output));
        assertEquals("C".repeat(50), jdbc.queryForObject("SELECT tr_description FROM transaction_type WHERE tr_type='04'", String.class));
        var lines = Files.readAllLines(output.resolve("sysout/MNTTRDB2.STEP1.txt"));
        assertEquals("PROCESSING   A04" + "C".repeat(50), lines.get(2));
        assertTrue(lines.contains("IGNORING COMMENTED LINE"));
    }
}
