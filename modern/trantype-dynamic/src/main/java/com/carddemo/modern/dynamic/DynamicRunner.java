package com.carddemo.modern.dynamic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.postgresql.PGConnection;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Runs DSNRUN, then records SYSOUT, return codes and PostgreSQL COPY snapshots. */
@Component
public class DynamicRunner implements ApplicationRunner {
    private final RunUnit unit;
    private final JobLauncher launcher;
    private final Job maintenance;

    DynamicRunner(RunUnit unit, JobLauncher launcher, Job maintenance) {
        this.unit = unit;
        this.launcher = launcher;
        this.maintenance = maintenance;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path fixture = Path.of(option(args, "case"));
        Path out = Path.of(option(args, "out"));
        Path estate = Path.of(option(args, "estate.root"));
        RecordedRun recorded = new RecordedRun(out);
        unit.open(fixture, estate);
        try (var connection = unit.connection) {
            var execution = launcher.run(maintenance, new JobParametersBuilder()
                    .addString("case", fixture.toString()).toJobParameters());
            if (execution.getStatus() != BatchStatus.COMPLETED) {
                throw new IllegalStateException("MNTTRDYN failed", execution.getAllFailureExceptions().getFirst());
            }
            unit.finish();
            snapshotDatabase(out);
        }
        recorded.sysout("MNTTRDYN", "STEP1", unit.sysout);
        recorded.returnCodes("MNTTRDYN", unit.rc, Map.of("STEP1", unit.rc));
    }

    private void snapshotDatabase(Path out) throws Exception {
        Files.createDirectories(out.resolve("db"));
        var copy = unit.connection.unwrap(PGConnection.class).getCopyAPI();
        for (var table : Map.of("carddemo.transaction_type", "tr_type",
                "carddemo.transaction_type_categor", "trc_type_code, trc_type_category").entrySet()) {
            try (var csv = Files.newOutputStream(out.resolve("db/" + table.getKey() + ".csv"))) {
                copy.copyOut("COPY (SELECT * FROM " + table.getKey() + " ORDER BY " + table.getValue()
                        + ") TO STDOUT WITH CSV HEADER", csv);
            }
        }
    }

    private static String option(ApplicationArguments args, String name) {
        if (!args.containsOption(name)) throw new IllegalArgumentException("Missing --" + name);
        return args.getOptionValues(name).getFirst();
    }
}
