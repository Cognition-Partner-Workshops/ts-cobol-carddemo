package org.carddemo.modern.posting;

import java.nio.file.Path;
import java.sql.DriverManager;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootApplication
public class PostingApplication {
    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException("usage: posting <input-directory> <output-directory> <clock> <sort-start-date> <sort-end-date>");
        }
        try (var connection = DriverManager.getConnection("jdbc:postgresql://db:5432/carddemo", "carddemo", "carddemo");
                var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS posting");
        }
        try (var context = SpringApplication.run(PostingApplication.class)) {
            var execution = context.getBean(JobLauncher.class).run(context.getBean(Job.class),
                    new JobParametersBuilder().addString("input", Path.of(args[0]).toAbsolutePath().toString())
                            .addString("output", Path.of(args[1]).toAbsolutePath().toString())
                            .addString("clock", args[2]).addString("sortStart", args[3]).addString("sortEnd", args[4])
                            .addLong("run", System.nanoTime()).toJobParameters());
            if (execution.getStatus().isUnsuccessful()) throw new IllegalStateException("posting failed: " + execution.getExitStatus());
        }
    }

    @Bean
    Job posting(JobRepository repository, PlatformTransactionManager transactionManager, JdbcTemplate jdbc) {
        var post = new StepBuilder("POSTTRAN.STEP15", repository).tasklet((contribution, chunk) -> {
            var step = contribution.getStepExecution();
            var parameters = step.getJobParameters();
            int rc = new PostingRun(jdbc).post(Path.of(parameters.getString("input")),
                    Path.of(parameters.getString("output")), parameters.getString("clock"));
            step.getJobExecution().getExecutionContext().putInt("postingRc", rc);
            if (rc != 0) contribution.setExitStatus(new ExitStatus("RC" + rc, "CBTRN02C RC=" + rc));
            return RepeatStatus.FINISHED;
        }, transactionManager).build();
        var report = new StepBuilder("TRANREPT.STEP10R", repository).tasklet((contribution, chunk) -> {
            var step = contribution.getStepExecution();
            var parameters = step.getJobParameters();
            var output = Path.of(parameters.getString("output"));
            int rc = new PostingRun(jdbc).report(output, parameters.getString("sortStart"), parameters.getString("sortEnd"));
            int postingRc = step.getJobExecution().getExecutionContext().getInt("postingRc");
            PostingRun.writeReturnCodes(output, postingRc, rc);
            if (Math.max(postingRc, rc) >= 12) contribution.setExitStatus(new ExitStatus("FAILED", "posting RC=" + postingRc + ", report RC=" + rc));
            return RepeatStatus.FINISHED;
        }, transactionManager).build();
        return new JobBuilder("posting", repository).start(post).on("*").to(report)
                .from(report).on("FAILED").fail().from(report).on("*").end().end().build();
    }
}
