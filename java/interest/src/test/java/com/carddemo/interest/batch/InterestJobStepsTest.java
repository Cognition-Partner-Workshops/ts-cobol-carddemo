package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;

import com.carddemo.interest.batch.InterestJobConfig.CalcStore;

/** The assembly of interestCalcJob: which steps it has, and how the export gets the run of the load. */
class InterestJobStepsTest {

    /** A step that records what the job execution context held when it ran. */
    private static final class RecordingStep implements Step {

        private final String name;
        private final List<String> seen = new ArrayList<>();

        RecordingStep(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isAllowStartIfComplete() {
            return true;
        }

        @Override
        public int getStartLimit() {
            return 3;
        }

        @Override
        public void execute(StepExecution stepExecution) {
            ExecutionContext context = stepExecution.getJobExecution().getExecutionContext();
            seen.add(context.containsKey("interest.run.id") ? "interest.run.id=" + context.getLong("interest.run.id")
                    : "no interest.run.id");
        }
    }

    /** Never called: the tests build the job and do not launch it. */
    private static final JobRepository REPOSITORY = Mockito.mock(JobRepository.class);

    private static StepExecution stepExecution() {
        return new StepExecution("exportDatasetsStep", new JobExecution(1L));
    }

    @Test
    void handover_givesTheExportTheRunKeyOfTheLoad() throws Exception {
        RecordingStep export = new RecordingStep("exportDatasetsStep");
        StepExecution execution = stepExecution();
        execution.getJobExecution().getExecutionContext().putLong("interestRunId", 42L);

        new InterestRunHandover(export).execute(execution);

        assertEquals(List.of("interest.run.id=42"), export.seen);
    }

    @Test
    void handover_withoutARunKeyLeavesTheRefusalToTheExport() throws Exception {
        RecordingStep export = new RecordingStep("exportDatasetsStep");

        new InterestRunHandover(export).execute(stepExecution());

        assertEquals(List.of("no interest.run.id"), export.seen);
    }

    @Test
    void handover_isTheExportStepToTheJob() {
        Step handover = new InterestRunHandover(new RecordingStep("exportDatasetsStep"));

        assertEquals("exportDatasetsStep", handover.getName());
        assertTrue(handover.isAllowStartIfComplete());
        assertEquals(3, handover.getStartLimit());
    }

    @Test
    void jobSteps_calcAloneIsOneStepOnEitherStore() {
        RecordingStep calc = new RecordingStep("calcInterestStep");
        for (CalcStore store : CalcStore.values()) {
            var job = (SimpleJob) new InterestJobConfig().interestCalcJob(REPOSITORY,
                    new RecordingStep("loadExtractsStep"), calc, new RecordingStep("exportDatasetsStep"), store,
                    JobSteps.CALC);

            assertEquals(List.of("calcInterestStep"), List.copyOf(job.getStepNames()));
        }
    }

    @Test
    void jobSteps_loadCalcExportInThatOrderOnTheTables() {
        var job = (SimpleJob) new InterestJobConfig().interestCalcJob(REPOSITORY,
                new RecordingStep("loadExtractsStep"), new RecordingStep("calcInterestStep"),
                new RecordingStep("exportDatasetsStep"), CalcStore.JDBC, JobSteps.LOAD_CALC_EXPORT);

        assertEquals(List.of("loadExtractsStep", "calcInterestStep", "exportDatasetsStep"),
                List.copyOf(job.getStepNames()));
        assertFalse(job.getStep("exportDatasetsStep") instanceof RecordingStep, "the export runs behind the handover");
    }

    @Test
    void jobSteps_loadCalcExportIsRefusedOnTheFileAdapters() {
        IllegalStateException refusal = assertThrows(IllegalStateException.class,
                () -> new InterestJobConfig().interestCalcJob(REPOSITORY, new RecordingStep("loadExtractsStep"),
                        new RecordingStep("calcInterestStep"), new RecordingStep("exportDatasetsStep"), CalcStore.FILE,
                        JobSteps.LOAD_CALC_EXPORT));

        assertTrue(refusal.getMessage().contains("interest.calc.store=jdbc"), refusal.getMessage());
    }
}
