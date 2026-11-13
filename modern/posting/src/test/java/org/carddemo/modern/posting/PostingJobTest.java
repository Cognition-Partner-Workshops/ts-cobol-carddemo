package org.carddemo.modern.posting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class PostingJobTest {
    @TempDir Path temporary;

    @Test
    void rejectsContinueToTheReportButAbendsFailTheBatchJob() throws Exception {
        var datasource = new DriverManagerDataSource("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo");
        new JdbcTemplate(datasource).execute("CREATE SCHEMA IF NOT EXISTS posting_batch_test");
        try (var context = new SpringApplicationBuilder(PostingApplication.class).run(
                "--spring.datasource.url=jdbc:postgresql://db:5432/carddemo?currentSchema=posting_batch_test",
                "--spring.main.banner-mode=off")) {
            var launcher = context.getBean(JobLauncher.class);
            var job = context.getBean(Job.class);
            var rejected = launcher.run(job, parameters(Path.of("../../fixtures/posting/p02-invalid-card/input"),
                    temporary.resolve("rejects")));
            assertEquals(BatchStatus.COMPLETED, rejected.getStatus());
            assertEquals(2, rejected.getStepExecutions().size());
            assertEquals("RC4", rejected.getStepExecutions().stream()
                    .filter(step -> step.getStepName().equals("POSTTRAN.STEP15")).findFirst().orElseThrow().getExitStatus().getExitCode());

            var input = Files.createDirectory(temporary.resolve("input"));
            try (var files = Files.list(Path.of("../../fixtures/posting/p01-baseline-mixed/input"))) {
                for (var file : files.toList()) Files.copy(file, input.resolve(file.getFileName()));
            }
            Files.writeString(input.resolve(PostingRun.PREFIX + "TRANTYPE.PS.txt"), "");
            var failed = launcher.run(job, parameters(input, temporary.resolve("failed")));
            assertEquals(BatchStatus.FAILED, failed.getStatus());
            assertEquals(2, failed.getStepExecutions().size());
        }
    }

    private static org.springframework.batch.core.JobParameters parameters(Path input, Path output) throws Exception {
        var chains = Files.readString(Path.of("../../tools/runjcl/chains.json"));
        var clock = java.util.regex.Pattern.compile("\"clock\":\\s*\"([^\"]+)\"").matcher(chains);
        org.junit.jupiter.api.Assertions.assertTrue(clock.find());
        var jcl = Files.readString(Path.of("../../app/jcl/TRANREPT.jcl"));
        var start = java.util.regex.Pattern.compile("PARM-START-DATE,C'([^']+)'").matcher(jcl);
        var end = java.util.regex.Pattern.compile("PARM-END-DATE,C'([^']+)'").matcher(jcl);
        org.junit.jupiter.api.Assertions.assertTrue(start.find());
        org.junit.jupiter.api.Assertions.assertTrue(end.find());
        return new JobParametersBuilder().addString("input", input.toAbsolutePath().toString())
                .addString("output", output.toAbsolutePath().toString()).addString("clock", clock.group(1))
                .addString("sortStart", start.group(1)).addString("sortEnd", end.group(1))
                .addLong("run", System.nanoTime()).toJobParameters();
    }
}
