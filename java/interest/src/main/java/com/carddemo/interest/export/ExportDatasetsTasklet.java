package com.carddemo.interest.export;

import java.nio.file.Path;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * exportDatasetsStep as a tasklet: the whole export is one unit of work (architecture.md section 5)
 * and a restart repeats it (section 6). It takes {@code outputDir} and {@code format} from the job
 * parameters and the key of the run's interest_run row from the job execution context, where the
 * step that created the row has to leave it under {@link #RUN_ID_KEY}: the job parameter
 * {@code runId} is text and interest_run.run_id is a generated number.
 */
public final class ExportDatasetsTasklet implements Tasklet {

    public static final String STEP_NAME = "exportDatasetsStep";
    /** Job execution context: the interest_run.run_id of this job execution, a Long. */
    public static final String RUN_ID_KEY = "interest.run.id";
    static final String OUTPUT_DIR = "outputDir";
    static final String FORMAT = "format";

    private final DatasetExport export;

    public ExportDatasetsTasklet(DatasetExport export) {
        this.export = export;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        JobExecution job = contribution.getStepExecution().getJobExecution();
        if (!job.getExecutionContext().containsKey(RUN_ID_KEY)) {
            throw new ExportRefusedException("the job execution context has no " + RUN_ID_KEY);
        }
        DatasetExport.Exported exported = export.export(
                job.getExecutionContext().getLong(RUN_ID_KEY),
                Path.of(job.getJobParameters().getString(OUTPUT_DIR)),
                ExportFormat.of(job.getJobParameters().getString(FORMAT)));
        contribution.incrementWriteCount(exported.transactionCount());
        return RepeatStatus.FINISHED;
    }
}
