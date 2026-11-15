package com.carddemo.interest.recon;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.carddemo.interest.InterestApplication;
import com.carddemo.interest.load.ExtractFormat;
import com.carddemo.interest.load.ExtractLoader;
import com.carddemo.interest.load.LoadResult;

/**
 * Command line of the data move kit, behind {@code make recon-java} and {@code make sidebyside-java}:
 *
 * <pre>
 * recon               inputDir= outputDir= report= [format=fixture|host] [run=&lt;interest_run.run_id&gt;] [title=]
 * trial-load          inputDir= rejects= [format=] [parmDate=]      (TL3: loads the four tables, as loadExtractsStep does)
 * sidebyside-prepare  legacy= target= work= name= [timestamps=format|exact]
 * sidebyside-report   compare= report= name= legacy= target= prepared= [timestamps=format|exact]
 * </pre>
 *
 * Exit code 0 when no figure is DIFFERENT and no difference is a defect, 1 when one is, 2 on a usage error.
 * {@code recon} and {@code trial-load} start the application context without the job; only {@code trial-load} writes to the database.
 */
public final class DataMoveKit {

    private static final int EXIT_OK = 0;
    private static final int EXIT_DIFFERENT = 1;
    private static final int EXIT_USAGE = 2;

    private DataMoveKit() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: recon | trial-load | sidebyside-prepare | sidebyside-report, then name=value arguments");
            return EXIT_USAGE;
        }
        Map<String, String> options = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            int equals = args[i].indexOf('=');
            if (equals > 0 && equals < args[i].length() - 1) {
                options.put(args[i].substring(0, equals), args[i].substring(equals + 1));
            }
        }
        try {
            return switch (args[0]) {
                case "recon" -> recon(options);
                case "trial-load" -> trialLoad(options);
                case "sidebyside-prepare" -> prepare(options);
                case "sidebyside-report" -> sideBySideReport(options);
                default -> {
                    System.err.println("unknown command " + args[0]);
                    yield EXIT_USAGE;
                }
            };
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return EXIT_USAGE;
        } catch (IOException e) {
            System.err.println(args[0] + ": " + e.getMessage());
            return EXIT_DIFFERENT;
        }
    }

    private static String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null) {
            throw new IllegalArgumentException("missing argument " + name + "=");
        }
        return value;
    }

    /** The application context without the job and without creating the job repository's tables. */
    private static ConfigurableApplicationContext context() {
        return new SpringApplicationBuilder(InterestApplication.class)
                .web(WebApplicationType.NONE)
                .properties("spring.batch.job.enabled=false", "spring.batch.jdbc.initialize-schema=never", "logging.level.root=WARN")
                .run();
    }

    private static int recon(Map<String, String> options) throws IOException {
        Path inputDir = Path.of(required(options, "inputDir"));
        Path outputDir = Path.of(required(options, "outputDir"));
        Path reportFile = Path.of(required(options, "report"));
        ExtractFormat format = ExtractFormat.of(options.getOrDefault("format", "fixture"));
        OptionalLong runId = options.containsKey("run") ? OptionalLong.of(Long.parseLong(options.get("run"))) : OptionalLong.empty();
        ReconReport report;
        try (ConfigurableApplicationContext context = context()) {
            report = context.getBean(ReconReporter.class).report(options.getOrDefault("title", inputDir.toString()), inputDir, outputDir, format, runId);
        }
        write(reportFile, report.markdown());
        System.out.println("recon " + report.title() + ": " + report.count(Verdict.EQUAL) + " EQUAL, " + report.count(Verdict.DIFFERENT)
                + " DIFFERENT, " + report.count(Verdict.NOT_AVAILABLE) + " NOT AVAILABLE -> " + reportFile);
        return report.count(Verdict.DIFFERENT) == 0 ? EXIT_OK : EXIT_DIFFERENT;
    }

    private static int trialLoad(Map<String, String> options) {
        Path inputDir = Path.of(required(options, "inputDir"));
        Path rejects = Path.of(required(options, "rejects"));
        ExtractFormat format = ExtractFormat.of(options.getOrDefault("format", "fixture"));
        try (ConfigurableApplicationContext context = context()) {
            LoadResult result = context.getBean(ExtractLoader.class).load(inputDir, format, options.getOrDefault("parmDate", ""), rejects);
            System.out.println("trial load: interest_run " + result.runId() + " LOADED, " + result.roundTrip().records() + " records");
            return EXIT_OK;
        } catch (RuntimeException e) {
            System.err.println("trial load failed: " + e.getMessage() + (Files.exists(rejects) ? " - reject list: " + rejects : ""));
            return EXIT_DIFFERENT;
        }
    }

    private static int prepare(Map<String, String> options) throws IOException {
        Path work = Path.of(required(options, "work"));
        SideBySide.Prepared prepared = SideBySide.prepare(Path.of(required(options, "legacy")), Path.of(required(options, "target")),
                work, required(options, "name"), TimestampRule.of(options.get("timestamps")));
        write(work.resolve("prepared.txt"), prepared.timestampsWellFormed() + " " + prepared.timestampsMalformed() + "\n");
        return EXIT_OK;
    }

    private static int sideBySideReport(Map<String, String> options) throws IOException {
        TimestampRule rule = TimestampRule.of(options.get("timestamps"));
        Path compare = Path.of(required(options, "compare"));
        Path reportFile = Path.of(required(options, "report"));
        if (!Files.isRegularFile(compare)) {
            throw new IOException("the comparator wrote no report at " + compare);
        }
        String[] prepared = Files.readString(Path.of(required(options, "prepared")), StandardCharsets.ISO_8859_1).trim().split(" ");
        List<SideBySide.Classified> differences = SideBySide.classify(Files.readString(compare, StandardCharsets.UTF_8), rule);
        write(reportFile, SideBySide.markdown(required(options, "name"), required(options, "legacy"), required(options, "target"), rule,
                Long.parseLong(prepared[0]), Long.parseLong(prepared[1]), differences));
        long defects = differences.stream().filter(difference -> difference.classId().equals(SideBySide.DEFECT)).count();
        System.out.println("sidebyside " + options.get("name") + ": " + differences.size() + " difference(s), " + defects + " defect(s) -> " + reportFile);
        return defects == 0 ? EXIT_OK : EXIT_DIFFERENT;
    }

    private static void write(Path file, String text) throws IOException {
        if (file.toAbsolutePath().getParent() != null) {
            Files.createDirectories(file.toAbsolutePath().getParent());
        }
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }
}
