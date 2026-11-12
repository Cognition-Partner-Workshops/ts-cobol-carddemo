package org.carddemo.modern.posting;

import java.nio.file.Path;
import java.sql.DriverManager;
import org.springframework.batch.core.*;
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
        if (args.length != 5) throw new IllegalArgumentException("usage: posting <input> <output> <clock> <sort-start> <sort-end>");
        try (var connection = DriverManager.getConnection("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo"); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS posting");
        }
        try (var context = SpringApplication.run(PostingApplication.class)) {
            var execution = context.getBean(JobLauncher.class).run(context.getBean(Job.class), new JobParametersBuilder()
                    .addString("input", Path.of(args[0]).toAbsolutePath().toString()).addString("output", Path.of(args[1]).toAbsolutePath().toString())
                    .addString("clock", args[2]).addString("sortStart", args[3]).addString("sortEnd", args[4]).addLong("run", System.nanoTime()).toJobParameters());
            if (execution.getStatus().isUnsuccessful()) throw new IllegalStateException("posting failed: " + execution.getExitStatus());
        }
    }
    @Bean Job posting(JobRepository repository, PlatformTransactionManager transactions, JdbcTemplate jdbc) {
        var step = new StepBuilder("POSTTRAN-TRANREPT", repository).tasklet((contribution, chunk) -> {
            var p = contribution.getStepExecution().getJobParameters();
            int rc = new PostingRun(jdbc).run(Path.of(p.getString("input")), Path.of(p.getString("output")), p.getString("clock"), p.getString("sortStart"), p.getString("sortEnd"));
            if (rc >= 12) contribution.setExitStatus(new ExitStatus("FAILED", "posting RC=" + rc));
            else if (rc != 0) contribution.setExitStatus(new ExitStatus("RC" + rc));
            return RepeatStatus.FINISHED;
        }, transactions).build();
        return new JobBuilder("posting", repository).start(step).build();
    }
}
