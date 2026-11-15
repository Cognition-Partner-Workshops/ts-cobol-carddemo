package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

import com.carddemo.interest.load.LoadExtractsStepConfig;

/** The interest_run row of the loader reaches the calculation and the export (ticket 09). */
class LoadedRunHandOverTest {

    private final JobExecution job = new JobExecution(1L);
    private final StepExecution calc = new StepExecution(InterestJobConfig.CALC_STEP_NAME, job);

    @Test
    void loadedRunIsHandedToTheCalculation() {
        job.getExecutionContext().putLong(LoadExtractsStepConfig.RUN_ID_KEY, 7L);

        new LoadedRunHandOver().beforeStep(calc);

        assertEquals(7L, job.getExecutionContext().getLong(CalcInterestTableRun.INTEREST_RUN_ID));
    }

    @Test
    void runOfARestartedCalculationStays() {
        job.getExecutionContext().putLong(LoadExtractsStepConfig.RUN_ID_KEY, 8L);
        job.getExecutionContext().putLong(CalcInterestTableRun.INTEREST_RUN_ID, 7L);

        new LoadedRunHandOver().beforeStep(calc);

        assertEquals(7L, job.getExecutionContext().getLong(CalcInterestTableRun.INTEREST_RUN_ID));
    }

    @Test
    void withoutALoadStepNothingIsPut() {
        new LoadedRunHandOver().beforeStep(calc);

        assertFalse(job.getExecutionContext().containsKey(CalcInterestTableRun.INTEREST_RUN_ID));
    }
}
