package com.carddemo.interest.recon;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.carddemo.interest.codec.CardDemoLayouts;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.DecodedRecord;
import com.carddemo.interest.codec.LayoutField;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.jdbc.ReplicaTable;
import com.carddemo.interest.load.ExtractFile;
import com.carddemo.interest.load.LoadFigures;
import com.carddemo.interest.recon.TrialLoadProfile.RoundTripCount;

/**
 * The reconciliation report of one load and one run: RC1-RC6 of strategy-interest.md, each line
 * EQUAL, DIFFERENT or NOT AVAILABLE with both figures, and the profile of TL4. Built from three
 * sides that were read beforehand - the unloaded files, the database, the exported files - so
 * building it touches nothing.
 */
public record ReconReport(String title, List<String> header, List<ReconLine> lines, TrialLoadProfile profile, String whyNoProfile) {

    private static final String FILES = "unloaded file";
    private static final String RUN = "interest_run";
    private static final String TABLE = "table";
    private static final String EXPORT = "exported file";
    /** The columns the calculation updates (architecture, carddemo.account); RC3 leaves them out once the run is calculated. */
    private static final List<String> CALCULATED_ACCOUNT_FIELDS = List.of("ACCT-CURR-BAL", "ACCT-CURR-CYC-CREDIT", "ACCT-CURR-CYC-DEBIT");

    /** The unloaded files of a case, by extract. */
    public record Unloaded(Map<ExtractFile, RecordFiles> files) {
        RecordFiles of(ExtractFile file) {
            return files.get(file);
        }

        boolean allAvailable() {
            return files.values().stream().allMatch(RecordFiles::available);
        }
    }

    /** The exported files of a run: the SYSTRAN generation and the ACCTDATA image. */
    public record Exported(RecordFiles transactions, RecordFiles accounts) {
    }

    public static ReconReport of(String title, Unloaded unloaded, DatabaseSide database, Exported exported, CodePage codePage) {
        List<ReconLine> lines = new ArrayList<>();
        rc1(lines, unloaded, database);
        rc2(lines, unloaded, database);
        rc3(lines, unloaded, database, codePage);
        rc4(lines, unloaded, database);
        List<String> header = new ArrayList<>();
        header.add(database.failure() != null ? "Database: " + database.failure()
                : database.run() == null ? "Run: no row in interest_run"
                : "Run: interest_run " + database.run().runId() + ", status " + database.run().status()
                        + ", PARM date " + database.run().parmDate().trim());
        rc5(lines, header, unloaded, database, exported);
        lines.add(ReconLine.notAvailable("RC6", "ACCTDATA unloaded again after the write-back equals the account table (count and the five sums of RC2)",
                "needs a mainframe unload after the write-back; cannot be shown off-host"));
        boolean profiled = unloaded.allAvailable();
        return new ReconReport(title, List.copyOf(header), List.copyOf(lines),
                profiled ? TrialLoadProfile.of(unloaded.of(ExtractFile.ACCOUNTS), unloaded.of(ExtractFile.CARDS),
                        unloaded.of(ExtractFile.BALANCES), unloaded.of(ExtractFile.RATES), codePage) : null,
                profiled ? null : "an unloaded file cannot be read");
    }

    public long count(Verdict verdict) {
        return lines.stream().filter(line -> line.verdict() == verdict).count();
    }

    // ---------------------------------------------------------------- RC1, RC2: files against what the load recorded

    private static Figure loaded(DatabaseSide database, java.util.function.Function<LoadFigures, Object> figure) {
        if (database.failure() != null) {
            return Figure.missing(database.failure());
        }
        if (database.run() == null) {
            return Figure.missing("no row in interest_run");
        }
        if (database.run().loaded() == null) {
            return Figure.missing("interest_run " + database.run().runId() + " (" + database.run().status() + ") holds no load figures");
        }
        Object value = figure.apply(database.run().loaded());
        return value == null ? Figure.missing("not recorded in interest_run") : new Figure(value, null);
    }

