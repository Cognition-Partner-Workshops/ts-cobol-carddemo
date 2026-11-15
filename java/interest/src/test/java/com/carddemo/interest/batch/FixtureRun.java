package com.carddemo.interest.batch;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.AccountInterest;
import com.carddemo.interest.policy.InterestPolicy;

/**
 * Runs calcInterestStep on an input directory without a job repository: the reader, processor and
 * writer of the step ({@link CalcInterestRun}) on the file adapters, with the PARM and the frozen
 * clock of the recorded runs (2022071800, 2022-07-05 10:00:00.00). The whole input is one chunk.
 */
public final class FixtureRun {

    public static final Clock FROZEN = Clock.fixed(Instant.parse("2022-07-05T10:00:00Z"), ZoneOffset.UTC);

    private FixtureRun() {
    }

    /** Runs the step with the given policy; returns its exit status. The outputs are under outputDir. */
    public static ExitStatus run(Path inputDir, Path outputDir, InterestPolicy policy) {
        StepExecution stepExecution = new StepExecution(InterestJobConfig.CALC_STEP_NAME, new JobExecution(1L,
                new JobParametersBuilder()
                        .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                        .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                        .addString(InterestJobParameters.INPUT_DIR, inputDir.toString())
                        .addString(InterestJobParameters.OUTPUT_DIR, outputDir.toString())
                        .addString(InterestJobParameters.FORMAT, "fixture")
                        .toJobParameters()));
        CalcInterestRun run = new CalcInterestRun(policy, FROZEN);
        run.beforeStep(stepExecution);
        List<AccountInterest> results = new ArrayList<>();
        for (AccountBalances item = run.read(); item != null; item = run.read()) {
            results.add(run.process(item));
        }
        run.write(new Chunk<>(results));
        stepExecution.setStatus(BatchStatus.COMPLETED);
        return run.afterStep(stepExecution);
    }
}
