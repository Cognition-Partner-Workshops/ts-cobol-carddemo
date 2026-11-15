package com.carddemo.interest.recon;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

import com.carddemo.interest.codec.RecordFormat;

/**
 * Command line of the reconciliation report ({@code make recon-java}):
 * {@code name=<label> inputDir=<unloaded files> outputDir=<exported files> report=<file> [runId=<interest_run.run_id>]}.
 * The database is the one of the job (INTEREST_DB_URL, INTEREST_DB_USER, INTEREST_DB_PASSWORD).
 * Exit code 0 when RC1 to RC5 are EQUAL, 1 when a line is DIFFERENT or NOT AVAILABLE, 2 on a usage error.
 */
public final class ReconCommand {

    static final int EXIT_EQUAL = 0;
    static final int EXIT_NOT_EQUAL = 1;
    static final int EXIT_USAGE = 2;

    private ReconCommand() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.getenv()));
    }

    static int run(String[] args, Map<String, String> environment) {
        Map<String, String> arguments = arguments(args);
        String name = arguments.get("name");
        String inputDir = arguments.get("inputDir");
        String outputDir = arguments.get("outputDir");
        String reportFile = arguments.get("report");
        if (name == null || inputDir == null || outputDir == null || reportFile == null) {
            System.err.println("usage: name=<label> inputDir=<dir> outputDir=<dir> report=<file> [runId=<interest_run.run_id>]");
            return EXIT_USAGE;
        }
        OptionalLong runId;
        try {
            runId = arguments.containsKey("runId") ? OptionalLong.of(Long.parseLong(arguments.get("runId"))) : OptionalLong.empty();
        } catch (NumberFormatException e) {
            System.err.println("runId must be a run_id of carddemo.interest_run");
            return EXIT_USAGE;
        }
        // the job parameters accept the fixture format only (REL-08 is open); the report reads what the job read
        RecordFormat format = RecordFormat.fixture();
        UnloadedFiles files = UnloadedFiles.read(Path.of(inputDir), format);
        ExportedFiles exported = ExportedFiles.read(Path.of(outputDir), format);
        ReconReport report;
        try (JdbcReconTables tables = JdbcReconTables.open(
                environment.getOrDefault("INTEREST_DB_URL", "jdbc:postgresql://localhost:5432/carddemo"),
                environment.getOrDefault("INTEREST_DB_USER", "carddemo"),
                environment.getOrDefault("INTEREST_DB_PASSWORD", "carddemo"))) {
            report = Reconciliation.report(name, files, exported, tables, runId);
        } catch (SQLException e) {
            report = Reconciliation.report(name, files, exported,
                    ReconTables.unavailable("the database cannot be reached: " + Figure.firstLine(e)), runId);
        }
        try {
            Path target = Path.of(reportFile);
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.writeString(target, report.markdown(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("cannot write the report " + reportFile);
            return EXIT_USAGE;
        }
        System.out.println(report.summaryLine());
        return report.verdict() == Verdict.EQUAL ? EXIT_EQUAL : EXIT_NOT_EQUAL;
    }

    static Map<String, String> arguments(String[] args) {
        Map<String, String> arguments = new HashMap<>();
        for (String argument : args) {
            int at = argument.indexOf('=');
            if (at > 0) {
                arguments.put(argument.substring(0, at), argument.substring(at + 1));
            }
        }
        return arguments;
    }
}
