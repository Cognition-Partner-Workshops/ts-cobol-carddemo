package com.carddemo.interest.recon;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordLayout.Kind;
import com.carddemo.interest.codec.RecordOrigin;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.load.DecodedExtract;
import com.carddemo.interest.load.ExtractFile;
import com.carddemo.interest.recon.ReconTables.RunRow;
import com.carddemo.interest.recon.ReconTables.TableFigures;
import com.carddemo.interest.recon.ReconTables.WrittenFigures;

/**
 * The reconciliation of one load and one run (strategy-interest.md, RC1 to RC6): every figure is
 * computed on two sides - the unloaded and exported files, and interest_run with the tables - and
 * the two are put next to each other. The kit reports; it decides nothing (REL-04, REL-05 are
 * open) and it changes nothing. A figure that cannot be computed is NOT AVAILABLE, never equal.
 */
public final class Reconciliation {

    /** INT-R07: the three account columns the calculation writes; after it they are not the unloaded values. */
    private static final List<String> POSTED_FIELDS = List.of("ACCT-CURR-BAL", "ACCT-CURR-CYC-CREDIT", "ACCT-CURR-CYC-DEBIT");
    private static final String LOADED = "LOADED";
    private static final String RUN = "interest_run";

    private final String name;
    private final UnloadedFiles files;
    private final ExportedFiles exported;
    private final ReconTables tables;
    private final List<CheckLine> lines = new ArrayList<>();
    private final Map<String, Figure> figures = new LinkedHashMap<>();
    private Optional<RunRow> run = Optional.empty();
    private String runWhyNot = "no row in carddemo.interest_run";

    private Reconciliation(String name, UnloadedFiles files, ExportedFiles exported, ReconTables tables) {
        this.name = name;
        this.files = files;
        this.exported = exported;
        this.tables = tables;
    }

    /** The report of the run with that key, or of the run with the highest key when none is given. */
    public static ReconReport report(String name, UnloadedFiles files, ExportedFiles exported, ReconTables tables,
            OptionalLong runId) {
        Reconciliation reconciliation = new Reconciliation(name, files, exported, tables);
        reconciliation.readRun(runId);
        Map<ExtractFile, Optional<TableFigures>> tableFigures = new LinkedHashMap<>();
        Map<ExtractFile, String> tableWhyNot = new HashMap<>();
        for (ExtractFile file : ExtractFile.values()) {
            try {
                tableFigures.put(file, Optional.of(tables.figures(file, UnloadedFiles.summedColumns(file))));
            } catch (RuntimeException e) {
                tableFigures.put(file, Optional.empty());
                tableWhyNot.put(file, Figure.firstLine(e));
            }
        }
        for (ExtractFile file : ExtractFile.values()) {
            reconciliation.counts(file, tableFigures.get(file), tableWhyNot.get(file));
        }
        for (ExtractFile file : ExtractFile.values()) {
            reconciliation.sums(file, tableFigures.get(file), tableWhyNot.get(file));
        }
        Map<ExtractFile, Figure[]> edgeKeys = new HashMap<>();
        for (ExtractFile file : ExtractFile.values()) {
            edgeKeys.put(file, reconciliation.roundTrip(file));
        }
        for (ExtractFile file : ExtractFile.values()) {
            reconciliation.keys(file, tableFigures.get(file), tableWhyNot.get(file), edgeKeys.get(file));
        }
        reconciliation.afterTheCalculation(tableFigures, tableWhyNot);
        reconciliation.lines.add(new CheckLine("RC6", "ACCTDATA unloaded again after the write-back: count and the five sums of RC2",
                "ACCTDATA unloaded again", Figure.notAvailable("needs a mainframe unload after the write-back"),
                "carddemo.account", Figure.notAvailable("not compared: there is no second unload off-host"),
                Verdict.NOT_AVAILABLE, "cannot be shown off-host; mainframe operations owner"));
        return new ReconReport(name, files.directory().toString(), exported.directory().toString(), reconciliation.run,
                reconciliation.runWhyNot, List.copyOf(reconciliation.lines), reconciliation.figures, files.profile(),
                files.rejects());
    }

    private void readRun(OptionalLong runId) {
        try {
            run = tables.run(runId);
            if (run.isEmpty() && runId.isPresent()) {
                runWhyNot = "no row in carddemo.interest_run with run_id " + runId.getAsLong();
            }
        } catch (RuntimeException e) {
            runWhyNot = Figure.firstLine(e);
        }
    }

    private Figure runFigure(String column) {
        if (run.isEmpty()) {
            return Figure.notAvailable(runWhyNot);
        }
        Object value = run.get().figures().get(column);
        if (value == null) {
            return Figure.notAvailable(RUN + "." + column + " is empty");
        }
        return Figure.of(value instanceof BigDecimal number ? number : new BigDecimal(value.toString()));
    }

