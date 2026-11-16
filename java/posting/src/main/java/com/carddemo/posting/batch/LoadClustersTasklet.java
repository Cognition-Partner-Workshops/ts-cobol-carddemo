package com.carddemo.posting.batch;

import com.carddemo.posting.StepFiles;
import com.carddemo.posting.record.Records.Account;
import com.carddemo.posting.record.Records.CardXref;
import com.carddemo.posting.record.Records.TranCatBalance;
import com.carddemo.posting.record.Records.TranCategory;
import com.carddemo.posting.record.Records.TranType;
import com.carddemo.posting.store.PostingStore;
import java.util.List;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Coexistence adapter: rebuilds the posting schema from the flat images of the KSDS clusters the
 * step reads (the clusters stay the system of record until the neighbouring chains read
 * PostgreSQL). TRANFILE is not loaded: CBTRN02C opens it OUTPUT.
 */
public class LoadClustersTasklet implements Tasklet {

    private final PostingStore store;
    private final List<String> ddnames;

    public LoadClustersTasklet(PostingStore store, String... ddnames) {
        this.store = store;
        this.ddnames = List.of(ddnames);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        store.clear();
        for (String ddname : ddnames) {
            switch (ddname) {
                case "ACCTFILE" -> load(ddname, Account.LRECL).forEach(r -> store.insert(Account.parse(r)));
                case "XREFFILE", "CARDXREF" -> load(ddname, 50).forEach(r -> store.insert(CardXref.parse(r)));
                case "TCATBALF" -> load(ddname, TranCatBalance.LRECL)
                        .forEach(r -> store.insert(TranCatBalance.parse(r)));
                case "TRANTYPE" -> load(ddname, 60).forEach(r -> store.insert(TranType.parse(r)));
                case "TRANCATG" -> load(ddname, 60).forEach(r -> store.insert(TranCategory.parse(r)));
                default -> throw new IllegalArgumentException("no loader for DD " + ddname);
            }
        }
        return RepeatStatus.FINISHED;
    }

    private static List<String> load(String ddname, int lrecl) {
        return StepFiles.readFixed(StepFiles.dd(ddname), lrecl);
    }
}
