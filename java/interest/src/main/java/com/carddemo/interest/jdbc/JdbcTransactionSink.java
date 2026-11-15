package com.carddemo.interest.jdbc;

import java.math.BigDecimal;
import java.util.Arrays;

import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.TransactionRecord;
import com.carddemo.interest.domain.InterestTransaction;
import com.carddemo.interest.port.TransactionSink;

/**
 * FI-3 on carddemo.interest_transaction: one row per interest transaction of the run, numbered in
 * write order (seq_no 1..n, the order of the SYSTRAN generation; CBACT04C:500). The merchant text
 * fields are empty and the FILLER is spaces, which the program never sets (G05, INT-T04); CHAR
 * columns pad themselves.
 */
public final class JdbcTransactionSink implements TransactionSink {

    private static final String INSERT = """
            INSERT INTO carddemo.interest_transaction
                (run_id, seq_no, tran_id, tran_type_cd, tran_cat_cd, tran_source, tran_desc, tran_amt,
                 tran_merchant_id, tran_merchant_name, tran_merchant_city, tran_merchant_zip,
                 tran_card_num, tran_orig_ts, tran_proc_ts, tran_filler)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, '', '', '', ?, ?, ?, ?)""";

    private final JdbcTemplate jdbc;
    private final long interestRunId;
    private final byte[] blankFiller;
    private int lastSeqNo;

    public JdbcTransactionSink(JdbcTemplate jdbc, long interestRunId, int lastSeqNoBefore) {
        this.jdbc = jdbc;
        this.interestRunId = interestRunId;
        this.lastSeqNo = lastSeqNoBefore;
        this.blankFiller = new byte[TransactionRecord.LAYOUT.field("FILLER").length()];
        Arrays.fill(blankFiller, CodePage.FIXTURE.space());
    }

    @Override
    public void write(InterestTransaction transaction) {
        jdbc.update(INSERT,
                interestRunId,
                lastSeqNo + 1,
                transaction.id(),
                transaction.typeCode(),
                BigDecimal.valueOf(transaction.categoryCode()),
                transaction.source(),
                transaction.description(),
                transaction.amount(),
                BigDecimal.valueOf(transaction.merchantId()),
                transaction.cardNumber(),
                transaction.originTimestamp(),
                transaction.processedTimestamp(),
                blankFiller);
        lastSeqNo++;
    }

    /** The seq_no of the last row written by the run; part of the step's restart state. */
    public int lastSeqNo() {
        return lastSeqNo;
    }
}
