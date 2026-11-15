package com.carddemo.interest.recon;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.FieldSpec;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.export.DatasetExporter;
import com.carddemo.interest.jdbc.ReplicaTableWriter;
import com.carddemo.interest.load.ExtractFile;
import com.carddemo.interest.load.ExtractFormat;
import com.carddemo.interest.load.TableFigures;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Builds the reconciliation report of one load and run (strategy-interest.md, RC1-RC6 and TL4;
 * ticket 10). One side of every line is counted here from the unloaded files or from the tables as
 * they are now; the other is the figure the loader or the exporter stored in interest_run.
 *
 * <p>The report reads and writes nothing: all its statements run in one read-only transaction, in
 * which PostgreSQL refuses any INSERT, UPDATE, DELETE or DDL. A figure that cannot be computed - a
 * file that is missing or undecodable, an interest_run column that is still empty, a statement
 * that fails - is NOT AVAILABLE, never EQUAL.
 */
public final class ReconciliationReporter {

    private static final String RUN_TABLE = ExtractFile.SCHEMA + ".interest_run";
    private static final String TRANSACTION_TABLE = ExtractFile.SCHEMA + ".interest_transaction";
    private static final Set<String> AFTER_CALCULATION =
            Set.of(DatasetExporter.STATUS_CALCULATED, DatasetExporter.STATUS_EXPORTED);

    private static final String FROM_FILE = "unloaded file";
    private static final String FROM_TABLE = "table";
    private static final String FROM_RUN = "interest_run";

    private final JdbcTemplate jdbc;
    private final ReplicaTableWriter tables;
    private final TransactionTemplate readOnly;

