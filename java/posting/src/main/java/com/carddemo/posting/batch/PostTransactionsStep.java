package com.carddemo.posting.batch;

import com.carddemo.posting.StepFiles;
import com.carddemo.posting.record.Records;
import com.carddemo.posting.record.Records.Account;
import com.carddemo.posting.record.Records.CardXref;
import com.carddemo.posting.record.Records.TranCatBalance;
import com.carddemo.posting.record.Records.Transaction;
import com.carddemo.posting.record.Text;
import com.carddemo.posting.record.Zoned;
import com.carddemo.posting.store.PostingStore;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * POSTTRAN STEP15 (CBTRN02C): validate each daily transaction against CARDXREF and ACCTDATA,
 * post the accepted ones (TCATBALF, ACCTDATA, TRANSACT) and write the rest to DALYREJS(+1).
 * The processor applies the balance updates itself so that later records of the same chunk see
 * them, exactly like the record-at-a-time COBOL loop.
 */
@Component
public class PostTransactionsStep implements ItemStreamReader<PostTransactionsStep.Daily>,
        ItemProcessor<PostTransactionsStep.Daily, PostTransactionsStep.Outcome>,
        ItemWriter<PostTransactionsStep.Outcome>, StepExecutionListener {

    static final String JOB = "POSTTRAN";
    static final String STEP = "STEP15";
    static final int REJECT_LRECL = 430;

    /** A DALYTRAN record as read (kept verbatim for the reject file) and as parsed. */
    public record Daily(String record, Transaction transaction) {
    }

    /** Posted transaction, or the reject trailer (WS-VALIDATION-TRAILER) for a failed one. */
    public record Outcome(Daily daily, Transaction posted, int reason, String reasonDescription) {
    }

    private final StepFiles run;
    private final PostingStore store;
    private java.util.Iterator<String> dailyRecords;
    private BufferedWriter rejectFile;
    private long transactionCount;
    private long rejectCount;

    public PostTransactionsStep(StepFiles run, PostingStore store) {
        this.run = run;
        this.store = store;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        transactionCount = 0;
        rejectCount = 0;
        run.display("START OF EXECUTION OF PROGRAM CBTRN02C");
    }

    @Override
    public void open(ExecutionContext executionContext) {
        try {
            rejectFile = Files.newBufferedWriter(StepFiles.dd("DALYREJS"), StandardCharsets.ISO_8859_1);
            dailyRecords = StepFiles.readFixed(StepFiles.dd("DALYTRAN"), Transaction.LRECL).iterator();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Daily read() throws IOException {
        if (dailyRecords == null || !dailyRecords.hasNext()) {
            return null;
        }
        String record = dailyRecords.next();
        return new Daily(record, Transaction.parse(record));
    }

    @Override
    public Outcome process(Daily daily) {
        transactionCount++;
        Transaction tran = daily.transaction();
        Optional<CardXref> xref = store.findXref(tran.cardNum());
        if (xref.isEmpty()) {
            return new Outcome(daily, null, 100, "INVALID CARD NUMBER FOUND");
        }
        Optional<Account> found = store.findAccount(xref.get().acctId());
        if (found.isEmpty()) {
            return new Outcome(daily, null, 101, "ACCOUNT RECORD NOT FOUND");
        }
        Account account = found.get();
        int reason = 0;
        String reasonDescription = "";
        // Quirk: the limit check uses this cycle's credits minus debits, not ACCT-CURR-BAL.
        BigDecimal tempBal = Zoned.fit(account.currCycCredit().subtract(account.currCycDebit())
                .add(tran.amount()), 9, 2);
        if (account.creditLimit().compareTo(tempBal) < 0) {
            reason = 102;
            reasonDescription = "OVERLIMIT TRANSACTION";
        }
        // Both checks run; an expired account overwrites the over-limit reason.
        if (Text.pad(account.expirationDate(), 10).compareTo(Text.slice(tran.origTs(), 0, 10)) < 0) {
            reason = 103;
            reasonDescription = "TRANSACTION RECEIVED AFTER ACCT EXPIRATION";
        }
        if (reason != 0) {
            return new Outcome(daily, null, reason, reasonDescription);
        }
        Transaction posted = tran.withProcTs(run.db2Timestamp());
        updateTranCatBalance(account.acctId(), tran);
        updateAccount(account, tran.amount());
        return new Outcome(daily, posted, 0, "");
    }

    private void updateTranCatBalance(long acctId, Transaction tran) {
        Optional<TranCatBalance> balance = store.findTranCatBalance(acctId, tran.typeCd(), tran.catCd());
        if (balance.isEmpty()) {
            run.display("TCATBAL record not found for key : "
                    + TranCatBalance.key(acctId, tran.typeCd(), tran.catCd()) + ".. Creating.");
            store.insert(new TranCatBalance(acctId, tran.typeCd(), tran.catCd(),
                    Zoned.fit(tran.amount(), 9, 2)));
            return;
        }
        TranCatBalance current = balance.get();
        store.updateBalance(new TranCatBalance(acctId, current.typeCd(), current.catCd(),
                Zoned.fit(current.balance().add(tran.amount()), 9, 2)));
    }

    private void updateAccount(Account account, BigDecimal amount) {
        boolean credit = amount.signum() >= 0;
        store.updateCurrentBalances(new Account(account.acctId(), account.activeStatus(),
                Zoned.fit(account.currBal().add(amount), 10, 2), account.creditLimit(), account.cashCreditLimit(),
                account.openDate(), account.expirationDate(), account.reissueDate(),
                credit ? Zoned.fit(account.currCycCredit().add(amount), 10, 2) : account.currCycCredit(),
                credit ? account.currCycDebit() : Zoned.fit(account.currCycDebit().add(amount), 10, 2),
                account.addrZip(), account.groupId()));
    }

    @Override
    public void write(Chunk<? extends Outcome> chunk) throws IOException {
        for (Outcome outcome : chunk) {
            if (outcome.posted() != null) {
                store.insert(outcome.posted());
            } else {
                rejectCount++;
                String reject = Records.reject(outcome.daily().record(), outcome.reason(),
                        outcome.reasonDescription());
                rejectFile.write(Text.pad(reject, REJECT_LRECL));
            }
        }
        rejectFile.flush();
    }

    @Override
    public void close() {
        try {
            if (rejectFile != null) {
                rejectFile.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        run.display("TRANSACTIONS PROCESSED :" + Text.digits(transactionCount, 9));
        run.display("TRANSACTIONS REJECTED  :" + Text.digits(rejectCount, 9));
        run.display("END OF EXECUTION OF PROGRAM CBTRN02C");
        run.returnCode(rejectCount > 0 ? 4 : 0);
        return stepExecution.getExitStatus();
    }
}
