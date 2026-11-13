package org.carddemo.modern.posting;

import java.nio.file.Path;
import java.sql.DriverManager;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootApplication
public class PostingApplication {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: posting <input-directory> <output-directory>");
        try (var connection = DriverManager.getConnection("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo");
                var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS posting");
        }
        try (var context = SpringApplication.run(PostingApplication.class)) {
            var execution = context.getBean(JobLauncher.class).run(context.getBean(Job.class),
                    new JobParametersBuilder().addString("input", Path.of(args[0]).toAbsolutePath().toString())
                            .addString("output", Path.of(args[1]).toAbsolutePath().toString())
                            .addLong("run", System.nanoTime()).toJobParameters());
            if (execution.getStatus().isUnsuccessful() || execution.getExitStatus().getExitCode().equals("FAILED")) {
                throw new IllegalStateException("posting failed: " + execution.getExitStatus());
            }
        }
    }

    @Bean
    Job posting(JobRepository repository, PlatformTransactionManager transactionManager, JdbcTemplate jdbc) {
        var step = new StepBuilder("STEP15", repository).tasklet((contribution, chunk) -> {
            var parameters = contribution.getStepExecution().getJobParameters();
            int rc = new PostingRun(jdbc).run(Path.of(parameters.getString("input")),
                    Path.of(parameters.getString("output")));
            if (rc > 4) contribution.setExitStatus(new org.springframework.batch.core.ExitStatus("FAILED", "posting RC=" + rc));
            return RepeatStatus.FINISHED;
        }, transactionManager).build();
        return new JobBuilder("POSTTRAN-TRANREPT", repository).start(step).build();
    }
}
