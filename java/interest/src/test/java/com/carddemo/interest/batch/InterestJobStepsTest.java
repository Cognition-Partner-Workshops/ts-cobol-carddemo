package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

import com.carddemo.interest.load.LoadExtractsStepConfig;

/** Ticket 09: which steps interestCalcJob has, and the hand-over of the run between load and calculation. */
class InterestJobStepsTest {

    private final InterestJobConfig config = new InterestJobConfig();

    @Test
    void allSteps_onTheFileAdapters_isRefused() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> config.interestCalcJob(null, null, null, null, "all", "file"));
        assertEquals("interest.job.steps=all needs interest.calc.store=jdbc: the load and the export work on the tables",
                refused.getMessage());
    }

    @Test
    void anotherValueOfSteps_isRefused() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> config.interestCalcJob(null, null, null, null, "load", "jdbc"));
        assertEquals("interest.job.steps must be calc or all", refused.getMessage());
    }

    @Test
    void handOver_putsTheRunOfTheLoadWhereTheCalculationReadsIt() {
        StepExecution calc = new StepExecution(InterestJobConfig.CALC_STEP_NAME, new JobExecution(1L));
        calc.getJobExecution().getExecutionContext().putLong(LoadExtractsStepConfig.RUN_ID_KEY, 42L);

        new LoadedRunHandOver().beforeStep(calc);

        assertEquals(42L, calc.getJobExecution().getExecutionContext().getLong(JdbcCalcInterestRun.INTEREST_RUN_ID));
    }

    @Test
    void handOver_keepsTheRunOfARestartedCalculation() {
        // a restart carries the job context of the failed execution: the calculation's run stays
        StepExecution calc = new StepExecution(InterestJobConfig.CALC_STEP_NAME, new JobExecution(1L));
        calc.getJobExecution().getExecutionContext().putLong(LoadExtractsStepConfig.RUN_ID_KEY, 43L);
        calc.getJobExecution().getExecutionContext().putLong(JdbcCalcInterestRun.INTEREST_RUN_ID, 42L);

        new LoadedRunHandOver().beforeStep(calc);

        assertEquals(42L, calc.getJobExecution().getExecutionContext().getLong(JdbcCalcInterestRun.INTEREST_RUN_ID));
    }

    @Test
    void handOver_withoutALoadStep_leavesTheJobContextAsItIs() {
        StepExecution calc = new StepExecution(InterestJobConfig.CALC_STEP_NAME, new JobExecution(1L));

        new LoadedRunHandOver().beforeStep(calc);

        assertFalse(calc.getJobExecution().getExecutionContext().containsKey(JdbcCalcInterestRun.INTEREST_RUN_ID));
    }
}
