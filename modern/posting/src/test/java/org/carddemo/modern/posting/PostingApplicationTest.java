package org.carddemo.modern.posting;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class PostingApplicationTest {
    @TempDir Path temporary;
    @Test void abendFailsBothTheBatchStatusAndProcessWhileRcFourRemainsSuccessful() throws Exception {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo"));
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS posting_application_test");
        var input = temporary.resolve("input"); Files.createDirectory(input);
        try (var paths = Files.list(Path.of("../../fixtures/posting/p01-baseline-mixed/input"))) {
            for (var p : paths.toList()) Files.copy(p, input.resolve(p.getFileName()));
        }
        Files.delete(input.resolve(PostingRun.PREFIX + "TRANTYPE.PS.txt"));
        assertNotEquals(0, launch(input, "abend"));
        assertEquals("FAILED", jdbc.queryForObject("SELECT status FROM posting_application_test.batch_job_execution ORDER BY job_execution_id DESC LIMIT 1", String.class));
        assertTrue(Files.readString(temporary.resolve("abend/rc.json")).contains("\"STEP10R\": 12"));
        assertEquals(0, launch(Path.of("../../fixtures/posting/p02-invalid-card" + "/input").toAbsolutePath(), "reject"));
        assertEquals("COMPLETED", jdbc.queryForObject("SELECT status FROM posting_application_test.batch_job_execution ORDER BY job_execution_id DESC LIMIT 1", String.class));
        assertTrue(Files.readString(temporary.resolve("reject/rc.json")).contains("\"STEP15\": 4"));
    }
    private int launch(Path input, String name) throws Exception {
        var log = temporary.resolve(name + ".log");
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                PostingApplication.class.getName(), input.toString(), temporary.resolve(name).toString(),
                "2022-07-05 10:00:00.00", "2022-01-01", "2022-07-06");
        process.environment().put("SPRING_DATASOURCE_URL", "jdbc:postgresql://db:5432/carddemo?currentSchema=posting_application_test");
        var child = process.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean done = child.waitFor(30, TimeUnit.SECONDS);
        if (!done) child.destroyForcibly();
        assertTrue(done, "posting main timed out");
        int rc = child.exitValue();
        if (name.equals("reject")) assertEquals(0, rc, Files.readString(log));
        return rc;
    }
}