    public ReconciliationReporter(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.tables = new ReplicaTableWriter(jdbc);
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    /**
     * @param title what the report is of, for its heading (the case name)
     * @param runId the interest_run to report; empty for the latest one
     */
    public ReconReport report(String title, Optional<Long> runId, Path inputDir, ExtractFormat format) {
        return readOnly.execute(status -> {
            // The driver starts a READ ONLY transaction for a read-only connection; this makes it so on any driver.
            jdbc.execute("SET TRANSACTION READ ONLY");
            return build(title, runId, inputDir, format);
        });
    }

    private ReconReport build(String title, Optional<Long> requestedRun, Path inputDir, ExtractFormat format) {
        List<String> notes = new ArrayList<>();
        Optional<Long> runId = requestedRun.or(() -> attempt(() -> jdbc.queryForObject("SELECT max(run_id) FROM " + RUN_TABLE, Long.class)));
        Map<String, Object> run = runId.flatMap(id -> attempt(() -> jdbc.queryForList("SELECT * FROM " + RUN_TABLE + " WHERE run_id = ?", id)))
                .filter(rows -> !rows.isEmpty()).map(rows -> rows.get(0)).orElse(Map.of());
        if (run.isEmpty()) {
            notes.add("interest_run has no row " + runId.map(String::valueOf).orElse("at all") + ": every figure of the run is NOT AVAILABLE.");
        }
        Optional<String> runStatus = Optional.ofNullable((String) run.get("status"));
        boolean afterCalculation = runStatus.filter(AFTER_CALCULATION::contains).isPresent();

        ExtractSide files = new ExtractSide(inputDir, format);
        List<ReconLine> rc1 = new ArrayList<>();
        List<ReconLine> rc2 = new ArrayList<>();
        List<ReconLine> rc3 = new ArrayList<>();
        List<ReconLine> rc4 = new ArrayList<>();
        long g02OfFiles = 0;
        long g03OfFiles = 0;
        boolean allFilesRead = true;
        for (ExtractFile file : ExtractFile.values()) {
            Optional<ExtractSide.Figures> ofFile = files.figures(file);
            Optional<TableFigures> ofTable = attempt(() -> tables.figures(file));
            Optional<List<RecordValues>> tableRows = attempt(() -> tables.rowsInKeyOrder(file));
            String table = file.table();

            rc1.add(ReconLine.ofCounts("RC1", table + ": records and rows", FROM_FILE, ofFile.map(ExtractSide.Figures::records),
                    FROM_TABLE, ofTable.map(TableFigures::rowCount)));

            for (FieldSpec field : sumFields(file)) {
                String column = file.column(field);
                rc2.add(ReconLine.ofAmounts("RC2", "sum of " + field.name(), FROM_FILE, ofFile.map(figures -> figures.sums().get(column)),
                        FROM_RUN + ".loaded_" + column + "_sum", amount(run, "loaded_" + column + "_sum")));
            }

            rc3.add(ReconLine.ofCounts("RC3", table + ": records that differ from their row encoded again"
                    + (afterCalculation && !CalculationWrites.fields(file).isEmpty() ? " (the three amounts the calculation writes left out, INT-R07)" : ""),
                    "allowed", Optional.of(0L), FROM_FILE + " against " + FROM_TABLE,
                    ofFile.flatMap(figures -> tableRows.flatMap(rows -> roundTripDifferences(file, format, figures, rows, afterCalculation)))));

            rc4.add(ReconLine.ofCounts("RC4", table + ": distinct keys and rows", FROM_TABLE + " keys", ofTable.map(TableFigures::keyCount),
                    FROM_TABLE + " rows", ofTable.map(TableFigures::rowCount)));
            rc4.add(ReconLine.ofKeys("RC4", table + ": lowest key and first record", FROM_FILE, ofFile.map(ExtractSide.Figures::firstKey),
                    FROM_TABLE, tableRows.filter(rows -> !rows.isEmpty()).map(rows -> ExtractSide.keyOf(file, rows.get(0)))));
            rc4.add(ReconLine.ofKeys("RC4", table + ": highest key and last record", FROM_FILE, ofFile.map(ExtractSide.Figures::lastKey),
                    FROM_TABLE, tableRows.filter(rows -> !rows.isEmpty()).map(rows -> ExtractSide.keyOf(file, rows.get(rows.size() - 1)))));

            if (ofFile.isPresent()) {
                g02OfFiles += ofFile.get().g02Differences();
                g03OfFiles += ofFile.get().g03Differences();
            } else {
                allFilesRead = false;
            }
        }
        Optional<Long> g02 = allFilesRead ? Optional.of(g02OfFiles) : Optional.empty();
        Optional<Long> g03 = allFilesRead ? Optional.of(g03OfFiles) : Optional.empty();
        rc3.add(ReconLine.ofCounts("RC3", "round-trip differences of gap G02 (F-signed amounts)", FROM_FILE, g02,
                FROM_RUN + ".loaded_g02_difference_count", count(run, "loaded_g02_difference_count")));
        rc3.add(ReconLine.ofCounts("RC3", "round-trip differences of gap G03 (minus zero)", FROM_FILE, g03,
                FROM_RUN + ".loaded_g03_difference_count", count(run, "loaded_g03_difference_count")));
        files.unavailable().values().forEach(reason -> notes.add(reason + ": its figures are NOT AVAILABLE."));

        List<ReconLine> lines = new ArrayList<>();
        lines.addAll(rc1);
        lines.addAll(rc2);
        lines.addAll(rc3);
        lines.addAll(rc4);

        // RC5, after the calculation and the export.
        Optional<Long> transactions = runId.flatMap(id -> attempt(() -> jdbc.queryForObject(
                "SELECT count(*) FROM " + TRANSACTION_TABLE + " WHERE run_id = ?", Long.class, id)));
        Optional<BigDecimal> transactionAmounts = runId.flatMap(id -> attempt(() -> jdbc.queryForObject(
                "SELECT coalesce(sum(tran_amt), 0.00) FROM " + TRANSACTION_TABLE + " WHERE run_id = ?", BigDecimal.class, id)));
        Optional<BigDecimal> balanceNow = attempt(() -> jdbc.queryForObject(
                "SELECT sum(acct_curr_bal) FROM " + ExtractFile.ACCOUNT.qualifiedTable(), BigDecimal.class));
        Optional<BigDecimal> balanceLoaded = amount(run, "loaded_acct_curr_bal_sum");
        // Without a finished calculation the balances have not moved for this run: there is no change to state.
        Optional<BigDecimal> balanceChange = afterCalculation
                ? balanceNow.flatMap(now -> balanceLoaded.map(now::subtract)) : Optional.empty();
        Optional<BigDecimal> notAdded = amount(run, "written_interest_not_added_sum");
        Optional<String> lastAccount = attempt(() -> jdbc.queryForObject(
                "SELECT lpad(max(trancat_acct_id)::text, 11, '0') FROM " + ExtractFile.TRAN_CAT_BALANCE.qualifiedTable(), String.class));

        lines.add(ReconLine.ofCounts("RC5", "rows written to interest_transaction and records of the exported SYSTRAN generation",
                FROM_TABLE, afterCalculation ? transactions : Optional.empty(), FROM_RUN + ".written_systran_count", count(run, "written_systran_count")));
        lines.add(ReconLine.ofCounts("RC5", "rows written to interest_transaction and the count of the calculation step",
                FROM_TABLE, afterCalculation ? transactions : Optional.empty(), FROM_RUN + ".written_transaction_count", count(run, "written_transaction_count")));
        lines.add(ReconLine.ofAmounts("RC5", "sum of TRAN-AMT and the change of the sum of ACCT-CURR-BAL plus the interest of the last account (INT-Q04)",
                FROM_TABLE, afterCalculation ? transactionAmounts : Optional.empty(), "balance change + " + FROM_RUN + ".written_interest_not_added_sum",
                balanceChange.flatMap(change -> notAdded.map(change::add))));

        // RC6 needs ACCTDATA unloaded again on the mainframe after the write-back.
        lines.add(ReconLine.notAvailable(ReconReport.OFF_HOST_CHECK, "ACCTDATA unloaded again after the write-back: count and five sums",
                "mainframe unload", FROM_RUN + ".written_acctdata_count", count(run, "written_acctdata_count").map(String::valueOf)));
        notes.add("RC6 cannot be shown off-host: it needs the mainframe unload of ACCTDATA after the write-back (strategy CO2)."
                + " The target side of it is in interest_run (written_acctdata_count and the five written_acct_*_sum columns).");
        if (!afterCalculation) {
            notes.add("The run is not CALCULATED or EXPORTED: RC5 is NOT AVAILABLE.");
        }

        ReconReport.Rc5Figures rc5 = new ReconReport.Rc5Figures(afterCalculation ? transactions : Optional.empty(),
                afterCalculation ? transactionAmounts : Optional.empty(), balanceChange, notAdded, lastAccount);
        return new ReconReport(title, runId, runStatus, inputDir.toString(), format.name().toLowerCase(java.util.Locale.ROOT),
                lines, rc5, profile(run, format), notes);
    }

    /** RC2: the amounts of the file, then its hash total. */
    private static List<FieldSpec> sumFields(ExtractFile file) {
        List<FieldSpec> fields = new ArrayList<>(file.summedFields());
        file.hashTotalField().ifPresent(fields::add);
        return fields;
    }

    /**
     * RC3: the number of file records that have no table row which, encoded again, is the same
     * record. The file side is the record encoded again too, so G02 and G03 do not count here; they
     * are counted on their own lines.
     */
    private Optional<Long> roundTripDifferences(ExtractFile file, ExtractFormat format, ExtractSide.Figures ofFile,
            List<RecordValues> tableRows, boolean afterCalculation) {
        List<FieldSpec> leftOut = afterCalculation ? CalculationWrites.fields(file) : List.of();
        RecordCodec codec = new RecordCodec(file.layout(), format.codePage());
        Map<String, Integer> ofTable = new HashMap<>();
        try {
            for (int i = 0; i < tableRows.size(); i++) {
                byte[] image = codec.encode(tableRows.get(i), new RecordSource("table " + file.table(), i + 1));
                ofTable.merge(image(image, leftOut), 1, Integer::sum);
            }
        } catch (RuntimeException notEncodable) {
            return Optional.empty();
        }
        long differences = 0;
        for (byte[] record : ofFile.encodedRows()) {
            String image = image(record, leftOut);
            if (ofTable.merge(image, -1, Integer::sum) < 0) {
                differences++;
            }
        }
        // A table row no file record accounts for is a difference as well.
        differences += ofTable.values().stream().filter(left -> left > 0).mapToLong(Integer::longValue).sum();
        return Optional.of(differences);
    }

    private static String image(byte[] record, List<FieldSpec> leftOut) {
        byte[] image = record.clone();
        leftOut.forEach(field -> java.util.Arrays.fill(image, field.offset(), field.end(), (byte) 0));
        return HexFormat.of().formatHex(image);
    }

    /** TL4: what the mapping gaps ask to be counted, on the loaded tables and the loader's round-trip counts. */
    private List<ProfileLine> profile(Map<String, Object> run, ExtractFormat format) {
        String balances = ExtractFile.TRAN_CAT_BALANCE.qualifiedTable();
        String accounts = ExtractFile.ACCOUNT.qualifiedTable();
        return List.of(
                new ProfileLine("accounts with several cards", "G08, INT-H03", query("SELECT count(*) FROM (SELECT xref_acct_id FROM "
                        + ExtractFile.CARD_XREF.qualifiedTable() + " GROUP BY xref_acct_id HAVING count(*) > 1) several")),
                new ProfileLine("accounts with mixed type codes (letters and digits)", "G06", query("SELECT count(*) FROM (SELECT trancat_acct_id FROM "
                        + balances + " GROUP BY trancat_acct_id HAVING bool_or(trancat_type_cd ~ '[0-9]')"
                        + " AND bool_or(trancat_type_cd ~ '[^0-9 ]')) mixed")),
                new ProfileLine("minus zero amounts", "G03", count(run, "loaded_g03_difference_count")),
                new ProfileLine("F-signed amounts", "G02", count(run, "loaded_g02_difference_count")),
                new ProfileLine("fillers that are not blank", "TL4", nonBlankFillers(format.codePage())),
                new ProfileLine("accounts that are not active", "INT-H04", query("SELECT count(*) FROM " + accounts
                        + " WHERE acct_active_status <> 'Y'")),
                new ProfileLine("balances without a rate and without a DEFAULT rate", "TL4, INT-R08", query("SELECT count(*) FROM " + balances + " b"
                        + " JOIN " + accounts + " a ON a.acct_id = b.trancat_acct_id WHERE NOT EXISTS (SELECT 1 FROM "
                        + ExtractFile.DISCLOSURE_GROUP.qualifiedTable() + " d WHERE d.dis_tran_type_cd = b.trancat_type_cd"
                        + " AND d.dis_tran_cat_cd = b.trancat_cd AND d.dis_acct_group_id IN (a.acct_group_id, 'DEFAULT'))")));
    }

    private Optional<Long> nonBlankFillers(CodePage codePage) {
        return attempt(() -> {
            long count = 0;
            for (ExtractFile file : ExtractFile.values()) {
                List<FieldSpec> fillers = file.layout().fields().stream().filter(field -> field.kind() == FieldKind.FILLER).toList();
                for (RecordValues row : tables.rowsInKeyOrder(file)) {
                    for (FieldSpec filler : fillers) {
                        byte[] bytes = row.filler(filler);
                        boolean blank = true;
                        for (byte b : bytes) {
                            blank &= b == codePage.space();
                        }
                        if (!blank) {
                            count++;
                        }
                    }
                }
            }
            return count;
        });
    }

    private Optional<Long> query(String sql) {
        return attempt(() -> jdbc.queryForObject(sql, Long.class));
    }

    private static Optional<Long> count(Map<String, Object> run, String column) {
        return Optional.ofNullable((Number) run.get(column)).map(Number::longValue);
    }

    private static Optional<BigDecimal> amount(Map<String, Object> run, String column) {
        return Optional.ofNullable((BigDecimal) run.get(column));
    }

    /** A figure, or nothing when it cannot be computed. */
    private static <T> Optional<T> attempt(Supplier<T> figure) {
        try {
            return Optional.ofNullable(figure.get());
        } catch (RuntimeException notComputable) {
            return Optional.empty();
        }
    }
}
