package com.carddemo.posting.batch;

import com.carddemo.posting.StepFiles;
import com.carddemo.posting.record.Records.Account;
import com.carddemo.posting.record.Records.TranCatBalance;
import com.carddemo.posting.record.Records.Transaction;
import com.carddemo.posting.store.PostingStore;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/** Writes the clusters CBTRN02C changed back to the step's flat images, in key order. */
@Component
public class UnloadClustersTasklet implements Tasklet {

    private final PostingStore store;

    public UnloadClustersTasklet(PostingStore store) {
        this.store = store;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        StepFiles.writeFixed(StepFiles.dd("TRANFILE"),
                store.transactions().stream().map(Transaction::format).toList(), Transaction.LRECL);
        StepFiles.writeFixed(StepFiles.dd("ACCTFILE"),
                store.accounts().stream().map(Account::format).toList(), Account.LRECL);
        StepFiles.writeFixed(StepFiles.dd("TCATBALF"),
                store.tranCatBalances().stream().map(TranCatBalance::format).toList(), TranCatBalance.LRECL);
        return RepeatStatus.FINISHED;
    }
}