    private static void rc1(List<ReconLine> lines, Unloaded unloaded, DatabaseSide database) {
        count(lines, "account", unloaded.of(ExtractFile.ACCOUNTS), database, ReplicaTable.ACCOUNT, LoadFigures::accountCount);
        count(lines, "card_xref", unloaded.of(ExtractFile.CARDS), database, ReplicaTable.CARD_XREF, LoadFigures::cardXrefCount);
        count(lines, "tran_cat_balance", unloaded.of(ExtractFile.BALANCES), database, ReplicaTable.TRAN_CAT_BALANCE, LoadFigures::tranCatBalanceCount);
        count(lines, "disclosure_group", unloaded.of(ExtractFile.RATES), database, ReplicaTable.DISCLOSURE_GROUP, LoadFigures::disclosureGroupCount);
    }

    private static void count(List<ReconLine> lines, String name, RecordFiles file, DatabaseSide database, ReplicaTable table,
            java.util.function.Function<LoadFigures, Object> recorded) {
        lines.add(ReconLine.compare("RC1", "records of " + file.fileName() + " = rows loaded into " + name,
                FILES, file.count(), RUN, loaded(database, recorded)));
        String noLoad = database.whyNoLoad();
        lines.add(ReconLine.compare("RC1", "records of " + file.fileName() + " = rows in " + name,
                FILES, file.count(), TABLE,
                noLoad != null ? Figure.missing(noLoad) : Figure.of((long) database.tableRecords().get(table).size())));
    }

    private static void rc2(List<ReconLine> lines, Unloaded unloaded, DatabaseSide database) {
        RecordFiles accounts = unloaded.of(ExtractFile.ACCOUNTS);
        sum(lines, accounts, "ACCT-CURR-BAL", database, LoadFigures::acctCurrBalSum);
        sum(lines, accounts, "ACCT-CREDIT-LIMIT", database, LoadFigures::acctCreditLimitSum);
        sum(lines, accounts, "ACCT-CASH-CREDIT-LIMIT", database, LoadFigures::acctCashCreditLimitSum);
        sum(lines, accounts, "ACCT-CURR-CYC-CREDIT", database, LoadFigures::acctCurrCycCreditSum);
        sum(lines, accounts, "ACCT-CURR-CYC-DEBIT", database, LoadFigures::acctCurrCycDebitSum);
        sum(lines, unloaded.of(ExtractFile.BALANCES), "TRAN-CAT-BAL", database, LoadFigures::tranCatBalSum);
        sum(lines, unloaded.of(ExtractFile.RATES), "DIS-INT-RATE", database, LoadFigures::disIntRateSum);
        sum(lines, accounts, "ACCT-ID", database, LoadFigures::acctIdSum);
        sum(lines, unloaded.of(ExtractFile.CARDS), "XREF-ACCT-ID", database, LoadFigures::xrefAcctIdSum);
        sum(lines, unloaded.of(ExtractFile.BALANCES), "TRANCAT-ACCT-ID", database, LoadFigures::trancatAcctIdSum);
    }

    private static void sum(List<ReconLine> lines, RecordFiles file, String field, DatabaseSide database,
            java.util.function.Function<LoadFigures, Object> recorded) {
        String what = field.endsWith("ACCT-ID") ? "hash total of " + field : "sum of " + field;
        lines.add(ReconLine.compare("RC2", what, FILES, file.sum(field), RUN, loaded(database, recorded)));
    }

    // ---------------------------------------------------------------- RC3, RC4: files against the rows of the tables

    private static String key(ReplicaTable table, byte[] record) {
        StringBuilder key = new StringBuilder();
        for (String name : table.keyFields()) {
            LayoutField field = table.layout().field(name);
            key.append(RecordImage.image(Arrays.copyOfRange(record, field.offset(), field.end())));
        }
        return key.toString();
    }

    /** A key is shown by the last four characters of each of its fields: enough to tell two keys apart, not a card number. */
    private static String shown(ReplicaTable table, byte[] record) {
        List<String> parts = new ArrayList<>();
        for (String name : table.keyFields()) {
            LayoutField field = table.layout().field(name);
            String value = RecordImage.image(Arrays.copyOfRange(record, field.offset(), field.end())).trim();
            parts.add(value.length() > 4 ? "..." + value.substring(value.length() - 4) : value);
        }
        return String.join("/", parts);
    }

