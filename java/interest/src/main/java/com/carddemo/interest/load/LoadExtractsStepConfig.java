package com.carddemo.interest.load;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.carddemo.interest.batch.InterestJobParameters;

/**
 * loadExtractsStep, the first step of interestCalcJob (architecture, section Batch job boundary).
 * It reads the job parameters parmDate, inputDir, outputDir and format, and leaves the id of the
 * interest_run row in the job's execution context for the steps after it.
 */
@Configuration
public class LoadExtractsStepConfig {

    public static final String STEP_NAME = "loadExtractsStep";
    /** Key of the interest_run id (a long) in the job execution context. */
    public static final String RUN_ID_KEY = "interest.run.id";
    /** The reject list is written beside the output directory, never inside it: {@code <outputDir>.load-rejects.txt}. */
    public static final String REJECT_LIST_SUFFIX = ".load-rejects.txt";

    private static final Logger LOG = LoggerFactory.getLogger(LoadExtractsStepConfig.class);

    public static Path rejectList(Path outputDir) {
        return outputDir.resolveSibling(outputDir.getFileName() + REJECT_LIST_SUFFIX);
    }

    @Bean
    Step loadExtractsStep(JobRepository jobRepository, ExtractLoader loader) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            JobParameters parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
            LoadResult result = loader.load(Path.of(parameters.getString(InterestJobParameters.INPUT_DIR)),
                    ExtractFormat.of(parameters.getString(InterestJobParameters.FORMAT)), parameters.getString(InterestJobParameters.PARM_DATE),
                    rejectList(Path.of(parameters.getString(InterestJobParameters.OUTPUT_DIR))));
            chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext()
                    .putLong(RUN_ID_KEY, result.runId());
            contribution.incrementWriteCount(result.roundTrip().records());
            LOG.info("loadExtractsStep: interest_run {} LOADED, {} records", result.runId(), result.roundTrip().records());
            return RepeatStatus.FINISHED;
        };
        // The loader owns its transactions, one per table (architecture, section Transaction boundaries);
        // the step itself holds none, so no table load is part of a larger one. No skip, no retry (S6.2).
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(tasklet, new ResourcelessTransactionManager())
                .build();
    }
}
