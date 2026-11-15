package com.carddemo.interest.load;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.support.TransactionTemplate;

import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.jdbc.load.InterestRunLoadJournal;
import com.carddemo.interest.jdbc.load.InterestRunLoadJournal.Loaded;
import com.carddemo.interest.jdbc.load.ReplicaTableWriter;
import com.carddemo.interest.load.LoadOutcome.TableLoad;

/**
 * The work of loadExtractsStep (architecture.md sections 2, 3 and 5; strategy TL3, RC1-RC4). The
 * four unloaded files are read and decoded first, so that an undecodable record is found before a
 * table is touched; then each table is replaced in a transaction of its own (TRUNCATE, every
 * INSERT, and the comparison of the table's count and sums with the file's); last the figures go
 * to carddemo.interest_run. Loading again replaces the same rows: PostgreSQL is a copy of the
 * mainframe files while REL-02 keeps the mainframe as the source of record (REL-01: file bridge).
 */
public final class ExtractLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractLoader.class);

    private final ReplicaTableWriter tables;
    private final InterestRunLoadJournal journal;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public ExtractLoader(ReplicaTableWriter tables, InterestRunLoadJournal journal, TransactionTemplate transaction,
            Clock clock) {
        this.tables = tables;
        this.journal = journal;
        this.transaction = transaction;
        this.clock = clock;
    }

    /**
     * Replaces the four tables from the files of inputDir. A record that cannot be decoded gives an
     * outcome with the reject list and a FAILED run; a file that cannot be read or a table that
     * cannot be replaced stops the run (standards S6.2), also with a FAILED run.
     */
    public LoadOutcome load(Path inputDir, RecordFormat format, String parmDate) {
        Instant startedAt = clock.instant();
        ExtractDecoder decoder = new ExtractDecoder(format);
        Map<ExtractFile, DecodedExtract> extracts = new EnumMap<>(ExtractFile.class);
        List<RejectedRecord> rejects = new ArrayList<>();
        try {
            for (ExtractFile file : ExtractFile.values()) {
                Optional<DecodedExtract> extract = decoder.decode(file, read(inputDir, file), rejects);
                extract.ifPresent(decoded -> extracts.put(file, decoded));
            }
        } catch (InterestRunStoppedException e) {
            journal.failed(parmDate, startedAt, clock.instant());
            throw e;
        }
        if (!rejects.isEmpty()) {
            long runId = journal.failed(parmDate, startedAt, clock.instant());
            LOG.error("loadExtractsStep run {} FAILED: {} rejected records, no table was changed", runId, rejects.size());
            return new LoadOutcome(runId, List.of(), rejects, null);
        }
        List<TableLoad> loads = new ArrayList<>();
        for (DecodedExtract extract : extracts.values()) {
            try {
                loads.add(transaction.execute(status -> replace(extract)));
            } catch (DataAccessException | InterestRunStoppedException e) {
                journal.failed(parmDate, startedAt, clock.instant());
                if (e instanceof InterestRunStoppedException stopped) {
                    throw stopped;
                }
                throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT,
                        "cannot replace table " + extract.file().table() + " from file " + extract.file().fileName(), e);
            }
        }
        long runId = journal.loaded(parmDate, startedAt,
                loads.stream().map(load -> new Loaded(load.file(), load.figures())).toList());
        ExtractProfile profile = ExtractProfile.of(extracts, format.codePage().space());
        for (TableLoad load : loads) {
            LOG.info("loadExtractsStep run {} table {} count {} sums {} keyRangeOfFile {}", runId, load.file().table(),
                    load.figures().count(), load.figures().sums(), load.isKeyRangeOfFile());
        }
        LOG.info("loadExtractsStep run {} LOADED profile {}", runId, profile);
        return new LoadOutcome(runId, loads, List.of(), profile);
    }

    /** One transaction: the table holds the file, with the file's count and sums, or what it held before. */
    private TableLoad replace(DecodedExtract extract) {
        ExtractFile file = extract.file();
        tables.replace(extract);
        LoadFigures loaded = tables.figures(file, List.copyOf(extract.figures().sums().keySet()));
        if (!loaded.sameAs(extract.figures())) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "table " + file.table()
                    + " does not hold the count and sums of file " + file.fileName() + " (RC1, RC2)");
        }
        return new TableLoad(file, loaded, isKeyRangeOfFile(extract));
    }

    /** RC4. Distinct keys are proven by the decoder and by the primary key. */
    private boolean isKeyRangeOfFile(DecodedExtract extract) {
        List<RecordValues> rows = extract.rows();
        if (rows.isEmpty()) {
            return true;
        }
        ExtractFile file = extract.file();
        return tables.edgeKey(file, false).equals(ExtractDecoder.key(file, rows.get(0)))
                && tables.edgeKey(file, true).equals(ExtractDecoder.key(file, rows.get(rows.size() - 1)));
    }

    private static byte[] read(Path inputDir, ExtractFile file) {
        try {
            return Files.readAllBytes(inputDir.resolve(file.fileName()));
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.OPEN_INPUT, "cannot read input file " + file.fileName(), e);
        }
    }
}
