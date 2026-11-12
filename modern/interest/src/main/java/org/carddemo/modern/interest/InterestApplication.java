package org.carddemo.modern.interest;

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
public class InterestApplication {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("usage: interest <input-directory> <output-directory> <parm-date> <clock>");
        try (var connection = DriverManager.getConnection("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo");
                var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS interest");
        }
        try (var context = SpringApplication.run(InterestApplication.class)) {
            var execution = context.getBean(JobLauncher.class).run(context.getBean(Job.class),
                    new JobParametersBuilder().addString("input", Path.of(args[0]).toAbsolutePath().toString())
                            .addString("output", Path.of(args[1]).toAbsolutePath().toString())
                            .addString("parmDate", args[2])
                            .addString("clock", args[3])
                            .addLong("run", System.nanoTime()).toJobParameters());
            if (execution.getStatus().isUnsuccessful() || execution.getExitStatus().getExitCode().equals("FAILED")) {
                throw new IllegalStateException("INTCALC failed: " + execution.getExitStatus());
            }
        }
    }

    @Bean
    Job interest(JobRepository repository, PlatformTransactionManager transactionManager, JdbcTemplate jdbc) {
        var step = new StepBuilder("STEP15", repository).tasklet((contribution, chunk) -> {
            var parameters = contribution.getStepExecution().getJobParameters();
            int rc = new InterestRun(jdbc).run(Path.of(parameters.getString("input")),
                    Path.of(parameters.getString("output")), parameters.getString("parmDate"), parameters.getString("clock"));
            if (rc != 0) contribution.setExitStatus(new org.springframework.batch.core.ExitStatus("FAILED", "CBACT04C RC=" + rc));
            return RepeatStatus.FINISHED;
        }, transactionManager).build();
        return new JobBuilder("INTCALC", repository).start(step).build();
    }
}
