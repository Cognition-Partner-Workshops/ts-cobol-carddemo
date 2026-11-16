package com.carddemo.posting.batch;

import com.carddemo.posting.ChainRun;
import com.carddemo.posting.record.Records.Account;
import com.carddemo.posting.record.Records.CardXref;
import com.carddemo.posting.record.Records.TranCatBalance;
import com.carddemo.posting.record.Records.TranCategory;
import com.carddemo.posting.record.Records.TranType;
import com.carddemo.posting.record.Records.Transaction;
import com.carddemo.posting.store.PostingStore;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * The chain's setup jobs (ACCTFILE, XREFFILE, TCATBALF, TRANTYPE, TRANCATG, TRANFILE): delete
 * and redefine each cluster and REPRO the case's flat file into it.
 */
@Component
public class LoadClustersTasklet implements Tasklet {

    private final ChainRun run;
    private final PostingStore store;

    public LoadClustersTasklet(ChainRun run, PostingStore store) {
        this.run = run;
        this.store = store;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        store.clear();
        load("AWS.M2.CARDDEMO.ACCTDATA.PS", Account::parse, store::insert);
        load("AWS.M2.CARDDEMO.CARDXREF.PS", CardXref::parse, store::insert);
        load("AWS.M2.CARDDEMO.TCATBALF.PS", TranCatBalance::parse, store::insert);
        load("AWS.M2.CARDDEMO.TRANTYPE.PS", TranType::parse, store::insert);
        load("AWS.M2.CARDDEMO.TRANCATG.PS", TranCategory::parse, store::insert);
        load("AWS.M2.CARDDEMO.DALYTRAN.PS.INIT", Transaction::parse, store::insert);
        return RepeatStatus.FINISHED;
    }

    private <T> void load(String dsn, Function<String, T> parser, Consumer<T> insert) {
        run.readInput(dsn).stream().map(parser).forEach(insert);
    }
}
