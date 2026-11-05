package com.carddemo.batch.interest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * INTCALC job driver: loads the PS inputs the way the interest chain's setup jobs do, runs
 * {@link Cbact04c} and writes the outputs in the recorder format used by tools/parity
 * ({@code datasets/<DSN>.txt}, {@code sysout/INTCALC.STEP15.txt}, {@code rc.json},
 * {@code manifest.json}).
 *
 * <pre>
 * java -jar carddemo-interest.jar --input DIR --out DIR [--parm 2022071800] [--clock "2022-07-05 10:00:00.00"]
 * </pre>
 */
public final class InterestJob {
    static final String ACCTDATA_PS = "AWS.M2.CARDDEMO.ACCTDATA.PS";
    static final String CARDXREF_PS = "AWS.M2.CARDDEMO.CARDXREF.PS";
    static final String TCATBALF_PS = "AWS.M2.CARDDEMO.TCATBALF.PS";
    static final String DISCGRP_PS = "AWS.M2.CARDDEMO.DISCGRP.PS";
    static final String ACCTDATA_KSDS = "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS";
    static final String SYSTRAN_GDG = "AWS.M2.CARDDEMO.SYSTRAN.G0001V00";
    /** PARM on INTCALC STEP15 in app/jcl/INTCALC.jcl. */
    static final String DEFAULT_PARM = "2022071800";

    private InterestJob() {
    }

    /** Outputs of one INTCALC run, already in their recorded byte form. */
    public record Result(int returnCode, List<String> accounts, List<String> transactions,
                         String sysout, String rcJson, String manifestJson) {
    }

