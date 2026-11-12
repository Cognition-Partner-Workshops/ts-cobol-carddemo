package com.carddemo.modern.trantype;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.postgresql.PGConnection;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

/**
 * One parity run: reset and seed the tables (as {@code record.py reset_database}), run MNTTRDB2,
 * then write rc.json, SYSOUT and the table snapshots (as {@code record.py snapshot_database}).
 */
@Component
public class Mnttrdb2Runner implements ApplicationRunner {

    private final DataSource dataSource;
    private final JobLauncher launcher;
    private final Job mnttrdb2Job;
    private final Sysout sysout;
    private final Estate estate;

    public Mnttrdb2Runner(DataSource dataSource, JobLauncher launcher, Job mnttrdb2Job, Sysout sysout, Estate estate) {
        this.dataSource = dataSource;
        this.launcher = launcher;
        this.mnttrdb2Job = mnttrdb2Job;
        this.sysout = sysout;
        this.estate = estate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("case") || !args.containsOption("out")) {
            throw new IllegalArgumentException("usage: --case=<fixtures/trantype-update/case> --out=<dir>");
        }
        Path caseDir = Path.of(args.getOptionValues("case").get(0)).toAbsolutePath();
        Path out = Path.of(args.getOptionValues("out").get(0));

        resetDatabase(caseDir);
        sysout.clear();
        JobExecution execution = launcher.run(mnttrdb2Job, new JobParametersBuilder()
                .addString("case", caseDir.toString())
                .addLong("run", System.nanoTime())
                .toJobParameters());
        if (execution.getStatus() != BatchStatus.COMPLETED) {
            throw new IllegalStateException("MNTTRDB2 failed: " + execution.getAllFailureExceptions());
        }
        StepExecution step1 = execution.getStepExecutions().iterator().next();
        int rc = step1.getExecutionContext().getInt(CobtupdtProcessor.RETURN_CODE_KEY);

        RecordedRun run = new RecordedRun(out);
        Map<String, Integer> steps = new LinkedHashMap<>();
        steps.put("STEP1", rc);
        run.returnCodes("MNTTRDB2", rc, steps);
        run.sysout("MNTTRDB2", "STEP1", sysout.lines());
        snapshotDatabase(run);
    }

    private void resetDatabase(Path caseDir) throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            c.setAutoCommit(true);
            for (String table : estate.tables().keySet()) {
                s.execute("TRUNCATE TABLE " + table + " CASCADE");
            }
            Path seed = caseDir.resolve("input").resolve(estate.seed());
            if (Files.exists(seed)) {
                ScriptUtils.executeSqlScript(c, new FileSystemResource(seed));
            }
        }
    }

    /** {@code COPY (SELECT * FROM <table> ORDER BY <order>) TO STDOUT WITH CSV HEADER}, as psql prints it. */
    private void snapshotDatabase(RecordedRun run) throws Exception {
        try (Connection c = dataSource.getConnection()) {
            var copy = c.unwrap(PGConnection.class).getCopyAPI();
            for (Map.Entry<String, String> t : estate.tables().entrySet()) {
                ByteArrayOutputStream csv = new ByteArrayOutputStream();
                copy.copyOut("COPY (SELECT * FROM " + t.getKey() + " ORDER BY " + t.getValue() + ") TO STDOUT WITH CSV HEADER", csv);
                run.db(t.getKey(), csv.toByteArray());
            }
        }
    }
}
