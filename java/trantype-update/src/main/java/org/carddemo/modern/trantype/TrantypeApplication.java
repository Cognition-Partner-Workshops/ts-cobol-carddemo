package org.carddemo.modern.trantype;

import javax.sql.DataSource;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;

@SpringBootApplication
public class TrantypeApplication {
    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(TrantypeApplication.class, args)));
    }

    @Bean
    public UpdateProgram program() {
        return new UpdateProgram(System.out::println);
    }

    @Bean
    public Job updateJob(JobRepository repository, PlatformTransactionManager transactions,
                         DataSource dataSource, UpdateProgram program) {
        var step = new StepBuilder("STEP1", repository).tasklet((contribution, context) -> {
            try (var connection = dataSource.getConnection()) {
                program.run(Path.of(System.getenv("DD_INPFILE")), connection);
            }
            return RepeatStatus.FINISHED;
        }, transactions).build();
        return new JobBuilder("MNTTRDB2", repository).start(step).build();
    }

    @Bean
    public ApplicationRunner runner(JobLauncher launcher, Job updateJob, UpdateProgram program,
                                     org.springframework.context.ConfigurableApplicationContext context) {
        return args -> {
            var execution = launcher.run(updateJob, new JobParametersBuilder().toJobParameters());
            if (execution.getStatus() != BatchStatus.COMPLETED) {
                throw new IllegalStateException("MNTTRDB2 failed: " + execution.getAllFailureExceptions());
            }
            context.getBeanFactory().registerSingleton("programExitCode",
                    (org.springframework.boot.ExitCodeGenerator) program::returnCode);
        };
    }
}
