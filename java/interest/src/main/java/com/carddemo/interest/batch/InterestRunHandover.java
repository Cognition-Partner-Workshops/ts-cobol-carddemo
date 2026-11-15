package com.carddemo.interest.batch;

import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import com.carddemo.interest.export.ExportDatasetsTasklet;

/**
 * Joins exportDatasetsStep to the steps before it. loadExtractsStep leaves the key of the run's
 * carddemo.interest_run row in the job execution context as {@code interestRunId}, where
 * calcInterestStep reads it; exportDatasetsStep reads {@code interest.run.id}. This step is the
 * export step with the key copied first: same name, same restart settings, nothing else added.
 * A job execution context without the key is left as it is and the export refuses the run.
 */
final class InterestRunHandover implements Step {

    private final Step exportDatasetsStep;

    InterestRunHandover(Step exportDatasetsStep) {
        this.exportDatasetsStep = exportDatasetsStep;
    }

    @Override
    public String getName() {
        return exportDatasetsStep.getName();
    }

    @Override
    public boolean isAllowStartIfComplete() {
        return exportDatasetsStep.isAllowStartIfComplete();
    }

    @Override
    public int getStartLimit() {
        return exportDatasetsStep.getStartLimit();
    }

    @Override
    public void execute(StepExecution stepExecution) throws JobInterruptedException {
        ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
        if (jobContext.containsKey(CalcInterestTableRun.INTEREST_RUN_ID)) {
            jobContext.putLong(ExportDatasetsTasklet.RUN_ID_KEY, jobContext.getLong(CalcInterestTableRun.INTEREST_RUN_ID));
        }
        exportDatasetsStep.execute(stepExecution);
    }
}
