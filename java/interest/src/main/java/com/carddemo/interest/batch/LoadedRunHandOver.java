package com.carddemo.interest.batch;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;

import com.carddemo.interest.load.LoadExtractsStepConfig;

/**
 * Joins loadExtractsStep to calcInterestStep: the loader leaves the interest_run row of the run in
 * the job execution context under its own key, the calculation and the export read it under
 * theirs. Before the calculation starts the id is handed over, so the three steps work on one row.
 *
 * <p>A value the calculation already put there stays: on a restart it is the run being continued.
 */
final class LoadedRunHandOver implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
        if (jobContext.containsKey(LoadExtractsStepConfig.RUN_ID_KEY)
                && !jobContext.containsKey(CalcInterestTableRun.INTEREST_RUN_ID)) {
            jobContext.putLong(CalcInterestTableRun.INTEREST_RUN_ID,
                    jobContext.getLong(LoadExtractsStepConfig.RUN_ID_KEY));
        }
    }
}
