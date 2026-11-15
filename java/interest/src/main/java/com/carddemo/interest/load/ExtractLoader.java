package com.carddemo.interest.load;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.DecodedRecord;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.jdbc.ReplicaTable;
import com.carddemo.interest.jdbc.ReplicaTableReader;
import com.carddemo.interest.jdbc.ReplicaTableWriter;

/**
 * Loads the four unloaded files into the four replica tables (INT-T05, strategy item TL3).
 *
 * <p>All four files are decoded before any table is touched, so a record that cannot be decoded
 * leaves the four tables as they were. Then each table is replaced in a transaction of its own:
 * TRUNCATE, the INSERTs, and the checks of RC1, RC2 and RC3 on what was inserted; a failed check
 * rolls that table back. The interest_run row with the figures is written last. Nothing is skipped
 * and nothing is retried (standards S6.2).
 */
@Component
public class ExtractLoader {

    private final ReplicaTableWriter writer;
    private final ReplicaTableReader reader;
    private final LoadRunJournal journal;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public ExtractLoader(ReplicaTableWriter writer, ReplicaTableReader reader, LoadRunJournal journal,
            PlatformTransactionManager transactionManager, Clock clock) {
        this.writer = writer;
        this.reader = reader;
        this.journal = journal;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /**
     * @param rejectList where the reject list is written when a record is rejected; a list left by
     *        an earlier load is removed first
     * @throws InterestRunStoppedException when the load stops; interest_run then holds a FAILED row
     */
    public LoadResult load(Path inputDir, ExtractFormat format, String parmDate, Path rejectList) {
        Instant startedAt = clock.instant();
        try {
            Files.deleteIfExists(rejectList);
            Map<ExtractFile, DecodedExtract> extracts = decodeAll(inputDir, format, rejectList);
            CodePage codePage = format.codePage();
            RoundTrip roundTrip = RoundTrip.NONE;
            Map<ReplicaTable, Boolean> keyRanges = new EnumMap<>(ReplicaTable.class);
            for (DecodedExtract extract : extracts.values()) {
                TableCheck check = transaction.execute(status -> replace(extract, codePage));
                roundTrip = roundTrip.plus(check.roundTrip());
                keyRanges.put(extract.file().table(), check.keyRangeEqualsFileEnds());
            }
            LoadFigures figures = LoadFigures.of(reader::count, reader::sum);
            requireEqual(fileFigures(extracts), figures);
            long runId = transaction.execute(status -> journal.loaded(parmDate, startedAt, figures));
            return new LoadResult(runId, figures, roundTrip, keyRanges, ExtractProfile.of(extracts, codePage));
        } catch (RuntimeException | IOException e) {
            transaction.executeWithoutResult(status -> journal.failed(parmDate, startedAt, clock.instant()));
            if (e instanceof InterestRunStoppedException stopped) {
                throw stopped;
            }
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "the load of the replica tables failed", e);
        }
    }

    private static Map<ExtractFile, DecodedExtract> decodeAll(Path inputDir, ExtractFormat format, Path rejectList)
            throws IOException {
        Map<ExtractFile, DecodedExtract> extracts = new EnumMap<>(ExtractFile.class);
        List<Reject> rejects = new ArrayList<>();
        for (ExtractFile file : ExtractFile.values()) {
            DecodedExtract extract = ExtractDecoder.decode(inputDir, file, format);
            extracts.put(file, extract);
            rejects.addAll(extract.rejects());
        }
        if (!rejects.isEmpty()) {
            if (rejectList.getParent() != null) {
                Files.createDirectories(rejectList.getParent());
            }
            List<String> lines = new ArrayList<>();
            lines.add("file\trecord\tfield\treason");
            rejects.forEach(reject -> lines.add(reject.line()));
            Files.write(rejectList, lines, StandardCharsets.UTF_8);
            throw new InterestRunStoppedException(StopReason.RECORD_REJECTED,
                    rejects.size() + " rejected, no table was loaded; reject list " + rejectList.getFileName());
        }
        return extracts;
    }

    /** One table, inside its transaction: replace, then RC1 and RC3 against the file and the key ends of RC4. */
    private TableCheck replace(DecodedExtract extract, CodePage codePage) {
        ReplicaTable table = extract.file().table();
        writer.replace(table, extract.records());
        List<byte[]> rows = reader.records(table, codePage);
        if (rows.size() != extract.sources().size()) {
            throw new InterestRunStoppedException(StopReason.RECORD_REJECTED,
                    "RC1: " + table.tableName() + " holds " + rows.size() + " rows, file " + extract.fileName() + " has "
                            + extract.sources().size() + " records");
        }
        Map<String, byte[]> rowOfKey = new HashMap<>();
        rows.forEach(row -> rowOfKey.put(RoundTrip.key(table, row), row));
        RoundTrip roundTrip = RoundTrip.NONE;
        for (byte[] source : extract.sources()) {
            roundTrip = roundTrip.plus(RoundTrip.compare(table.layout(), source, rowOfKey.get(RoundTrip.key(table, source)), codePage));
        }
        if (roundTrip.other() > 0) {
            throw new InterestRunStoppedException(StopReason.RECORD_REJECTED,
                    "RC3: " + roundTrip.other() + " differences between " + table.tableName() + " and file "
                            + extract.fileName() + " that neither G02 nor G03 explains");
        }
        boolean keyEnds = rows.isEmpty()
                || (RoundTrip.key(table, rows.get(0)).equals(RoundTrip.key(table, extract.sources().get(0)))
                        && RoundTrip.key(table, rows.get(rows.size() - 1))
                                .equals(RoundTrip.key(table, extract.sources().get(extract.sources().size() - 1))));
        return new TableCheck(roundTrip, keyEnds);
    }

    private static LoadFigures fileFigures(Map<ExtractFile, DecodedExtract> extracts) {
        Map<ReplicaTable, List<DecodedRecord>> records = new EnumMap<>(ReplicaTable.class);
        extracts.values().forEach(extract -> records.put(extract.file().table(), extract.records()));
        return LoadFigures.of(
                table -> records.get(table).size(),
                (table, field) -> records.get(table).stream().map(record -> record.number(field))
                        .reduce(BigDecimal.ZERO.setScale(table.layout().field(field).scale()), BigDecimal::add));
    }

    private static void requireEqual(LoadFigures files, LoadFigures tables) {
        if (!files.equals(tables)) {
            throw new InterestRunStoppedException(StopReason.RECORD_REJECTED,
                    "RC1/RC2: the counts and sums of the tables differ from those of the files");
        }
    }

    private record TableCheck(RoundTrip roundTrip, boolean keyRangeEqualsFileEnds) {
    }
}