    /** True when the run has passed the calculation: the three posted account columns are no longer the unloaded values. */
    private boolean isCalculated() {
        return run.isPresent() && !LOADED.equals(run.get().status());
    }

    private static String tableName(ExtractFile file) {
        return file.table().substring(file.table().indexOf('.') + 1);
    }

    private static Figure tableFigure(Optional<TableFigures> figures, String whyNot, java.util.function.Function<TableFigures, Figure> figure) {
        return figures.map(figure).orElseGet(() -> Figure.notAvailable(whyNot));
    }

    // RC1: row count of each table = record count of its unloaded file
    private void counts(ExtractFile file, Optional<TableFigures> table, String whyNot) {
        Figure rows = tableFigure(table, whyNot, figures -> Figure.of(figures.rows()));
        String column = "loaded_" + tableName(file) + "_count";
        lines.add(CheckLine.compare("RC1", "records and rows of " + tableName(file), file.fileName(), files.count(file),
                file.table(), rows, ""));
        lines.add(CheckLine.compare("RC1", "count recorded at the load of " + tableName(file), RUN + "." + column,
                runFigure(column), file.table(), rows, ""));
    }

    // RC2: sums equal on both sides, and the hash totals of the account ids
    private void sums(ExtractFile file, Optional<TableFigures> table, String whyNot) {
        for (FieldLayout field : file.layout().fields()) {
            String column = file.column(field);
            if (!UnloadedFiles.summedColumns(file).contains(column) || field.kind() == Kind.FILLER) {
                continue;
            }
            String what = (field.kind() == Kind.SIGNED ? "sum of " : "hash total of ") + field.name();
            String recorded = "loaded_" + column + "_sum";
            lines.add(CheckLine.compare("RC2", what + ", recorded at the load", file.fileName(), files.sum(file, column),
                    RUN + "." + recorded, runFigure(recorded), ""));
            if (isCalculated() && POSTED_FIELDS.contains(field.name())) {
                // not a line: the table no longer holds the loaded value; ACCT-CURR-BAL after the calculation is RC5
                continue;
            }
            if (run.isEmpty() && POSTED_FIELDS.contains(field.name())) {
                lines.add(CheckLine.compare("RC2", what + ", in the table now", file.fileName(), files.sum(file, column),
                        file.table(), Figure.notAvailable("the run is not known: the column may hold the value after the calculation"), ""));
                continue;
            }
            Figure now = tableFigure(table, whyNot, figures -> figures.sums().get(column) == null
                    ? Figure.notAvailable("no row in " + file.table()) : Figure.of(figures.sums().get(column)));
            lines.add(CheckLine.compare("RC2", what + ", in the table now", file.fileName(), files.sum(file, column),
                    file.table(), now, ""));
        }
    }

    /**
     * RC3: every row, encoded again, is its source record. The sign bytes of gaps G02 and G03 are
     * counted in the file and are not a failure; any other difference is. Returns the lowest and
     * the highest key of the table for RC4.
     */
    private Figure[] roundTrip(ExtractFile file) {
        String what = "rows of " + tableName(file) + " that encode to their source record";
        Optional<DecodedExtract> extract = files.extract(file);
        RecordCodec codec = files.format().codec(file.layout());
        // without a run it is not known whether the calculation has written the three columns: they are left out as well
        boolean isMasked = file == ExtractFile.ACCOUNTS && (isCalculated() || run.isEmpty());
        Map<List<Object>, byte[]> sourceByKey = new HashMap<>();
        if (extract.isPresent()) {
            int number = 0;
            for (RecordValues values : extract.get().rows()) {
                number++;
                sourceByKey.put(UnloadedFiles.keyOf(file, values),
                        masked(file, codec.encode(values, new RecordOrigin(file.fileName(), number)), isMasked));
            }
        }
        long[] counted = new long[3]; // equal, other differences, rows without a record
        String[] edges = new String[2];
        FieldLayout fillerField = file.layout().fields().stream().filter(field -> field.kind() == Kind.FILLER).findFirst().orElse(null);
        Figure rowsRead = Figure.attempt(() -> {
            long[] rows = new long[1];
            tables.rows(file, row -> {
                rows[0]++;
                if (edges[0] == null) {
                    edges[0] = UnloadedFiles.keyText(row.key());
                }
                edges[1] = UnloadedFiles.keyText(row.key());
                byte[] source = sourceByKey.remove(row.key());
                if (source == null) {
                    counted[2]++;
                    return;
                }
                byte[] encoded;
                try {
                    encoded = codec.encode(row.values(), new RecordOrigin(file.table(), (int) rows[0]));
                } catch (RuntimeException e) {
                    counted[1]++;
                    return;
                }
                if (fillerField != null) {
                    if (row.filler() == null || row.filler().length != fillerField.length()) {
                        counted[1]++;
                        return;
                    }
                    System.arraycopy(row.filler(), 0, encoded, fillerField.offset(), fillerField.length());
                }
                counted[Arrays.equals(source, masked(file, encoded, isMasked)) ? 0 : 1]++;
            });
            return Figure.of(rows[0]);
        });
        Figure lowest = !rowsRead.isAvailable() ? rowsRead : edges[0] == null ? Figure.notAvailable("no row in " + file.table()) : Figure.of(edges[0]);
        Figure highest = !rowsRead.isAvailable() ? rowsRead : edges[1] == null ? Figure.notAvailable("no row in " + file.table()) : Figure.of(edges[1]);
        if (extract.isEmpty()) {
            lines.add(CheckLine.compare("RC3", what, file.fileName(), files.notAvailable(file), file.table(),
                    Figure.notAvailable("no source record to compare with"), ""));
            return new Figure[] {lowest, highest};
        }
        long recordsWithoutRow = sourceByKey.size();
        long other = counted[1] + counted[2] + recordsWithoutRow;
        String note = "in the file: G02 (F-signed) " + extract.get().fSignedAmounts() + ", G03 (minus zero) "
                + extract.get().minusZeroAmounts() + ", counted and not a failure; other differences " + other
                + (other == 0 ? "" : " (rows that differ " + counted[1] + ", rows without a record " + counted[2]
                        + ", records without a row " + recordsWithoutRow + ")")
                + (isMasked ? "; " + String.join(", ", POSTED_FIELDS) + " left out: written by the calculation (INT-R07), see RC5" : "");
        lines.add(CheckLine.compare("RC3", what, file.fileName(), files.count(file), file.table(),
                rowsRead.isAvailable() ? Figure.of(counted[0]) : rowsRead, other == 0, note));
        return new Figure[] {lowest, highest};
    }

