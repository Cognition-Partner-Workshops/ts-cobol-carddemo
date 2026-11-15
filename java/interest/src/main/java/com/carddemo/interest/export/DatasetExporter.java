package com.carddemo.interest.export;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.FieldSpec;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.codec.TransactionRecord;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.jdbc.FinishedRunReader;

/**
 * Writes the results of one calculated run from the tables as the datasets the mainframe takes
 * back (architecture.md section 2, interest-export): the run's interest transactions as the new
 * SYSTRAN generation in write order, the account table as the ACCTDATA image in key order, and the
 * return codes (INT-R06, INT-T03; standards S6.4). The run log of the calculation step is carried
 * into the new directory unchanged.
 *
 * <p>Unit of work (architecture.md section 5): the files are written to a staging directory and
 * moved into place, then interest_run is set to EXPORTED with what was written (strategy items
 * RC5 and RC6). No other row is changed.
 */
@Component
public class DatasetExporter {

    public static final String TRANSACTIONS_DATASET = "AWS.M2.CARDDEMO.SYSTRAN.G0001V00";
    public static final String ACCOUNTS_DATASET = "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS";
    public static final String DATASETS_DIR = "datasets";
    public static final String RUN_LOG_FILE = "sysout/INTCALC.STEP15.txt";
    public static final String STATUS_CALCULATED = "CALCULATED";
    public static final String STATUS_EXPORTED = "EXPORTED";

    private static final Logger log = LoggerFactory.getLogger(DatasetExporter.class);
    private static final String STEP = "exportDatasetsStep";

    /** Only a run that is still CALCULATED becomes EXPORTED; the figures are RC5 and RC6. */
    private static final String MARK_EXPORTED = """
            UPDATE carddemo.interest_run
               SET status = ?,
                   written_systran_count = ?, written_systran_tran_amt_sum = ?,
                   written_acctdata_count = ?, written_acct_curr_bal_sum = ?, written_acct_credit_limit_sum = ?,
                   written_acct_cash_credit_limit_sum = ?, written_acct_curr_cyc_credit_sum = ?,
                   written_acct_curr_cyc_debit_sum = ?
             WHERE run_id = ? AND status = ?""";

    private final FinishedRunReader finishedRun;
    private final JdbcTemplate jdbc;

    public DatasetExporter(FinishedRunReader finishedRun, JdbcTemplate jdbc) {
        this.finishedRun = finishedRun;
        this.jdbc = jdbc;
    }

    public ExportTotals export(long runId, Path outputDir, ExportFormat format) {
        refuseUnlessCalculated(runId);
        OutputDirectory directory = new OutputDirectory(outputDir, runId);
        ExportTotals totals;
        try {
            Path staging = directory.openStaging();
            Files.createDirectories(staging.resolve(DATASETS_DIR));
            SumOfSignedFields transactionAmounts = new SumOfSignedFields(TransactionRecord.LAYOUT.fields());
            long transactions = writeDataset(staging, format, TRANSACTIONS_DATASET,
                    new TransactionRecord(format.codePage()).codec(), FinishedRunReader.TRANSACTION_TABLE,
                    transactionAmounts, each -> finishedRun.transactionsInWriteOrder(runId, each));
            SumOfSignedFields accountAmounts = new SumOfSignedFields(AccountRecord.LAYOUT.fields());
            long accounts = writeDataset(staging, format, ACCOUNTS_DATASET,
                    new AccountRecord(format.codePage()).codec(), FinishedRunReader.ACCOUNT_TABLE,
                    accountAmounts, finishedRun::accountsInKeyOrder);
            Files.writeString(staging.resolve(RecordedReturnCodes.FILE),
                    RecordedReturnCodes.json(RecordedReturnCodes.NORMAL_END), StandardCharsets.ISO_8859_1);
            carryRunLog(directory, staging, runId);
            directory.replaceWithStaging();
            totals = new ExportTotals(transactions, transactionAmounts.sums().get(0), accounts, accountAmounts.sums());
        } catch (IOException | UncheckedIOException | DataAccessException e) {
            directory.discardStaging();
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT,
                    "run " + runId + " cannot be exported: " + e.getClass().getSimpleName(), e);
        } catch (RuntimeException e) {
            directory.discardStaging();
            throw e;
        }
        markExported(runId, totals);
        log.info("run {} step {} format={} transactions={} accounts={} status={}",
                runId, STEP, format.parameterValue(), totals.transactionCount(), totals.accountCount(), STATUS_EXPORTED);
        return totals;
    }

    private void refuseUnlessCalculated(long runId) {
        String status = finishedRun.statusOf(runId).orElseThrow(() -> new InterestRunStoppedException(
                StopReason.WRITE_OUTPUT, "run " + runId + " is not in interest_run; nothing is exported"));
        if (!STATUS_CALCULATED.equals(status)) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT,
                    "run " + runId + " has status " + status + ", not " + STATUS_CALCULATED + "; nothing is exported");
        }
    }

    private static long writeDataset(Path staging, ExportFormat format, String datasetName, RecordCodec codec,
            String table, SumOfSignedFields sums, Consumer<Consumer<RecordValues>> rows) throws IOException {
        Path file = staging.resolve(DATASETS_DIR).resolve(format.datasetFile(datasetName));
        long[] count = {0};
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file))) {
            rows.accept(values -> {
                count[0]++;
                try {
                    format.write(out, codec.encode(values, new RecordSource(table, Math.toIntExact(count[0]))));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                sums.add(values);
            });
        }
        return count[0];
    }

    /** The run log is the calculation step's (INT-T02); the export keeps it, byte for byte. */
    private static void carryRunLog(OutputDirectory directory, Path staging, long runId) throws IOException {
        Path runLog = directory.existing(RUN_LOG_FILE);
        if (runLog == null) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT,
                    "run " + runId + ": the run log " + RUN_LOG_FILE + " of the calculation step is not in the output directory");
        }
        Path target = staging.resolve(RUN_LOG_FILE);
        Files.createDirectories(target.getParent());
        Files.copy(runLog, target);
    }

    private void markExported(long runId, ExportTotals totals) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(STATUS_EXPORTED);
        arguments.add(totals.transactionCount());
        arguments.add(totals.transactionAmountSum());
        arguments.add(totals.accountCount());
        arguments.addAll(totals.accountAmountSums());
        arguments.add(runId);
        arguments.add(STATUS_CALCULATED);
        int updated = jdbc.update(MARK_EXPORTED, arguments.toArray());
        if (updated != 1) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT,
                    "run " + runId + " was no longer " + STATUS_CALCULATED + " when the export ended");
        }
    }

    /** Sums of the signed (money) fields of a layout, in layout order; sums of scale-2 values (standards S3.3). */
    private static final class SumOfSignedFields {
        private final List<FieldSpec> fields;
        private final List<BigDecimal> sums = new ArrayList<>();

        SumOfSignedFields(List<FieldSpec> layoutFields) {
            this.fields = layoutFields.stream().filter(field -> field.kind() == FieldKind.SIGNED_ZONED).toList();
            fields.forEach(field -> sums.add(ExportTotals.zero()));
        }

        void add(RecordValues values) {
            for (int i = 0; i < fields.size(); i++) {
                sums.set(i, sums.get(i).add(values.signed(fields.get(i)))
                        .setScale(ExportTotals.MONEY_SCALE, RoundingMode.UNNECESSARY));
            }
        }

        List<BigDecimal> sums() {
            return List.copyOf(sums);
        }
    }
}
