package com.carddemo.interest.export;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carddemo.interest.codec.FixtureCodePage;
import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.codec.RecordOrigin;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.export.ExportSource.AccountRow;
import com.carddemo.interest.export.ExportSource.RunToExport;
import com.carddemo.interest.export.ExportSource.TransactionRow;

/**
 * The export of one finished run (INT-T07, component interest-export): the run's interest
 * transactions as the SYSTRAN generation, the account table as the ACCTDATA image, and the return
 * codes. Only a CALCULATED run is exported. Everything is written to a staging directory next to
 * the output directory and moved into place as a whole, so the output never mixes two exports and
 * a refused or failed export leaves no SYSTRAN generation (INTCALC.jcl:37 DISP=(NEW,CATLG,DELETE),
 * standards S6.4); then the run becomes EXPORTED (architecture.md section 5).
 */
public final class DatasetExport {

    static final String DATASETS_DIR = "datasets";
    static final String TRANSACTIONS_DATASET = "AWS.M2.CARDDEMO.SYSTRAN.G0001V00";
    static final String ACCOUNTS_DATASET = "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS";
    /** The run log of the calculation step: not written here, kept when the directory is replaced. */
    static final String RUN_LOG_FILE = "sysout/INTCALC.STEP15.txt";
    /** INT-T03: a run that reached CALCULATED ended normally. */
    private static final int NORMAL_END = 0;

    private static final Logger log = LoggerFactory.getLogger(DatasetExport.class);

    /** What was exported: the figures of RC5 as they left the tables. */
    public record Exported(long runId, int transactionCount, BigDecimal amountSum, int accountCount) {
    }

    private final ExportSource source;

    public DatasetExport(ExportSource source) {
        this.source = source;
    }

    public Exported export(long runId, Path outputDir, ExportFormat format) {
        RunToExport run = source.run(runId)
                .orElseThrow(() -> new ExportRefusedException("there is no run " + runId));
        if (!ExportSource.CALCULATED.equals(run.status())) {
            throw new ExportRefusedException("run " + runId + " has status " + run.status()
                    + "; only a run with status " + ExportSource.CALCULATED + " is exported");
        }
        List<TransactionRow> transactions = source.transactions(runId);
        List<AccountRow> accounts = source.accounts();
        Exported exported = new Exported(runId, transactions.size(), amountSum(transactions), accounts.size());
        checkAgainstCalculation(run, exported);

        ExportRecords records = new ExportRecords(format.recordFormat());
        String transactionsFile = format.fileOf(TRANSACTIONS_DATASET);
        String accountsFile = format.fileOf(ACCOUNTS_DATASET);
        List<byte[]> transactionRecords = new ArrayList<>();
        for (TransactionRow row : transactions) {
            transactionRecords.add(records.transaction(row,
                    new RecordOrigin(transactionsFile, transactionRecords.size() + 1)));
        }
        List<byte[]> accountRecords = new ArrayList<>();
        for (AccountRow row : accounts) {
            accountRecords.add(records.account(row, new RecordOrigin(accountsFile, accountRecords.size() + 1)));
        }

        Path stagingDir = outputDir.resolveSibling(outputDir.getFileName() + ".export-staging-" + runId);
        try {
            deleteTree(stagingDir);
            Path datasets = Files.createDirectories(stagingDir.resolve(DATASETS_DIR));
            RecordFormat recordFormat = format.recordFormat();
            Files.write(datasets.resolve(transactionsFile), recordFormat.content(transactionRecords));
            Files.write(datasets.resolve(accountsFile), recordFormat.content(accountRecords));
            Files.writeString(stagingDir.resolve(RecordedReturnCodes.FILE), RecordedReturnCodes.json(NORMAL_END),
                    FixtureCodePage.CHARSET);
            Path runLog = outputDir.resolve(RUN_LOG_FILE);
            if (Files.isRegularFile(runLog)) {
                Path kept = stagingDir.resolve(RUN_LOG_FILE);
                Files.createDirectories(kept.getParent());
                Files.copy(runLog, kept);
            }
            deleteTree(outputDir);
            Files.createDirectories(outputDir.toAbsolutePath().getParent());
            Files.move(stagingDir, outputDir, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            discard(stagingDir);
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot write the exported datasets", e);
        }
        if (!source.markExported(runId)) {
            throw new ExportRefusedException("run " + runId + " was no longer " + ExportSource.CALCULATED
                    + " when the export ended");
        }
        log.info("run {} step exportDatasetsStep exported: format={} transactions={} amountSum={} accounts={}",
                runId, format, exported.transactionCount(), exported.amountSum(), exported.accountCount());
        return exported;
    }

    /** Sum of scale-2 amounts: no rounding can take place (standards S3.3). */
    private static BigDecimal amountSum(List<TransactionRow> transactions) {
        BigDecimal sum = BigDecimal.ZERO;
        for (TransactionRow row : transactions) {
            sum = sum.add(row.amount());
        }
        return sum.setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * RC5: the records of the exported SYSTRAN generation and the sum of their TRAN-AMT equal the
     * count and sum recorded in interest_run after the calculation. Figures that were never
     * recorded cannot be checked, and the export is refused.
     */
    private static void checkAgainstCalculation(RunToExport run, Exported exported) {
        if (run.writtenTransactionCount() == null || run.writtenAmountSum() == null) {
            throw new ExportRefusedException("run " + run.runId()
                    + " has no written transaction count and amount sum (RC5) to check the export against");
        }
        if (run.writtenTransactionCount() != exported.transactionCount()
                || run.writtenAmountSum().compareTo(exported.amountSum()) != 0) {
            throw new ExportRefusedException("run " + run.runId() + " fails RC5: the calculation recorded "
                    + run.writtenTransactionCount() + " transactions with sum " + run.writtenAmountSum()
                    + ", the tables hold " + exported.transactionCount() + " with sum " + exported.amountSum());
        }
    }

    private static void discard(Path stagingDir) {
        try {
            deleteTree(stagingDir);
        } catch (IOException e) {
            log.warn("staging directory of the export could not be removed", e);
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