    public static void main(String[] args) throws IOException {
        Path input = null;
        Path out = null;
        String parm = DEFAULT_PARM;
        String clock = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--input" -> input = Path.of(args[++i]);
                case "--out" -> out = Path.of(args[++i]);
                case "--parm" -> parm = args[++i];
                case "--clock" -> clock = args[++i];
                default -> usage("unknown argument " + args[i]);
            }
        }
        if (input == null || out == null) {
            usage("--input and --out are required");
        }
        Result result = run(readInputs(input), parm, clock);
        write(result, out);
        System.exit(result.returnCode());
    }

    private static void usage(String problem) {
        System.err.println(problem);
        System.err.println("usage: InterestJob --input DIR --out DIR [--parm YYYYMMDDnn] [--clock 'YYYY-MM-DD HH:MM:SS.hh']");
        System.exit(2);
    }

    /** Reads {@code <DSN>.txt} for each PS input (one record per line; a missing file is empty). */
    public static Map<String, List<String>> readInputs(Path dir) throws IOException {
        Map<String, List<String>> inputs = new TreeMap<>();
        for (String dsn : List.of(ACCTDATA_PS, CARDXREF_PS, TCATBALF_PS, DISCGRP_PS)) {
            Path file = dir.resolve(dsn + ".txt");
            inputs.put(dsn, Files.exists(file)
                ? Text.lines(Files.readString(file, StandardCharsets.ISO_8859_1)) : List.of());
        }
        return inputs;
    }

    /**
     * Runs the job on PS input lines keyed by DSN.
     *
     * @param clock frozen clock {@code YYYY-MM-DD HH:MM:SS.hh} (like COB_CURRENT_DATE); null for now
     */
    public static Result run(Map<String, List<String>> psInputs, String parm, String clock) {
        IndexedFile accounts = IndexedFile.load(records(psInputs, ACCTDATA_PS, Cbact04c.ACCT_LRECL), 0, 11);
        IndexedFile xref = IndexedFile.load(records(psInputs, CARDXREF_PS, Cbact04c.XREF_LRECL), 0, 16)
            .withAlternateIndex(25, 11);
        IndexedFile tcatbal = IndexedFile.load(records(psInputs, TCATBALF_PS, Cbact04c.TCATBAL_LRECL), 0, 17);
        IndexedFile discgrp = IndexedFile.load(records(psInputs, DISCGRP_PS, Cbact04c.DISCGRP_LRECL), 0, 16);

        Cbact04c program = new Cbact04c(tcatbal, xref, discgrp, accounts, parm, db2Timestamp(clock));
        int rc = program.run();

        StringBuilder sysout = new StringBuilder();
        program.sysout().forEach(line -> sysout.append(line).append('\n'));
        if (!program.stderr().isEmpty()) {
            sysout.append('\n');
            program.stderr().forEach(line -> sysout.append(line).append('\n'));
        }
        List<String> accountOut = unload(accounts.inKeyOrder());
        List<String> tranOut = unload(program.transactions());
        return new Result(rc, accountOut, tranOut, sysout.toString(), rcJson(rc),
            manifestJson(accountOut.size(), tranOut.size()));
    }

    public static void write(Result result, Path out) throws IOException {
        Files.createDirectories(out.resolve("datasets"));
        Files.createDirectories(out.resolve("sysout"));
        writeLines(out.resolve("datasets").resolve(ACCTDATA_KSDS + ".txt"), result.accounts());
        writeLines(out.resolve("datasets").resolve(SYSTRAN_GDG + ".txt"), result.transactions());
        Files.writeString(out.resolve("sysout").resolve("INTCALC.STEP15.txt"), result.sysout(), StandardCharsets.ISO_8859_1);
        Files.writeString(out.resolve("rc.json"), result.rcJson(), StandardCharsets.ISO_8859_1);
        Files.writeString(out.resolve("manifest.json"), result.manifestJson(), StandardCharsets.ISO_8859_1);
    }

    private static void writeLines(Path file, List<String> lines) throws IOException {
        StringBuilder text = new StringBuilder();
        lines.forEach(line -> text.append(line).append('\n'));
        Files.writeString(file, text.toString(), StandardCharsets.ISO_8859_1);
    }

    /** Fixed-length records as the PS load sees them: each line space-filled or cut to LRECL. */
    private static List<String> records(Map<String, List<String>> inputs, String dsn, int lrecl) {
        List<String> records = new ArrayList<>();
        for (String line : inputs.getOrDefault(dsn, List.of())) {
            records.add(Text.pad(line, lrecl));
        }
        return records;
    }

    /** Recorded form of a dataset: one record per line, trailing spaces removed. */
    private static List<String> unload(Collection<String> records) {
        List<String> lines = new ArrayList<>(records.size());
        records.forEach(record -> lines.add(Text.rstrip(record)));
        return lines;
    }

    /** Z-GET-DB2-FORMAT-TIMESTAMP: YYYY-MM-DD-HH.MM.SS.hh0000 from FUNCTION CURRENT-DATE. */
    static String db2Timestamp(String clock) {
        String c = clock != null ? clock
            : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"));
        if (!c.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{2}")) {
            throw new IllegalArgumentException("clock must be YYYY-MM-DD HH:MM:SS.hh, got " + c);
        }
        return c.substring(0, 10) + "-" + c.substring(11, 13) + "." + c.substring(14, 16) + "."
            + c.substring(17, 19) + "." + c.substring(20, 22) + "0000";
    }

    /**
     * rc.json as tools/parity/record.py writes it. The recorder merges step return codes by
     * step name across the setup jobs and INTCALC, so the surviving entries are DISCGRP STEP05
     * (DELETE) and STEP10 (DEFINE CLUSTER), XREFFILE STEP20-30 (AIX, PATH, BLDINDEX) and INTCALC
     * STEP15 (this program). The load's own STEP15 (REPRO, RC 4 on skipped keys) is always
     * overwritten by INTCALC STEP15.
     */
    static String rcJson(int rc) {
        Map<String, Object> steps = new TreeMap<>(Map.of(
            "STEP05", 0, "STEP10", 0, "STEP15", rc, "STEP20", 0, "STEP25", 0, "STEP30", 0));
        Map<String, Object> job = new TreeMap<>(Map.of("maxrc", rc, "steps", steps));
        return Json.write(Map.of("INTCALC", job));
    }

    static String manifestJson(int accounts, int transactions) {
        return Json.write(Map.of(
            ACCTDATA_KSDS, Map.of("layout", "CVACT01Y", "lrecl", Cbact04c.ACCT_LRECL, "records", accounts),
            SYSTRAN_GDG, Map.of("layout", "CVTRA05Y", "lrecl", Cbact04c.TRAN_LRECL, "records", transactions)));
    }
}
