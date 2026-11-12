package org.carddemo.modern.trantype;

import java.nio.file.Path;
import java.sql.DriverManager;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.ExitStatus;
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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

@SpringBootApplication
public class TrantypeApplication {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("usage: trantype-update <input-directory> <output-directory> <clock>");
        java.time.LocalDateTime.parse(args[2], java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"));
        try (var connection = DriverManager.getConnection("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo");
                var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS trantype_update");
        }
        try (var context = SpringApplication.run(TrantypeApplication.class)) {
            var execution = context.getBean(JobLauncher.class).run(context.getBean(Job.class),
                    new JobParametersBuilder().addString("input", Path.of(args[0]).toAbsolutePath().toString())
                            .addString("output", Path.of(args[1]).toAbsolutePath().toString())
                            .addString("clock", args[2]).addLong("run", System.nanoTime()).toJobParameters());
            if (execution.getStatus().isUnsuccessful()) throw new IllegalStateException("MNTTRDB2 failed: " + execution.getExitStatus());
        }
    }

    @Bean
    Job maintenance(JobRepository repository, PlatformTransactionManager transactions, JdbcTemplate jdbc) {
        var attribute = new DefaultTransactionAttribute(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        var step = new StepBuilder("STEP1", repository).tasklet((contribution, chunk) -> {
            var parameters = contribution.getStepExecution().getJobParameters();
            int rc = new MaintenanceRun(jdbc).run(Path.of(parameters.getString("input")), Path.of(parameters.getString("output")));
            if (rc >= 12) throw new IllegalStateException("COBTUPDT RC=" + rc);
            if (rc != 0) contribution.setExitStatus(new ExitStatus("COMPLETED_WITH_ERRORS", "COBTUPDT RC=" + rc));
            return RepeatStatus.FINISHED;
        }, transactions).transactionAttribute(attribute).build();
        return new JobBuilder("MNTTRDB2", repository).start(step).build();
    }
}
