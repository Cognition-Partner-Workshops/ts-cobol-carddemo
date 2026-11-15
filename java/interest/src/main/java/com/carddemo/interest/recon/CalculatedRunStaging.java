package com.carddemo.interest.recon;

import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.FieldSpec;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.codec.TransactionRecord;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.export.DatasetExporter;
import com.carddemo.interest.export.ExportFormat;
import com.carddemo.interest.jdbc.FinishedRunReader;
import com.carddemo.interest.load.ExtractFile;
import com.carddemo.interest.load.ExtractFormat;
import com.carddemo.interest.load.ExtractLoader;
import com.carddemo.interest.load.LoadRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Brings one case into the state the reconciliation report is made for - loaded, calculated and
 * exported - for the trial load of the kit (ticket 10). The load is the loader's (ticket 05) and
 * the export is the exporter's (ticket 07). Between the two it is a STAND-IN: the job has no
 * calculation step on the tables yet (ticket 06, calc-step-jdbc), so the result of the calculation
 * is taken from the output directory of a run of interestCalcJob on its file adapters and posted
 * to the tables as that step will post it: the transactions into interest_transaction in write
 * order, the amounts of each account into account, and the step's figures into interest_run.
 * It is not part of the job and goes when ticket 06 is merged.
 *
 * <p>The interest that is not added (written_interest_not_added_sum) is counted here from the
 * tables, not from the balances: the transactions of the account that ends the category-balance
 * file (rule INT-Q04), found through card_xref. RC5 then checks it against the balance change.
 */
public final class CalculatedRunStaging {

    private static final String RUN_TABLE = ExtractFile.SCHEMA + ".interest_run";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ExtractLoader loader;
    private final DatasetExporter exporter;

    public CalculatedRunStaging(JdbcTemplate jdbc, PlatformTransactionManager transactionManager, Clock clock) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.loader = new ExtractLoader(jdbc, transactionManager, clock);
        this.exporter = new DatasetExporter(new FinishedRunReader(jdbc), jdbc);
    }

    /**
     * @param calculatedDir the output directory of the file-adapter run of the same inputs
     * @param outputDir where the export of the tables goes
     * @return the interest_run of the staged run
     */
    public long stage(String parmDate, Path inputDir, ExtractFormat format, Path calculatedDir, Path outputDir) {
        long runId = loader.load(new LoadRequest(parmDate, inputDir, format, outputDir.resolveSibling("load-rejects.txt"))).runId();
        ExportFormat exportFormat = ExportFormat.ofParameter(format.name().toLowerCase(Locale.ROOT));
        CodePage codePage = exportFormat.codePage();
        Path datasets = calculatedDir.resolve(DatasetExporter.DATASETS_DIR);
        List<RecordValues> transactions = read(datasets, DatasetExporter.TRANSACTIONS_DATASET, format, TransactionRecord.LAYOUT, codePage);
        List<RecordValues> accounts = read(datasets, DatasetExporter.ACCOUNTS_DATASET, format, AccountRecord.LAYOUT, codePage);
        transaction.executeWithoutResult(status -> post(runId, transactions, accounts));
        try {
            Path runLog = outputDir.resolve(DatasetExporter.RUN_LOG_FILE);
            Files.createDirectories(runLog.getParent());
            Files.copy(calculatedDir.resolve(DatasetExporter.RUN_LOG_FILE), runLog, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "the run log of the calculation cannot be carried over", e);
        }
        exporter.export(runId, outputDir, exportFormat);
        return runId;
    }

    private void post(long runId, List<RecordValues> transactions, List<RecordValues> accounts) {
        List<FieldSpec> transactionFields = TransactionRecord.LAYOUT.fields();
        String insert = "INSERT INTO " + FinishedRunReader.TRANSACTION_TABLE + " (run_id, seq_no, "
                + transactionFields.stream().map(CalculatedRunStaging::transactionColumn).collect(Collectors.joining(", "))
                + ") VALUES (?, ?" + ", ?".repeat(transactionFields.size()) + ")";
        for (int i = 0; i < transactions.size(); i++) {
            List<Object> arguments = new ArrayList<>(List.of(runId, i + 1));
            for (FieldSpec field : transactionFields) {
                arguments.add(value(transactions.get(i), field));
            }
            jdbc.update(insert, arguments.toArray());
        }
        ExtractFile account = ExtractFile.ACCOUNT;
        List<FieldSpec> written = CalculationWrites.fields(account);
        FieldSpec key = account.keyFields().get(0);
        String update = "UPDATE " + account.qualifiedTable() + " SET "
                + written.stream().map(field -> account.column(field) + " = ?").collect(Collectors.joining(", "))
                + " WHERE " + account.column(key) + " = ?";
        for (RecordValues row : accounts) {
            List<Object> arguments = new ArrayList<>();
            written.forEach(field -> arguments.add(row.signed(field)));
            arguments.add(row.unsigned(key));
            jdbc.update(update, arguments.toArray());
        }
        jdbc.update("UPDATE " + RUN_TABLE + " SET status = ?,"
                + " written_transaction_count = (SELECT count(*) FROM " + FinishedRunReader.TRANSACTION_TABLE + " WHERE run_id = ?),"
                + " written_tran_amt_sum = (SELECT coalesce(sum(tran_amt), 0.00) FROM " + FinishedRunReader.TRANSACTION_TABLE + " WHERE run_id = ?),"
                + " written_interest_not_added_sum = (SELECT coalesce(sum(t.tran_amt), 0.00) FROM " + FinishedRunReader.TRANSACTION_TABLE + " t"
                + " JOIN " + ExtractFile.CARD_XREF.qualifiedTable() + " x ON x.xref_card_num = t.tran_card_num"
                + " WHERE t.run_id = ? AND x.xref_acct_id = (SELECT max(trancat_acct_id) FROM " + ExtractFile.TRAN_CAT_BALANCE.qualifiedTable() + "))"
                + " WHERE run_id = ?", DatasetExporter.STATUS_CALCULATED, runId, runId, runId, runId);
    }

    /** Columns are named as the data map names them: the field in lower case, the FILLER as tran_filler. */
    private static String transactionColumn(FieldSpec field) {
        return field.kind() == FieldKind.FILLER ? "tran_filler" : field.name().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static Object value(RecordValues row, FieldSpec field) {
        return switch (field.kind()) {
            case UNSIGNED_ZONED -> row.unsigned(field);
            case SIGNED_ZONED -> row.signed(field);
            case TEXT -> row.text(field);
            case FILLER -> row.filler(field);
            case NOT_DISPLAY -> throw new IllegalStateException("field " + field.name() + " has no column (E5)");
        };
    }

    private static List<RecordValues> read(Path datasets, String datasetName, ExtractFormat format, RecordLayout layout, CodePage codePage) {
        // The file-adapter run writes the recorded format (FI-2) or, for format=host, the bare dataset name.
        String fileName = datasetName + (format == ExtractFormat.FIXTURE ? ".txt" : "");
        byte[] content;
        try {
            content = Files.readAllBytes(datasets.resolve(fileName));
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.OPEN_INPUT, "cannot read the calculated dataset " + fileName, e);
        }
        List<byte[]> records = format == ExtractFormat.HOST ? RecordImage.fromBlock(content, layout.length())
                : new String(content, codePage.charset()).lines().map(line -> RecordImage.fromLine(line, layout.length(), codePage)).toList();
        RecordCodec codec = new RecordCodec(layout, codePage);
        List<RecordValues> rows = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            rows.add(codec.decode(records.get(i), new RecordSource(fileName, i + 1)));
        }
        return rows;
    }
}
