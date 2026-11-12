package org.carddemo.modern.trantypedynamic;

import java.nio.file.Path;
import java.sql.DriverManager;
import javax.sql.DataSource;
import org.springframework.batch.core.ExitStatus;
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
import org.springframework.transaction.PlatformTransactionManager;

/** MNTTRDYN STEP1: {@code DSN RUN PROGRAM(CBTRNDYN) PARMS('...')} as a Spring Batch job. */
@SpringBootApplication
public class TrantypeDynamicApplication {
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("usage: trantype-dynamic <input-directory> <output-directory> [<parms>]");
        }
        try (var connection = DriverManager.getConnection("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo");
                var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS trantype_dynamic");
        }
        try (var context = SpringApplication.run(TrantypeDynamicApplication.class)) {
            var execution = context.getBean(JobLauncher.class).run(context.getBean(Job.class),
                    new JobParametersBuilder().addString("input", Path.of(args[0]).toAbsolutePath().toString())
                            .addString("output", Path.of(args[1]).toAbsolutePath().toString())
                            .addString("parms", args.length == 3 ? args[2] : "")
                            .addLong("run", System.nanoTime()).toJobParameters());
            if (execution.getStatus().isUnsuccessful()) {
                throw new IllegalStateException("MNTTRDYN failed: " + execution.getExitStatus());
            }
        }
    }

    @Bean
    Job mnttrdyn(JobRepository repository, PlatformTransactionManager transactionManager, DataSource dataSource) {
        var step = new StepBuilder(MnttrdynRun.STEP, repository).tasklet((contribution, chunk) -> {
            var parameters = contribution.getStepExecution().getJobParameters();
            int rc = new MnttrdynRun(dataSource).run(Path.of(parameters.getString("input")),
                    Path.of(parameters.getString("output")), parameters.getString("parms"));
            // A condition code is a normal step outcome; it is recorded in rc.json and the exit status.
            if (rc != 0) contribution.setExitStatus(new ExitStatus("RC" + rc, MnttrdynRun.JOB + " " + MnttrdynRun.STEP + " RC=" + rc));
            return RepeatStatus.FINISHED;
        }, transactionManager).build();
        return new JobBuilder(MnttrdynRun.JOB, repository).start(step).build();
    }
}
