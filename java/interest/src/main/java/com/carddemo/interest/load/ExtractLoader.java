package com.carddemo.interest.load;

import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.jdbc.ReplicaTableWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * interest-loader (architecture.md section 2): replaces the four replica tables from the unloaded
 * files and records the reconciliation figures in interest_run. It implements the proposed option
 * of three open decisions: REL-01 (file bridge at the dataset boundary), REL-02 (the mainframe
 * file is the system of record; the table is a copy replaced for each run) and REL-08 (cp037 and
 * C/D signs in the host format).
 *
 * <p>Order of work. All four files are decoded and checked before any table is touched, so one
 * undecodable record leaves the four tables as they were (TL3). Then each table is replaced in a
 * transaction of its own - TRUNCATE, the INSERTs, the figures of both sides compared (RC1, RC2,
 * RC4), the rows read back and encoded again (RC3), the figures written to interest_run
 * (architecture.md section 5). A failure rolls that table back, sets interest_run.status FAILED and
 * stops the run with the one stop exception (standards S6.2). Loading twice gives the same rows.
 */
public final class ExtractLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractLoader.class);

    private final JdbcTemplate jdbc;
    private final ReplicaTableWriter tables;
    private final LoadRunRecord runRecord;
    private final TransactionTemplate transaction;

    public ExtractLoader(JdbcTemplate jdbc, PlatformTransactionManager transactionManager, Clock clock) {
        this.jdbc = jdbc;
        this.tables = new ReplicaTableWriter(jdbc);
        this.runRecord = new LoadRunRecord(jdbc, clock);
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public LoadReport load(LoadRequest request) {
        long runId = transaction.execute(status -> runRecord.start(request.parmDate()));
        try {
            List<CheckedExtract> extracts = checkAll(request);
            Map<ExtractFile, TableFigures> figures = new EnumMap<>(ExtractFile.class);
            long g02Differences = 0;
            long g03Differences = 0;
            long nonBlankFillers = 0;
            for (CheckedExtract extract : extracts) {
                figures.put(extract.file(), transaction.execute(status -> replaceTable(runId, extract, request.format())));
                g02Differences += extract.g02Differences();
                g03Differences += extract.g03Differences();
                nonBlankFillers += extract.nonBlankFillers();
            }
            ExtractProfile profile = profile(g02Differences, g03Differences, nonBlankFillers);
            transaction.executeWithoutResult(status -> runRecord.markLoaded(runId));
            LoadReport report = new LoadReport(runId, figures, g02Differences, g03Differences, profile);
            logFigures(report);
            return report;
        } catch (InterestRunStoppedException stopped) {
            failed(runId, stopped);
            throw stopped;
        } catch (RuntimeException e) {
            failed(runId, e);
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "the load of the replica tables failed", e);
        }
    }

    private void failed(long runId, RuntimeException cause) {
        transaction.executeWithoutResult(status -> runRecord.markFailed(runId));
        LOG.error("interest_run {} step loadExtractsStep FAILED: {}", runId, cause.getMessage());
    }

    /** TL3: decode every record of the four files; with one reject nothing is loaded. */
    private List<CheckedExtract> checkAll(LoadRequest request) {
        ExtractReader reader = new ExtractReader(request.inputDir(), request.format());
        ExtractCheck check = new ExtractCheck(request.format().codePage());
        List<CheckedExtract> extracts = new ArrayList<>();
        List<LoadReject> rejects = new ArrayList<>();
        for (ExtractFile file : ExtractFile.values()) {
            CheckedExtract extract = check.check(file, file.fileName(request.format()), reader.read(file));
            extracts.add(extract);
            rejects.addAll(extract.rejects());
        }
        writeRejectList(request.rejectFile(), rejects);
        if (!rejects.isEmpty()) {
            LoadReject first = rejects.get(0);
            throw new InterestRunStoppedException(StopReason.RECORD_REJECTED, rejects.size() + " rejected, the first is file "
                    + first.file() + " record " + first.recordNumber() + " field " + first.field() + "; no table was changed; reject list "
                    + request.rejectFile().getFileName());
        }
        return extracts;
    }

    /** A load without rejects leaves no reject list, also not the one of an earlier attempt. */
    private static void writeRejectList(Path rejectFile, List<LoadReject> rejects) {
        try {
            if (rejects.isEmpty()) {
                Files.deleteIfExists(rejectFile);
                return;
            }
            Files.createDirectories(rejectFile.toAbsolutePath().getParent());
            Files.write(rejectFile, rejects.stream().map(LoadReject::line).toList(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write the reject list", e);
        }
    }

    /** One transaction: the table is replaced, proven equal to its file, and its figures are recorded. */
    private TableFigures replaceTable(long runId, CheckedExtract extract, ExtractFormat format) {
        ExtractFile file = extract.file();
        tables.replace(file, extract.rows());
        TableFigures ofTable = tables.figures(file);
        TableFigures ofFile = figuresOfFile(extract);
        if (!sameFigures(ofFile, ofTable)) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "table " + file.table()
                    + " does not reconcile with file " + file.fileName(format) + " (RC1, RC2, RC4): counts or sums differ");
        }
        if (!sameImages(extract.encodedRows(), encodedRowsOfTable(file, format))) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "table " + file.table()
                    + " encoded again differs from file " + file.fileName(format) + " (RC3)");
        }
        runRecord.recordTable(runId, file, ofTable, extract.g02Differences(), extract.g03Differences());
        return ofTable;
    }

    private static TableFigures figuresOfFile(CheckedExtract extract) {
        ExtractFile file = extract.file();
        Map<String, BigDecimal> sums = new LinkedHashMap<>();
        file.summedFields().forEach(field -> sums.put(file.column(field),
                extract.rows().stream().map(row -> row.signed(field)).reduce(BigDecimal.ZERO, BigDecimal::add)));
        file.hashTotalField().ifPresent(field -> sums.put(file.column(field),
                extract.rows().stream().map(row -> BigDecimal.valueOf(row.unsigned(field))).reduce(BigDecimal.ZERO, BigDecimal::add)));
        // The check has refused a second record with the same key, so every row is a distinct key (RC4).
        return new TableFigures(extract.rows().size(), extract.rows().size(), sums);
    }

    private static boolean sameFigures(TableFigures ofFile, TableFigures ofTable) {
        if (ofFile.rowCount() != ofTable.rowCount() || ofFile.keyCount() != ofTable.keyCount()
                || !ofFile.sums().keySet().equals(ofTable.sums().keySet())) {
            return false;
        }
        return ofFile.sums().entrySet().stream().allMatch(sum -> sum.getValue().compareTo(ofTable.sum(sum.getKey())) == 0);
    }

    private List<byte[]> encodedRowsOfTable(ExtractFile file, ExtractFormat format) {
        RecordCodec codec = new RecordCodec(file.layout(), format.codePage());
        List<byte[]> images = new ArrayList<>();
        List<RecordValues> rows = tables.rowsInKeyOrder(file);
        for (int i = 0; i < rows.size(); i++) {
            images.add(codec.encode(rows.get(i), new RecordSource("table " + file.table(), i + 1)));
        }
        return images;
    }

    /** The same records on both sides, whatever the order of the file. */
    private static boolean sameImages(List<byte[]> ofFile, List<byte[]> ofTable) {
        HexFormat hex = HexFormat.of();
        return ofFile.stream().map(hex::formatHex).sorted().toList().equals(ofTable.stream().map(hex::formatHex).sorted().toList());
    }

    /** TL4, on the loaded tables. */
    private ExtractProfile profile(long g02Differences, long g03Differences, long nonBlankFillers) {
        long accountsWithSeveralCards = count("SELECT count(*) FROM (SELECT xref_acct_id FROM carddemo.card_xref"
                + " GROUP BY xref_acct_id HAVING count(*) > 1) several");
        long accountsWithMixedTypeCodes = count("SELECT count(*) FROM (SELECT trancat_acct_id FROM carddemo.tran_cat_balance"
                + " GROUP BY trancat_acct_id HAVING bool_or(trancat_type_cd ~ '[0-9]') AND bool_or(trancat_type_cd ~ '[^0-9 ]')) mixed");
        long inactiveAccounts = count("SELECT count(*) FROM carddemo.account WHERE acct_active_status <> 'Y'");
        long balancesWithoutRate = count("SELECT count(*) FROM carddemo.tran_cat_balance b"
                + " JOIN carddemo.account a ON a.acct_id = b.trancat_acct_id"
                + " WHERE NOT EXISTS (SELECT 1 FROM carddemo.disclosure_group d WHERE d.dis_tran_type_cd = b.trancat_type_cd"
                + " AND d.dis_tran_cat_cd = b.trancat_cd AND d.dis_acct_group_id IN (a.acct_group_id, 'DEFAULT'))");
        return new ExtractProfile(accountsWithSeveralCards, accountsWithMixedTypeCodes, g03Differences, g02Differences,
                nonBlankFillers, inactiveAccounts, balancesWithoutRate);
    }

    private long count(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    /** Counts and sums only (standards S6.5). */
    private static void logFigures(LoadReport report) {
        report.figures().forEach((file, figures) -> LOG.info("interest_run {} step loadExtractsStep table {} rows={} keys={} sums={}",
                report.runId(), file.table(), figures.rowCount(), figures.keyCount(), figures.sums()));
        LOG.info("interest_run {} step loadExtractsStep LOADED g02={} g03={} profile findings={} {}",
                report.runId(), report.g02Differences(), report.g03Differences(), report.profile().findings(), report.profile());
    }
}
