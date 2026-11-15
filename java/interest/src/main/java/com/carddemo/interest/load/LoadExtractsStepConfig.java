package com.carddemo.interest.load;

import java.nio.file.Path;
import java.time.Clock;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * loadExtractsStep (architecture.md sections 2, 5 and 6): the first step of interestCalcJob while
 * the inputs stay on the mainframe. Restart repeats it. Job parameters read: parmDate, inputDir,
 * outputDir and format (architecture.md section 4) and, optional, rejectDir.
 */
@Configuration
public class LoadExtractsStepConfig {

    public static final String STEP_NAME = "loadExtractsStep";
    /** Job execution context: the interest_run.run_id of this run, for the steps that follow. */
    public static final String RUN_ID_KEY = "interest.runId";
    public static final String REJECT_DIR = "rejectDir";
    public static final String REJECT_FILE_NAME = STEP_NAME + ".rejects.txt";

    @Bean
    ExtractLoader extractLoader(JdbcTemplate jdbc, PlatformTransactionManager transactionManager, Clock runClock) {
        return new ExtractLoader(jdbc, transactionManager, runClock);
    }

    @Bean
    Step loadExtractsStep(JobRepository jobRepository, ExtractLoader loader) {
        Tasklet load = (contribution, chunkContext) -> {
            JobParameters parameters = contribution.getStepExecution().getJobParameters();
            LoadReport report = loader.load(new LoadRequest(
                    parameters.getString("parmDate"),
                    Path.of(parameters.getString("inputDir")),
                    ExtractFormat.ofParameter(parameters.getString("format")),
                    rejectFile(parameters)));
            contribution.getStepExecution().getJobExecution().getExecutionContext().putLong(RUN_ID_KEY, report.runId());
            report.figures().values().forEach(figures -> contribution.incrementWriteCount(figures.rowCount()));
            return RepeatStatus.FINISHED;
        };
        // The loader commits one table at a time itself (architecture.md section 5); the step adds no transaction.
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(load, new ResourcelessTransactionManager())
                .allowStartIfComplete(true)
                .build();
    }

    /** Beside the output directory, never in it: a stopped run leaves no output directory (standards S6.4). */
    private static Path rejectFile(JobParameters parameters) {
        String rejectDir = parameters.getString(REJECT_DIR);
        Path directory = rejectDir != null ? Path.of(rejectDir)
                : Path.of(parameters.getString("outputDir")).toAbsolutePath().resolveSibling("rejects");
        return directory.resolve(REJECT_FILE_NAME);
    }
}
