package com.carddemo.modern.interest;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.carddemo.modern.codec.Layouts;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * One parity run: load the case's inputs (setup jobs), run INTCALC, unload the outputs in the
 * layout {@code tools/parity/record.py} writes, so {@code compare.py} can check them.
 */
@Component
public class IntcalcRunner implements ApplicationRunner {

    private final InputLoader loader;
    private final JobLauncher launcher;
    private final Job intcalcJob;
    private final InterestRepository repository;
    private final Sysout sysout;
    private final Estate estate;
    private final Step15Abend abend;

    public IntcalcRunner(InputLoader loader, JobLauncher launcher, Job intcalcJob, InterestRepository repository,
                         Sysout sysout, Estate estate, Step15Abend abend) {
        this.loader = loader;
        this.launcher = launcher;
        this.intcalcJob = intcalcJob;
        this.repository = repository;
        this.sysout = sysout;
        this.estate = estate;
        this.abend = abend;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("case") || !args.containsOption("out")) {
            throw new IllegalArgumentException("usage: --case=<fixtures/interest/case> --out=<dir>");
        }
        Path caseDir = Path.of(args.getOptionValues("case").get(0));
        Path out = Path.of(args.getOptionValues("out").get(0));

        sysout.clear();
        abend.clear();
        Map<String, Integer> steps = new LinkedHashMap<>(loader.load(caseDir));
        JobExecution execution = launcher.run(intcalcJob, new JobParametersBuilder()
                .addString("case", caseDir.toString())
                .addLong("run", System.nanoTime())
                .toJobParameters());
        int rc = returnCode(execution);
        steps.put("STEP15", rc);

        RecordedRun run = new RecordedRun(out);
        run.returnCodes("INTCALC", rc, steps);
        run.sysout("INTCALC", "STEP15", sysout.lines());
        run.dataset("AWS.M2.CARDDEMO.SYSTRAN.G0001V00", Layouts.TRAN,
                repository.transactionsInWriteOrder().stream().map(Records.Transaction::fields).toList());
        run.dataset("AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS", Layouts.ACCOUNT,
                repository.accountsInKeyOrder().stream().map(Records.Account::fields).toList());
    }

    private int returnCode(JobExecution execution) {
        if (abend.abended()) {
            return CobolAbend.RETURN_CODE;
        }
        if (execution.getStatus() == BatchStatus.COMPLETED) {
            return 0;
        }
        List<Throwable> failures = execution.getAllFailureExceptions();
        if (failures.stream().anyMatch(IntcalcRunner::isAbend)) {
            return CobolAbend.RETURN_CODE;
        }
        throw new IllegalStateException("INTCALC failed: " + failures);
    }

    private static boolean isAbend(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof CobolAbend) {
                return true;
            }
        }
        return false;
    }
}
