package com.carddemo.interest.jdbc;

import java.util.Arrays;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.TransactionRecord;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.InterestTransaction;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.port.TransactionSink;

/**
 * FI-3 on carddemo.interest_transaction: the interest transactions of one run in the order written
 * (CBACT04C:500). seq_no is that order, 1..n inside the run; a restart goes on after the last
 * committed seq_no (architecture.md section 6).
 */
public final class JdbcTransactionSink implements TransactionSink {

    public static final String TABLE = "carddemo.interest_transaction";

    /**
     * Data-map rows CVTRA05Y/TRAN-ID to CVTRA05Y/FILLER, then run_id and seq_no. CBACT04C never sets
     * the three merchant texts (INT-T04): they are stored as the blank CHAR(n). The 20-byte FILLER is
     * never set either: it is stored as spaces of the code page (rule E4, gap G05).
     */
    private static final String INSERT = """
            INSERT INTO carddemo.interest_transaction
                   (tran_id, tran_type_cd, tran_cat_cd, tran_source, tran_desc, tran_amt, tran_merchant_id,
                    tran_merchant_name, tran_merchant_city, tran_merchant_zip, tran_card_num, tran_orig_ts, tran_proc_ts,
                    tran_filler, run_id, seq_no)
            VALUES (?, ?, ?, ?, ?, ?, ?, '', '', '', ?, ?, ?, ?, ?, ?)""";

    private final JdbcOperations jdbc;
    private final long runId;
    private final byte[] unsetFiller;
    private int lastSeqNo;

    public JdbcTransactionSink(JdbcOperations jdbc, CodePage codePage, long runId, int lastSeqNo) {
        this.jdbc = jdbc;
        this.runId = runId;
        this.lastSeqNo = lastSeqNo;
        int fillerLength = TransactionRecord.LAYOUT.fields().get(TransactionRecord.LAYOUT.fields().size() - 1).length();
        this.unsetFiller = new byte[fillerLength];
        Arrays.fill(unsetFiller, codePage.space());
    }

    @Override
    public void write(InterestTransaction transaction) {
        try {
            jdbc.update(INSERT, transaction.id(), transaction.typeCode(), transaction.categoryCode(),
                    transaction.source(), transaction.description(), transaction.amount(), transaction.merchantId(),
                    transaction.cardNumber(), transaction.originTimestamp(), transaction.processedTimestamp(),
                    unsetFiller, runId, lastSeqNo + 1);
        } catch (DataAccessException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot insert into table " + TABLE, e);
        }
        lastSeqNo++;
    }

    /** The seq_no of the last transaction written by this run. */
    public int lastSeqNo() {
        return lastSeqNo;
    }
}
