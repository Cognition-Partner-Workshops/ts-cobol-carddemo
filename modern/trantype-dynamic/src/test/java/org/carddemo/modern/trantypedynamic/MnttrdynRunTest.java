package org.carddemo.modern.trantypedynamic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class MnttrdynRunTest {
    @TempDir Path temporary;
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private Path input;
    private Path output;
    private String parms;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new DriverManagerDataSource("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo");
        new JdbcTemplate(dataSource).execute("CREATE SCHEMA IF NOT EXISTS trantype_dynamic_test");
        dataSource.setUrl("jdbc:postgresql://db:5432/carddemo?currentSchema=trantype_dynamic_test");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        var jcl = Files.readString(Path.of("../../app/app-transaction-type-db2/jcl/MNTTRDYN.jcl"));
        var parm = java.util.regex.Pattern.compile("PARMS\\('([^']*)'\\)").matcher(jcl);
        assertTrue(parm.find());
        parms = parm.group(1);
        output = temporary.resolve("output");
    }

    private void useFixture(String fixture) throws Exception {
        input = temporary.resolve("input-" + fixture);
        var source = Path.of("../../fixtures/trantype-dynamic/" + fixture + "/input");
        try (var paths = Files.walk(source)) {
            for (var path : paths.toList()) {
                var target = input.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) Files.createDirectories(target);
                else Files.copy(path, target);
            }
        }
    }

    private int run() throws Exception {
        return new MnttrdynRun(dataSource).run(input, output, parms);
    }

    private List<String> sysout() throws Exception {
        return Files.readAllLines(output.resolve("sysout/MNTTRDYN.STEP1.txt"));
    }

    private List<String> descriptions() {
        return jdbc.queryForList("SELECT tr_type || '=' || tr_description FROM transaction_type ORDER BY tr_type", String.class);
    }

    @Test
    void dynamicUpdateAndCalledAddShareOneUnitOfWork() throws Exception {
        useFixture("d01-update-then-apply");
        assertEquals(0, run());
        assertEquals(List.of("01=Purchase", "02=Payment - SEPA", "03=Credit", "04=Authorization"), descriptions());
        var lines = sysout();
        assertEquals("CBTRNDYN: 03 SQL LINES READ", lines.get(1));
        assertEquals("CBTRNDYN: EXECUTE SQLCODE +000000000", lines.get(2));
        assertEquals("CBTRNDYN: CALLING COBTUPDT", lines.get(3));
        assertEquals("DSNRUN: COMMIT AT END OF TASK", lines.getLast());
        assertTrue(Files.readString(output.resolve("rc.json")).contains("\"STEP1\": 0"));
        assertEquals("tr_type,tr_description\n01,Purchase\n02,Payment - SEPA\n03,Credit\n04,Authorization\n",
                Files.readString(output.resolve("db/carddemo.transaction_type.csv")));
    }

    @Test
    void dynamicDeleteWithAnEmptyUpdateFile() throws Exception {
        useFixture("d02-delete-statement");
        assertEquals(0, run());
        assertEquals(List.of("01=Purchase", "02=Payment"), descriptions());
        assertEquals(List.of("OPEN FILE OK", "CBTRNDYN: END", "DSNRUN: COMMIT AT END OF TASK"), sysout().subList(4, 7));
        assertEquals("trc_type_code,trc_type_category,trc_cat_data\n",
                Files.readString(output.resolve("db/carddemo.transaction_type_categor.csv")));
    }

    @Test
    void failedDynamicStatementRollsBackOnlyItselfAndSetsRc8() throws Exception {
        useFixture("d03-bad-statement");
        assertEquals(8, run());
        assertEquals("CBTRNDYN: EXECUTE SQLCODE -000000206", sysout().get(2));
        assertTrue(sysout().contains("RECORD UPDATED SUCCESSFULLY"));
        assertEquals("01=Purchase - updated", descriptions().getFirst());
    }

    @Test
    void missingKeyOnUpdateReportsNotFoundAndRc4() throws Exception {
        useFixture("d02-delete-statement");
        Files.writeString(input.resolve("INPFILE.txt"), "U09Nothing\n");
        assertEquals(4, run());
        assertTrue(sysout().contains(String.format("%-80s", "No records found.")));
    }

    @Test
    void invalidRecordTypeSetsRc4AndContinues() throws Exception {
        useFixture("d02-delete-statement");
        Files.writeString(input.resolve("INPFILE.txt"), "X01\nA07Chargeback\n");
        assertEquals(4, run());
        var lines = sysout();
        assertTrue(lines.contains(String.format("%-80s", "ERROR: TYPE NOT VALID")));
        assertTrue(lines.contains("RECORD INSERTED SUCCESSFULLY"));
        assertTrue(descriptions().contains("07=Chargeback"));
    }

    @Test
    void noParmsMeansNoCall() throws Exception {
        useFixture("d01-update-then-apply");
        parms = "";
        assertEquals(0, run());
        assertFalse(sysout().stream().anyMatch(line -> line.startsWith("CBTRNDYN: CALLING")));
        assertFalse(descriptions().contains("04=Authorization"));
    }

    @Test
    void unknownProgramFailsTheStep() throws Exception {
        useFixture("d01-update-then-apply");
        parms = "NOSUCHPG";
        assertThrows(IllegalStateException.class, this::run);
    }

    @Test
    void statementAssemblySkipsCommentsAndKeepsColumns1To72Only() throws Exception {
        useFixture("d02-delete-statement");
        Files.writeString(input.resolve("DYNSQL.txt"), "-- note\nDELETE FROM TRANSACTION_TYPE\n" + " ".repeat(72) + "IGNORED\n WHERE TR_TYPE = '01'\n");
        assertEquals(0, run());
        assertEquals("CBTRNDYN: 03 SQL LINES READ", sysout().get(1));
        assertEquals(List.of("02=Payment", "03=Credit"), descriptions());
    }

    @Test
    void statementIsCutAtTheHostVariableCapacity() {
        var statement = new StringBuilder();
        for (int i = 0; i < 12; i++) Cbtrndyn.append(statement, "x".repeat(73));
        assertEquals(Cbtrndyn.STMT_CAPACITY, statement.length());
    }

    @Test
    void sqlcodeDisplayFormats() {
        assertEquals("+000000000", SqlCodes.display(0));
        assertEquals("-000000206", SqlCodes.display(-206));
        assertEquals(" -400", SqlCodes.edited(-400));
        assertEquals("    0", SqlCodes.edited(0));
        assertEquals("-1234", SqlCodes.edited(-1234));
    }
}
