package com.carddemo.interest.export;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.carddemo.interest.batch.InterestJobParameters;
import com.carddemo.interest.load.ExtractFormat;
import com.carddemo.interest.load.LoadExtractsStepConfig;

/**
 * The step {@code exportDatasetsStep} of {@code interestCalcJob} (architecture: the whole export is
 * one unit; a restart repeats it; it changes no business table). It exports the run whose id
 * {@code loadExtractsStep} left in the job execution context, or the newest CALCULATED run when the
 * job was launched without that step.
 */
@Configuration
public class ExportDatasetsStepConfig {

    public static final String STEP_NAME = "exportDatasetsStep";
    private static final Logger LOG = LoggerFactory.getLogger(ExportDatasetsStepConfig.class);

    @Bean
    Step exportDatasetsStep(JobRepository jobRepository, DatasetExporter exporter) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            JobParameters parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
            ExecutionContext jobContext = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
            long runId = jobContext.containsKey(LoadExtractsStepConfig.RUN_ID_KEY)
                    ? jobContext.getLong(LoadExtractsStepConfig.RUN_ID_KEY)
                    : exporter.newestCalculatedRun();
            ExportResult result = exporter.export(runId, Path.of(parameters.getString(InterestJobParameters.OUTPUT_DIR)),
                    ExtractFormat.of(parameters.getString(InterestJobParameters.FORMAT)));
            contribution.incrementWriteCount(result.transactionCount() + result.accountCount());
            LOG.info("exportDatasetsStep: interest_run {} EXPORTED, {} transactions, {} accounts",
                    result.runId(), result.transactionCount(), result.accountCount());
            return RepeatStatus.FINISHED;
        };
        // The exporter reads in one read-only transaction and updates interest_run in a second one; no skip, no retry (S6.2).
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(tasklet, new ResourcelessTransactionManager())
                .build();
    }
}