    private static byte[] masked(ExtractFile file, byte[] record, boolean isMasked) {
        if (isMasked) {
            for (String name : POSTED_FIELDS) {
                FieldLayout field = file.layout().field(name);
                Arrays.fill(record, field.offset(), field.end(), (byte) 0);
            }
        }
        return record;
    }

    // RC4: distinct keys = rows; lowest and highest key = first and last record of the file
    private void keys(ExtractFile file, Optional<TableFigures> table, String whyNot, Figure[] edgeKeys) {
        String key = String.join("+", file.keyFields());
        lines.add(CheckLine.compare("RC4", "distinct keys (" + key + ") and rows of " + tableName(file), file.table() + ", distinct keys",
                tableFigure(table, whyNot, figures -> Figure.of(figures.distinctKeys())), file.table() + ", rows",
                tableFigure(table, whyNot, figures -> Figure.of(figures.rows())), ""));
        lines.add(CheckLine.compare("RC4", "lowest key of " + tableName(file), file.fileName() + ", first record",
                files.edgeKey(file, false), file.table() + ", lowest key", edgeKeys[0], "table order: collation C (E6)"));
        lines.add(CheckLine.compare("RC4", "highest key of " + tableName(file), file.fileName() + ", last record",
                files.edgeKey(file, true), file.table() + ", highest key", edgeKeys[1], "table order: collation C (E6)"));
    }

