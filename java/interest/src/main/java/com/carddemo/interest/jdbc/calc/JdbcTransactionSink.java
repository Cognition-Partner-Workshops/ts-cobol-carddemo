package com.carddemo.interest.jdbc.calc;

import java.util.Arrays;

import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordLayout.Kind;
import com.carddemo.interest.codec.RecordOrigin;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.codec.TransactionRecord;
import com.carddemo.interest.domain.InterestTransaction;
import com.carddemo.interest.port.TransactionSink;

/**
 * The interest transactions of one run as rows of carddemo.interest_transaction, in the order
 * written (CBACT04C:500; data-map rows CVTRA05Y/* and the columns run_id and seq_no). seq_no
 * continues from the value the last committed chunk saved (architecture.md section 6).
 */
public final class JdbcTransactionSink implements TransactionSink {

    static final String TABLE = "carddemo.interest_transaction";

    private static final String INSERT = "INSERT INTO " + TABLE
            + " (tran_id, tran_type_cd, tran_cat_cd, tran_source, tran_desc, tran_amt, tran_merchant_id,"
            + " tran_merchant_name, tran_merchant_city, tran_merchant_zip, tran_card_num, tran_orig_ts, tran_proc_ts,"
            + " tran_filler, run_id, seq_no) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /** Every value is checked against its picture by the codec before it is stored (standards S3.4). */
    private static final RecordCodec CODEC = RecordFormat.fixture().codec(TransactionRecord.LAYOUT);
    private static final FieldLayout FILLER = TransactionRecord.LAYOUT.fields().stream()
            .filter(field -> field.kind() == Kind.FILLER).findFirst().orElseThrow();

    private final JdbcTemplate jdbc;
    private final long interestRunId;
    private int lastSeqNo;

    public JdbcTransactionSink(JdbcTemplate jdbc, long interestRunId, int lastSeqNo) {
        this.jdbc = jdbc;
        this.interestRunId = interestRunId;
        this.lastSeqNo = lastSeqNo;
    }

    @Override
    public void write(InterestTransaction transaction) {
        int seqNo = lastSeqNo + 1;
        RecordValues values = TransactionRecord.values(transaction);
        byte[] record = CODEC.encode(values, new RecordOrigin(TABLE, seqNo));
        // G05, INT-T04: the FILLER of a new transaction is the spaces the codec writes
        byte[] filler = Arrays.copyOfRange(record, FILLER.offset(), FILLER.end());
        jdbc.update(INSERT,
                values.text("TRAN-ID"),
                values.text("TRAN-TYPE-CD"),
                values.unsigned("TRAN-CAT-CD"),
                values.text("TRAN-SOURCE"),
                values.text("TRAN-DESC"),
                values.signed("TRAN-AMT"),
                values.unsigned("TRAN-MERCHANT-ID"),
                values.text("TRAN-MERCHANT-NAME"),
                values.text("TRAN-MERCHANT-CITY"),
                values.text("TRAN-MERCHANT-ZIP"),
                values.text("TRAN-CARD-NUM"),
                values.text("TRAN-ORIG-TS"),
                values.text("TRAN-PROC-TS"),
                filler,
                interestRunId,
                seqNo);
        lastSeqNo = seqNo;
    }

    /** The seq_no of the last transaction written; saved with the commit. */
    public int lastSeqNo() {
        return lastSeqNo;
    }
}