    private static void rc3(List<ReconLine> lines, Unloaded unloaded, DatabaseSide database, CodePage codePage) {
        for (ExtractFile extract : ExtractFile.values()) {
            RecordFiles file = unloaded.of(extract);
            ReplicaTable table = extract.table();
            String what = "every row of " + tableName(table) + ", encoded again, equals its record of " + file.fileName();
            String noLoad = database.whyNoLoad();
            if (!file.available() || noLoad != null) {
                lines.add(ReconLine.notAvailable("RC3", what, file.available() ? noLoad : file.failure()));
                continue;
            }
            boolean maskCalculated = table == ReplicaTable.ACCOUNT && database.run().calculated();
            Map<String, byte[]> rows = new HashMap<>();
            for (byte[] row : database.tableRecords().get(table)) {
                rows.put(key(table, row), row);
            }
            long equalOrExplained = 0;
            long fSigned = 0;
            long minusZero = 0;
            long unexplained = 0;
            for (byte[] source : file.sources()) {
                byte[] row = rows.get(key(table, source));
                if (row != null && maskCalculated) {
                    row = row.clone();
                    for (String name : CALCULATED_ACCOUNT_FIELDS) {
                        LayoutField field = table.layout().field(name);
                        System.arraycopy(source, field.offset(), row, field.offset(), field.length());
                    }
                }
                RoundTripCount count = RoundTripCount.of(table.layout(), source, row, codePage);
                fSigned += count.fSigned();
                minusZero += count.minusZero();
                // A record that differs only by G02/G03 is explained; any other difference is not.
                if (count.other() == 0) {
                    equalOrExplained++;
                } else {
                    unexplained++;
                }
            }
            String note = (fSigned + minusZero > 0 ? "explained by a gap: " + fSigned + " field(s) G02 (F sign), "
                    + minusZero + " field(s) G03 (minus zero). " : "")
                    + (unexplained > 0 ? unexplained + " record(s) with a difference no gap explains, or without a row. " : "")
                    + (maskCalculated ? "The run is calculated: " + String.join(", ", CALCULATED_ACCOUNT_FIELDS)
                            + " are updated by the calculation and left out." : "");
            lines.add(ReconLine.compare("RC3", what, FILES, Figure.of((long) file.sources().size()),
                    "rows equal or explained by G02/G03", Figure.of(equalOrExplained), note.trim()));
        }
    }

    private static void rc4(List<ReconLine> lines, Unloaded unloaded, DatabaseSide database) {
        for (ExtractFile extract : ExtractFile.values()) {
            RecordFiles file = unloaded.of(extract);
            ReplicaTable table = extract.table();
            String name = tableName(table);
            String noLoad = database.whyNoLoad();
            if (noLoad != null) {
                lines.add(ReconLine.notAvailable("RC4", "distinct keys of " + name + " = its row count", noLoad));
                lines.add(ReconLine.notAvailable("RC4", "lowest and highest key of " + name + " = first and last record of " + file.fileName(), noLoad));
                continue;
            }
            List<byte[]> rows = database.tableRecords().get(table);
            Set<String> keys = new HashSet<>();
            rows.forEach(row -> keys.add(key(table, row)));
            lines.add(ReconLine.compare("RC4", "distinct keys of " + name + " = its row count",
                    "distinct keys", Figure.of((long) keys.size()), "rows", Figure.of((long) rows.size())));
            Figure fileEnds = !file.available() ? Figure.missing(file.failure())
                    : file.sources().isEmpty() ? Figure.missing("the file has no record")
                    : Figure.of(shown(table, file.sources().get(0)) + " to " + shown(table, file.sources().get(file.sources().size() - 1)));
            Figure tableEnds = rows.isEmpty() ? Figure.missing("the table has no row")
                    : Figure.of(shown(table, rows.get(0)) + " to " + shown(table, rows.get(rows.size() - 1)));
            boolean sameEnds = file.available() && !file.sources().isEmpty() && !rows.isEmpty()
                    && key(table, file.sources().get(0)).equals(key(table, rows.get(0)))
                    && key(table, file.sources().get(file.sources().size() - 1)).equals(key(table, rows.get(rows.size() - 1)));
            ReconLine shownLine = ReconLine.compare("RC4", "lowest and highest key of " + name + " = first and last record of " + file.fileName(),
                    "first to last record (key ends)", fileEnds, "lowest to highest key (key ends)", tableEnds);
            // The verdict is taken on the whole keys; the report shows the last four characters of each key field only.
            lines.add(shownLine.verdict() == Verdict.NOT_AVAILABLE ? shownLine
                    : new ReconLine(shownLine.check(), shownLine.what(), shownLine.leftSource(), shownLine.left(), shownLine.rightSource(),
                            shownLine.right(), sameEnds ? Verdict.EQUAL : Verdict.DIFFERENT, ""));
        }
    }

