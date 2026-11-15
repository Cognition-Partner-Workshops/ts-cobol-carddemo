package com.carddemo.interest.recon;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.carddemo.interest.export.DatasetExporter;
import com.carddemo.interest.load.ExtractFormat;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * The commands of the data-move kit (ticket 10), behind make recon-java and make sidebyside-java.
 * It is a main of its own, not the job: no Spring Batch, no job repository, nothing written to
 * schema batch. Arguments are name=value, as the job's parameters are.
 *
 * <pre>
 * stage    parmDate= inputDir= format= calculatedDir= outputDir=       load, post the calculation, export
 * report   title= inputDir= format= [runId=] [reportFile=]             the reconciliation report (read only)
 * classify legacy= target= ts=format|exact differences= [reportFile=]  the side-by-side report
 *          [legacyName= targetName=]                                   the directories as the report names them
 * </pre>
 *
 * Exit code: 0 when the report shows no difference, 1 when it shows one or a figure is NOT
 * AVAILABLE, 2 when the command could not run.
 */
public final class ReconCommand {

    static final int EXIT_EQUAL = 0;
    static final int EXIT_DIFFERENT = 1;
    static final int EXIT_USAGE = 2;

    /** First line of the differences file when compare.py compared something: what it checked. */
    static final String CHECKED_PREFIX = "#checked\t";

    private ReconCommand() {
    }

    public static void main(String[] args) {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
        int exit;
        try {
            exit = run(args);
        } catch (RuntimeException e) {
            System.err.println("recon: " + e.getMessage());
            exit = EXIT_USAGE;
        }
        System.exit(exit);
    }

    static int run(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("usage: stage|report|classify name=value ...");
        }
        Map<String, String> parameters = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            int equals = args[i].indexOf('=');
            if (equals < 1) {
                throw new IllegalArgumentException("not name=value: " + args[i]);
            }
            parameters.put(args[i].substring(0, equals), args[i].substring(equals + 1));
        }
        return switch (args[0]) {
            case "stage" -> stage(parameters);
            case "report" -> report(parameters);
            case "classify" -> classify(parameters);
            default -> throw new IllegalArgumentException("unknown command " + args[0]);
        };
    }

    private static int stage(Map<String, String> parameters) {
        DriverManagerDataSource dataSource = dataSource();
        long runId = new CalculatedRunStaging(new JdbcTemplate(dataSource), new DataSourceTransactionManager(dataSource),
                Clock.systemDefaultZone()).stage(required(parameters, "parmDate"), Path.of(required(parameters, "inputDir")),
                        ExtractFormat.ofParameter(required(parameters, "format")), Path.of(required(parameters, "calculatedDir")),
                        Path.of(required(parameters, "outputDir")));
        System.out.println("interest_run " + runId + " loaded, calculated (stand-in for ticket 06) and exported");
        return EXIT_EQUAL;
    }

    private static int report(Map<String, String> parameters) {
        DriverManagerDataSource dataSource = dataSource();
        ReconReport report = new ReconciliationReporter(new JdbcTemplate(dataSource), new DataSourceTransactionManager(dataSource))
                .report(required(parameters, "title"), Optional.ofNullable(parameters.get("runId")).map(Long::valueOf),
                        Path.of(required(parameters, "inputDir")), ExtractFormat.ofParameter(required(parameters, "format")));
        publish(report.toMarkdown(), parameters);
        return report.reconciled() ? EXIT_EQUAL : EXIT_DIFFERENT;
    }

    private static int classify(Map<String, String> parameters) {
        Path legacy = Path.of(required(parameters, "legacy"));
        TimestampRule rule = TimestampRule.ofParameter(required(parameters, "ts"));
        List<String> lines = readLines(Path.of(required(parameters, "differences")));
        boolean compared = !lines.isEmpty() && lines.get(0).startsWith(CHECKED_PREFIX);
        String checked = compared ? lines.get(0).substring(CHECKED_PREFIX.length()) : "";
        List<ComparedDifference> differences = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            String[] cells = line.split("\t", -1);
            if (cells.length != 4) {
                throw new IllegalArgumentException("not a difference line of four cells: " + line);
            }
            differences.add(new ComparedDifference(unescape(cells[0]), unescape(cells[1]), unescape(cells[2]), unescape(cells[3])));
        }
        SideBySideReport report = SideBySideReport.of(parameters.getOrDefault("legacyName", legacy.toString()),
                parameters.getOrDefault("targetName", required(parameters, "target")), rule, compared, checked,
                differences, new DifferenceClassifier(rule, accountRecords(legacy)));
        publish(report.toMarkdown(), parameters);
        return ReconStatus.EQUAL.label().equals(report.verdict()) ? EXIT_EQUAL : EXIT_DIFFERENT;
    }

    /** The records of the legacy ACCTDATA image in the recorded format (FI-2); 0 when the file is not there. */
    private static long accountRecords(Path legacy) {
        Path accounts = legacy.resolve(DatasetExporter.DATASETS_DIR).resolve(DatasetExporter.ACCOUNTS_DATASET + ".txt");
        return Files.isRegularFile(accounts) ? readLines(accounts).size() : 0;
    }

    /** compare.py values can hold tabs and line ends; the differences file escapes them with a backslash. */
    static String unescape(String cell) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < cell.length(); i++) {
            char c = cell.charAt(i);
            if (c == '\\' && i + 1 < cell.length()) {
                char next = cell.charAt(++i);
                text.append(next == 'n' ? '\n' : next == 't' ? '\t' : next);
            } else {
                text.append(c);
            }
        }
        return text.toString();
    }

    private static void publish(String markdown, Map<String, String> parameters) {
        System.out.print(markdown);
        String reportFile = parameters.get("reportFile");
        if (reportFile == null) {
            return;
        }
        try {
            Path file = Path.of(reportFile);
            if (file.toAbsolutePath().getParent() != null) {
                Files.createDirectories(file.toAbsolutePath().getParent());
            }
            Files.writeString(file, markdown, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write " + reportFile, e);
        }
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file, e);
        }
    }

    private static String required(Map<String, String> parameters, String name) {
        String value = parameters.get(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("parameter " + name + " is missing");
        }
        return value;
    }

    /** The same database settings as the job (application.yml): INTEREST_DB_URL, INTEREST_DB_USER, INTEREST_DB_PASSWORD. */
    private static DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                environment("INTEREST_DB_URL", "jdbc:postgresql://localhost:5432/carddemo"),
                environment("INTEREST_DB_USER", "carddemo"), environment("INTEREST_DB_PASSWORD", "carddemo"));
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isEmpty() ? fallback : value;
    }
}
