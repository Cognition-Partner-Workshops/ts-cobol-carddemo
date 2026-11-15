package com.carddemo.interest.batch;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;

import com.carddemo.interest.load.LoadExtractsStepConfig;

/**
 * Joins loadExtractsStep and calcInterestStep in one job (ticket 09): the load leaves the id of its
 * carddemo.interest_run row in the job execution context under its own key, the calculation looks
 * for the run under another. Before the calculation starts, the run of the load is put where the
 * calculation reads it, so the three steps of a job execution work on one row. A job without the
 * load step has no such entry and is left as it is: the calculation then creates its own row.
 */
final class LoadedRunHandOver implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
        if (jobContext.containsKey(LoadExtractsStepConfig.RUN_ID_KEY)
                && !jobContext.containsKey(JdbcCalcInterestRun.INTEREST_RUN_ID)) {
            jobContext.putLong(JdbcCalcInterestRun.INTEREST_RUN_ID, jobContext.getLong(LoadExtractsStepConfig.RUN_ID_KEY));
        }
    }
}