    private static String tableName(ReplicaTable table) {
        return table.tableName().substring(table.tableName().indexOf('.') + 1);
    }

    // ---------------------------------------------------------------- RC5: after the calculation

    private static void rc5(List<ReconLine> lines, List<String> header, Unloaded unloaded, DatabaseSide database, Exported exported) {
        RecordFiles systran = exported.transactions();
        RecordFiles after = exported.accounts();
        RecordFiles before = unloaded.of(ExtractFile.ACCOUNTS);
        String noCalculation = database.whyNoCalculation();

        lines.add(ReconLine.compare("RC5", "rows written to interest_transaction = records of the exported SYSTRAN generation",
                TABLE, noCalculation != null ? Figure.missing(noCalculation) : Figure.of(database.transactionRows()),
                EXPORT, systran.count()));
        lines.add(ReconLine.compare("RC5", "sum of tran_amt in interest_transaction = sum of TRAN-AMT of the exported SYSTRAN generation",
                TABLE, noCalculation != null ? Figure.missing(noCalculation) : Figure.of(database.transactionAmountSum()),
                EXPORT, systran.sum("TRAN-AMT")));

        Figure transactionSum = systran.sum("TRAN-AMT");
        Figure change = !before.available() ? Figure.missing(before.failure())
                : !after.available() ? Figure.missing(after.failure())
                : Figure.of(after.total("ACCT-CURR-BAL").subtract(before.total("ACCT-CURR-BAL")));
        Figure lastAccountInterest = lastAccountInterest(unloaded, systran);
        Figure changePlusLast = !change.available() ? change : !lastAccountInterest.available() ? lastAccountInterest
                : Figure.of(((BigDecimal) change.value()).add((BigDecimal) lastAccountInterest.value()));
        ReconLine equation = ReconLine.compare("RC5",
                "sum of TRAN-AMT = change of the sum of ACCT-CURR-BAL + interest of the last account (not added, INT-Q04)",
                EXPORT, transactionSum, "balance change + interest of the last account", changePlusLast,
                change.available() && lastAccountInterest.available() ? change.text() + " + " + lastAccountInterest.text() : "");
        lines.add(equation);

        RunRow run = database.run();
        String noRun = database.failure() != null ? database.failure() : run == null ? "no row in interest_run" : null;
        lines.add(ReconLine.compare("RC5", "transactions recorded by the calculation = records of the exported SYSTRAN generation",
                RUN, noRun != null ? Figure.missing(noRun) : Figure.of(run.writtenTransactionCount()), EXPORT, systran.count()));
        lines.add(ReconLine.compare("RC5", "sum of TRAN-AMT recorded by the calculation = sum of TRAN-AMT of the exported SYSTRAN generation",
                RUN, noRun != null ? Figure.missing(noRun) : Figure.of(run.writtenTranAmtSum()), EXPORT, transactionSum));
        lines.add(ReconLine.compare("RC5", "sum of ACCT-CURR-BAL recorded after the calculation = sum of ACCT-CURR-BAL of the exported ACCTDATA",
                RUN, noRun != null ? Figure.missing(noRun) : Figure.of(run.writtenAcctCurrBalSum()), EXPORT, after.sum("ACCT-CURR-BAL")));

        header.add("RC5 in words: " + systran.count().text() + " transaction(s) summing " + transactionSum.text()
                + "; sum of ACCT-CURR-BAL " + before.sum("ACCT-CURR-BAL").text() + " before and " + after.sum("ACCT-CURR-BAL").text()
                + " after, a balance change of " + change.text() + "; interest of the last account " + lastAccountInterest.text()
                + (equation.verdict() == Verdict.EQUAL ? ", not added to the last account (INT-Q04)."
                        : equation.verdict() == Verdict.DIFFERENT ? "; the equation of RC5 does not hold." : "."));
    }

