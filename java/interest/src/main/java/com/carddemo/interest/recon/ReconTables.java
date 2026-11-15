package com.carddemo.interest.recon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.load.ExtractFile;

/**
 * What the reconciliation report reads of schema carddemo: interest_run, the four replica tables
 * and interest_transaction. It writes nothing (ticket INT-T10, Acceptance: Database assertions).
 * The adapter is {@link JdbcReconTables}.
 */
public interface ReconTables {

    /** One row of interest_run: its key, PARM, status and the loaded_* and written_* figures by column name (null when empty). */
    record RunRow(long runId, String parmDate, String status, Map<String, Object> figures) {
    }

    /** RC1, RC2, RC4 as a table holds them now. */
    record TableFigures(long rows, Map<String, BigDecimal> sums, long distinctKeys) {
    }

    /** One row of a replica table: its key, its fields and the raw bytes of its FILLER column (E4). */
    record TableRow(List<Object> key, RecordValues values, byte[] filler) {
    }

    /** RC5 as interest_transaction holds it for one run. */
    record WrittenFigures(long transactions, BigDecimal amountSum) {
    }

    /** The run with that key, or the run with the highest key when none is given. */
    Optional<RunRow> run(OptionalLong runId);

    TableFigures figures(ExtractFile file, List<String> summedColumns);

    /** Every row of the table in the order of its key (E6: text keys in collation C). */
    void rows(ExtractFile file, Consumer<TableRow> consumer);

    WrittenFigures written(long runId);

    /** The account of the last balance record in key order: the account that ends the input (INT-Q04). */
    OptionalLong lastAccount();

    /** The sum of the transactions of the run written for the cards of one account. */
    BigDecimal interestOfAccount(long runId, long accountId);

    /** A source that cannot be read: every figure of the table side is then NOT AVAILABLE with this reason. */
    static ReconTables unavailable(String reason) {
        return new ReconTables() {
            private IllegalStateException unavailable() {
                return new IllegalStateException(reason);
            }

            @Override
            public Optional<RunRow> run(OptionalLong runId) {
                throw unavailable();
            }

            @Override
            public TableFigures figures(ExtractFile file, List<String> summedColumns) {
                throw unavailable();
            }

            @Override
            public void rows(ExtractFile file, Consumer<TableRow> consumer) {
                throw unavailable();
            }

            @Override
            public WrittenFigures written(long runId) {
                throw unavailable();
            }

            @Override
            public OptionalLong lastAccount() {
                throw unavailable();
            }

            @Override
            public BigDecimal interestOfAccount(long runId, long accountId) {
                throw unavailable();
            }
        };
    }
}