    // RC5: transactions written = records of the exported SYSTRAN; sum of TRAN-AMT = balance change + interest of the last account
    private void afterTheCalculation(Map<ExtractFile, Optional<TableFigures>> tableFigures, Map<ExtractFile, String> tableWhyNot) {
        Optional<WrittenFigures> written = Optional.empty();
        String writtenWhyNot = runWhyNot;
        if (run.isPresent()) {
            try {
                written = Optional.of(tables.written(run.get().runId()));
            } catch (RuntimeException e) {
                writtenWhyNot = Figure.firstLine(e);
            }
        }
        String whyNot = writtenWhyNot;
        Figure transactions = written.map(figures -> Figure.of(figures.transactions())).orElseGet(() -> Figure.notAvailable(whyNot));
        Figure amountSum = written.map(figures -> Figure.of(figures.amountSum())).orElseGet(() -> Figure.notAvailable(whyNot));
        Figure balanceAfter = tableFigure(tableFigures.get(ExtractFile.ACCOUNTS), tableWhyNot.get(ExtractFile.ACCOUNTS),
                figures -> figures.sums().get("acct_curr_bal") == null ? Figure.notAvailable("no row in carddemo.account")
                        : Figure.of(figures.sums().get("acct_curr_bal")));
        Figure balanceBefore = runFigure("loaded_acct_curr_bal_sum");
        Figure balanceChange = isCalculated() ? balanceAfter.minus(balanceBefore)
                : Figure.notAvailable(run.isPresent() ? "the run is " + run.get().status() + ": not calculated" : runWhyNot);
        String[] lastAccount = {"no balance record: no last account"};
        Figure lastInterest = !isCalculated() ? balanceChange : Figure.attempt(() -> {
            OptionalLong account = tables.lastAccount();
            if (account.isEmpty()) {
                return Figure.of(new BigDecimal("0.00"));
            }
            lastAccount[0] = "last account " + String.format("%011d", account.getAsLong());
            return Figure.of(tables.interestOfAccount(run.get().runId(), account.getAsLong()));
        });
        String transactionTable = "carddemo.interest_transaction";
        lines.add(CheckLine.compare("RC5", "transactions written", transactionTable + ", rows of the run", transactions,
                "exported " + ExportedFiles.TRANSACTIONS + ", records", exported.transactionCount(), ""));
        lines.add(CheckLine.compare("RC5", "transactions written, recorded by the run", RUN + ".written_transaction_count",
                runFigure("written_transaction_count"), transactionTable + ", rows of the run", transactions, ""));
        lines.add(CheckLine.compare("RC5", "sum of TRAN-AMT", transactionTable, amountSum,
                "exported " + ExportedFiles.TRANSACTIONS, exported.transactionAmountSum(), ""));
        lines.add(CheckLine.compare("RC5", "sum of TRAN-AMT, recorded by the run", RUN + ".written_tran_amt_sum",
                runFigure("written_tran_amt_sum"), transactionTable, amountSum, ""));
        lines.add(CheckLine.compare("RC5", "sum of ACCT-CURR-BAL after the calculation", "carddemo.account", balanceAfter,
                "exported " + ExportedFiles.ACCOUNTS, exported.accountBalanceSum(), ""));
        lines.add(CheckLine.compare("RC5", "sum of ACCT-CURR-BAL after the calculation, recorded by the run",
                RUN + ".written_acct_curr_bal_sum", runFigure("written_acct_curr_bal_sum"), "carddemo.account", balanceAfter, ""));
        CheckLine identity = CheckLine.compare("RC5", "sum of TRAN-AMT = balance change + interest of the last account, not added (INT-Q04)",
                transactionTable + ", sum of TRAN-AMT", amountSum, "balance change + interest of the last account",
                balanceChange.plus(lastInterest),
                "balance change " + balanceChange.text() + " (" + balanceBefore.text() + " before, " + balanceAfter.text()
                        + " after); interest of the last account, not added: " + lastInterest.text() + " (" + lastAccount[0] + ")");
        lines.add(identity);

        figures.put("Accounts", rowsOf(tableFigures, tableWhyNot, ExtractFile.ACCOUNTS));
        figures.put("Cards", rowsOf(tableFigures, tableWhyNot, ExtractFile.CARD_XREFS));
        figures.put("Balances", rowsOf(tableFigures, tableWhyNot, ExtractFile.CATEGORY_BALANCES));
        figures.put("Rates", rowsOf(tableFigures, tableWhyNot, ExtractFile.DISCLOSURE_GROUPS));
        figures.put("Sum ACCT-CURR-BAL before", balanceBefore);
        figures.put("Sum TRAN-CAT-BAL", sumOf(tableFigures, tableWhyNot, ExtractFile.CATEGORY_BALANCES, "tran_cat_bal"));
        figures.put("Sum DIS-INT-RATE", sumOf(tableFigures, tableWhyNot, ExtractFile.DISCLOSURE_GROUPS, "dis_int_rate"));
        figures.put("Transactions written", transactions);
        figures.put("Sum TRAN-AMT", amountSum);
        figures.put("Sum ACCT-CURR-BAL after", balanceAfter);
        figures.put("Balance change", balanceChange);
        figures.put("Interest of last account (not added)", lastInterest);
        figures.put("RC5", switch (identity.verdict()) {
            case EQUAL -> Figure.of("holds");
            case DIFFERENT -> Figure.of("does not hold");
            case NOT_AVAILABLE -> Figure.notAvailable("a figure of the identity is missing");
        });
    }

    private static Figure rowsOf(Map<ExtractFile, Optional<TableFigures>> tableFigures, Map<ExtractFile, String> whyNot, ExtractFile file) {
        return tableFigure(tableFigures.get(file), whyNot.get(file), figures -> Figure.of(figures.rows()));
    }

    private static Figure sumOf(Map<ExtractFile, Optional<TableFigures>> tableFigures, Map<ExtractFile, String> whyNot,
            ExtractFile file, String column) {
        return tableFigure(tableFigures.get(file), whyNot.get(file), figures -> figures.sums().get(column) == null
                ? Figure.notAvailable("no row in " + file.table()) : Figure.of(figures.sums().get(column)));
    }
}
