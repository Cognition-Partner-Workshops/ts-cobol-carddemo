package com.carddemo.interest.export;

import java.nio.file.Path;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * The work of exportDatasetsStep (architecture.md sections 4 and 5): one export of the run, in the
 * step's transaction. A restart repeats it (section 6).
 *
 * <p>It takes {@code outputDir} and {@code format} from the job parameters and the key of the
 * run's interest_run row from the job execution context under {@link #INTEREST_RUN_ID}, where the
 * step that creates the row puts it. The step itself is declared with the job, in package batch.
 */
public class ExportDatasetsTasklet implements Tasklet {

    /** Job execution context key of interest_run.run_id (a long). */
    public static final String INTEREST_RUN_ID = "interestRunId";
    public static final String OUTPUT_DIR = "outputDir";
    public static final String FORMAT = "format";

    private final DatasetExporter exporter;

    public ExportDatasetsTasklet(DatasetExporter exporter) {
        this.exporter = exporter;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var jobExecution = chunkContext.getStepContext().getStepExecution().getJobExecution();
        if (!jobExecution.getExecutionContext().containsKey(INTEREST_RUN_ID)) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT,
                    "no " + INTEREST_RUN_ID + " in the job execution context; nothing is exported");
        }
        long runId = jobExecution.getExecutionContext().getLong(INTEREST_RUN_ID);
        var parameters = jobExecution.getJobParameters();
        ExportTotals totals = exporter.export(runId, Path.of(parameters.getString(OUTPUT_DIR)),
                ExportFormat.ofParameter(parameters.getString(FORMAT)));
        contribution.incrementWriteCount(totals.transactionCount() + totals.accountCount());
        return RepeatStatus.FINISHED;
    }
}
