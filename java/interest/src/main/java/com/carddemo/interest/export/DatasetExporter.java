package com.carddemo.interest.export;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.carddemo.interest.codec.CardDemoLayouts;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.jdbc.InterestTransactionReader;
import com.carddemo.interest.jdbc.ReplicaTable;
import com.carddemo.interest.jdbc.ReplicaTableReader;
import com.carddemo.interest.load.ExtractFormat;

/**
 * Writes one calculated run from the tables (INT-T07).
 *
 * <p>The files are written to a staging directory beside the output directory and moved into place
 * in one step, so the output directory is this export or what it was before, never a mix. The run log
 * {@code sysout/INTCALC.STEP15.txt} belongs to the calculation step (INT-T07 acceptance): when that
 * step left it in the output directory, it is carried over unchanged; nothing else of an older
 * directory survives. The rows are read in one read-only REPEATABLE READ transaction, so the files
 * and the RC5 figures describe the same state of the tables.
 */
@Component
public class DatasetExporter {

    public static final String TRANSACTIONS_DATASET = "AWS.M2.CARDDEMO.SYSTRAN.G0001V00";
    public static final String ACCOUNTS_DATASET = "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS";
    public static final String DATASETS_DIR = "datasets";
    public static final String RUN_LOG_FILE = "sysout/INTCALC.STEP15.txt";
    public static final String RETURN_CODES_FILE = "rc.json";
    private static final String STAGING_SUFFIX = ".export-staging";
    /** STEP15 of a run that reached CALCULATED ended with return code 0. */
    private static final int CALCULATED_RETURN_CODE = 0;

    private final InterestTransactionReader transactions;
    private final ReplicaTableReader tables;
    private final ExportRunJournal journal;
    private final TransactionTemplate readOnly;
    private final TransactionTemplate update;
    private final Clock clock;

