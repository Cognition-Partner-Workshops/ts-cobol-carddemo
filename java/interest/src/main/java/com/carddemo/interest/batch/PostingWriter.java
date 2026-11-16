package com.carddemo.interest.batch;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.record.AccountRecord;
import com.carddemo.interest.record.TransactionRecord;

/**
 * REWRITE of ACCTFILE and WRITE to SYSTRAN(+1). After an abend the writes that preceded it are
 * committed (as they are on the legacy files) and the step is stopped.
 */
public class PostingWriter implements ItemWriter<StepOutput> {

    private final JdbcTemplate jdbc;
    private StepExecution stepExecution;

    public PostingWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @BeforeStep
    public void beforeStep(StepExecution execution) {
        this.stepExecution = execution;
    }

    @Override
    public void write(Chunk<? extends StepOutput> chunk) {
        List<Object[]> rewrites = new ArrayList<>();
        List<Object[]> inserts = new ArrayList<>();
        boolean abended = false;
        for (StepOutput out : chunk) {
            AccountRecord a = out.rewrittenAccount();
            if (a != null) {
                rewrites.add(new Object[] {a.currBal(), a.currCycCredit(), a.currCycDebit(), a.acctId()});
            }
            TransactionRecord t = out.transaction();
            if (t != null) {
                inserts.add(new Object[] {out.writeSeq(), t.tranId(), t.tranTypeCd(), t.tranCatCd(), t.tranSource(),
                        t.tranDesc(), t.tranAmt(), t.merchantId(), t.merchantName(), t.merchantCity(),
                        t.merchantZip(), t.cardNum(), t.origTs(), t.procTs()});
            }
            abended |= out.abended();
        }
        jdbc.batchUpdate("UPDATE account SET curr_bal = ?, curr_cyc_credit = ?, curr_cyc_debit = ? WHERE acct_id = ?",
                rewrites);
        jdbc.batchUpdate("INSERT INTO systran (write_seq, tran_id, tran_type_cd, tran_cat_cd, tran_source, tran_desc,"
                + " tran_amt, merchant_id, merchant_name, merchant_city, merchant_zip, card_num, orig_ts, proc_ts)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", inserts);
        if (abended && stepExecution != null) {
            stepExecution.setTerminateOnly();
        }
    }
}