    /**
     * The interest of the account that ends the category-balance file: the sum of TRAN-AMT of the
     * exported transactions whose card belongs to that account (CVACT03Y).
     */
    private static Figure lastAccountInterest(Unloaded unloaded, RecordFiles systran) {
        RecordFiles balances = unloaded.of(ExtractFile.BALANCES);
        RecordFiles cards = unloaded.of(ExtractFile.CARDS);
        for (RecordFiles needed : List.of(balances, cards, systran)) {
            if (!needed.available()) {
                return Figure.missing(needed.failure());
            }
        }
        if (balances.records().isEmpty()) {
            return Figure.missing("the category-balance file has no record");
        }
        BigDecimal lastAccount = balances.records().get(balances.records().size() - 1).number("TRANCAT-ACCT-ID");
        Set<String> cardsOfLastAccount = new HashSet<>();
        for (DecodedRecord card : cards.records()) {
            if (card.number("XREF-ACCT-ID").compareTo(lastAccount) == 0) {
                cardsOfLastAccount.add(card.text("XREF-CARD-NUM"));
            }
        }
        BigDecimal interest = BigDecimal.ZERO.setScale(CardDemoLayouts.TRANSACTION.field("TRAN-AMT").scale());
        for (DecodedRecord transaction : systran.records()) {
            if (cardsOfLastAccount.contains(transaction.text("TRAN-CARD-NUM"))) {
                interest = interest.add(transaction.number("TRAN-AMT"));
            }
        }
        return Figure.of(interest);
    }

    // ---------------------------------------------------------------- the report as text

    public String markdown() {
        StringBuilder text = new StringBuilder("# Reconciliation report - " + title + "\n\n");
        header.forEach(line -> text.append(line).append("\n\n"));
        text.append("Result: ").append(count(Verdict.EQUAL)).append(" EQUAL, ").append(count(Verdict.DIFFERENT)).append(" DIFFERENT, ")
                .append(count(Verdict.NOT_AVAILABLE)).append(" NOT AVAILABLE. A figure that cannot be computed is NOT AVAILABLE, never EQUAL.\n\n");
        text.append("| Check | What | First figure | Second figure | Result | Note |\n|---|---|---|---|---|---|\n");
        for (ReconLine line : lines) {
            text.append("| ").append(line.check()).append(" | ").append(line.what()).append(" | ")
                    .append(side(line.leftSource(), line.left())).append(" | ").append(side(line.rightSource(), line.right())).append(" | **")
                    .append(line.verdict().text()).append("** | ").append(line.note()).append(" |\n");
        }
        text.append("\n## Profile of the unloaded files (TL4)\n\n");
        if (profile == null) {
            text.append(Verdict.NOT_AVAILABLE.text()).append(" (").append(whyNoProfile).append(")\n");
        } else {
            text.append(profile.nothingFound() ? "Nothing found.\n\n" : "Findings: see the counts that are not 0.\n\n");
            text.append("| Counted | Gap or rule | Count |\n|---|---|---|\n");
            for (String[] row : profile.rows()) {
                text.append("| ").append(row[0]).append(" | ").append(row[1]).append(" | ").append(row[2]).append(" |\n");
            }
        }
        return text.toString();
    }

    private static String side(String source, Figure figure) {
        return figure.available() ? source + ": " + figure.text() : figure.text();
    }
}