    public DatasetExporter(InterestTransactionReader transactions, ReplicaTableReader tables, ExportRunJournal journal,
            PlatformTransactionManager transactionManager, Clock clock) {
        this.transactions = transactions;
        this.tables = tables;
        this.journal = journal;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
        this.readOnly.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        this.update = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /** The newest CALCULATED run, for a launch that does not carry a run id. */
    public long newestCalculatedRun() {
        return journal.newestCalculated().orElseThrow(() -> new InterestRunStoppedException(StopReason.WRITE_OUTPUT,
                "export refused: no run with status " + ExportRunJournal.CALCULATED));
    }

    /**
     * Exports the run to the output directory.
     *
     * @throws InterestRunStoppedException when the run is not CALCULATED, when its RC5 figures differ
     *         from what the tables hold, or when the files cannot be written; the output directory is
     *         then what it was before
     */
    public ExportResult export(long runId, Path outputDir, ExtractFormat format) {
        Path staging = outputDir.resolveSibling(outputDir.getFileName() + STAGING_SUFFIX);
        try {
            ExportResult result = readOnly.execute(status -> stage(runId, outputDir, staging, format));
            moveIntoPlace(staging, outputDir);
            boolean marked = update.execute(status -> journal.exported(result, clock.instant()));
            if (!marked) {
                throw refused(runId, "the run changed status while it was exported");
            }
            return result;
        } catch (IOException | UncheckedIOException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot write the export of run " + runId, e);
        } finally {
            deleteTree(staging);
        }
    }

    private ExportResult stage(long runId, Path outputDir, Path staging, ExtractFormat format) {
        ExportRunJournal.Run run = journal.find(runId).orElseThrow(() -> refused(runId, "there is no such run"));
        if (!ExportRunJournal.CALCULATED.equals(run.status())) {
            throw refused(runId, "its status is " + run.status() + ", not " + ExportRunJournal.CALCULATED);
        }
        CodePage codePage = format.codePage();
        List<byte[]> transactionRecords = transactions.records(runId, codePage);
        List<byte[]> accountRecords = tables.records(ReplicaTable.ACCOUNT, codePage);
        ExportResult result = new ExportResult(runId, outputDir, transactionRecords.size(), transactions.amountSum(runId),
                accountRecords.size(), tables.sum(ReplicaTable.ACCOUNT, "ACCT-CURR-BAL"));
        requireRc5(run, result);
        try {
            deleteTree(staging);
            Files.createDirectories(staging.resolve(DATASETS_DIR));
            write(staging, TRANSACTIONS_DATASET, CardDemoLayouts.TRANSACTION, transactionRecords, format);
            write(staging, ACCOUNTS_DATASET, CardDemoLayouts.ACCOUNT, accountRecords, format);
            Files.writeString(staging.resolve(RETURN_CODES_FILE), ExportedReturnCodes.json(CALCULATED_RETURN_CODE),
                    StandardCharsets.ISO_8859_1);
            Path runLog = outputDir.resolve(RUN_LOG_FILE);
            if (Files.isRegularFile(runLog)) {
                Files.createDirectories(staging.resolve(RUN_LOG_FILE).getParent());
                Files.copy(runLog, staging.resolve(RUN_LOG_FILE));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    /** RC5: what is exported is what the calculation recorded; a figure it did not record is not checked. */
    private static void requireRc5(ExportRunJournal.Run run, ExportResult result) {
        if (run.transactionCount() != null && run.transactionCount() != result.transactionCount()) {
            throw refused(run.runId(), "RC5: " + result.transactionCount() + " transactions in the table, "
                    + run.transactionCount() + " recorded by the calculation");
        }
        if (differs(run.tranAmtSum(), result.tranAmtSum())) {
            throw refused(run.runId(), "RC5: the transaction amounts in the table add up to " + result.tranAmtSum()
                    + ", the calculation recorded " + run.tranAmtSum());
        }
        if (differs(run.acctCurrBalSum(), result.acctCurrBalSum())) {
            throw refused(run.runId(), "RC5: the account balances in the table add up to " + result.acctCurrBalSum()
                    + ", the calculation recorded " + run.acctCurrBalSum());
        }
    }

    private static boolean differs(BigDecimal recorded, BigDecimal exported) {
        return recorded != null && recorded.compareTo(exported) != 0;
    }

    private static InterestRunStoppedException refused(long runId, String why) {
        return new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "export of run " + runId + " refused: " + why);
    }

    private static void write(Path staging, String dataset, RecordLayout layout, List<byte[]> records, ExtractFormat format)
            throws IOException {
        Path file = staging.resolve(DATASETS_DIR).resolve(dataset + format.fileSuffix());
        if (format == ExtractFormat.FIXTURE) {
            StringBuilder text = new StringBuilder();
            records.forEach(record -> text.append(RecordImage.toLine(RecordImage.image(record))).append('\n'));
            Files.writeString(file, text, StandardCharsets.ISO_8859_1);
            return;
        }
        byte[] content = new byte[records.size() * layout.length()];
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).length != layout.length()) {
                throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, RecordSource.built(dataset) + ": record " + (i + 1)
                        + " is " + records.get(i).length + " bytes, " + layout.copybook() + " is " + layout.length());
            }
            System.arraycopy(records.get(i), 0, content, i * layout.length(), layout.length());
        }
        Files.write(file, content);
    }

    private static void moveIntoPlace(Path staging, Path outputDir) throws IOException {
        Path old = outputDir.resolveSibling(outputDir.getFileName() + STAGING_SUFFIX + "-old");
        deleteTree(old);
        Files.createDirectories(outputDir.toAbsolutePath().getParent());
        boolean hadOutput = Files.exists(outputDir);
        if (hadOutput) {
            Files.move(outputDir, old, StandardCopyOption.ATOMIC_MOVE);
        }
        try {
            Files.move(staging, outputDir, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            if (hadOutput) {
                Files.move(old, outputDir, StandardCopyOption.ATOMIC_MOVE);
            }
            throw e;
        }
        deleteTree(old);
    }

    private static void deleteTree(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
