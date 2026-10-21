package com.carddemo.batch.jobs;

import java.util.Optional;

import com.carddemo.batch.RunProperties;
import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.DailyTransaction;
import com.carddemo.batch.domain.PostingRules;
import com.carddemo.batch.domain.RejectReason;
import com.carddemo.batch.domain.Transaction;
import com.carddemo.batch.store.BatchRepository;
import com.carddemo.batch.store.Datasets;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/** POSTTRAN: validate each daily transaction in file order, then post it or write it to the reject file. */
public class PostTranTasklet implements Tasklet {

    private final BatchRepository repo;
    private final RunProperties run;

    public PostTranTasklet(BatchRepository repo, RunProperties run) {
        this.repo = repo;
        this.run = run;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        int processed = 0;
        int rejected = 0;
        for (DailyTransaction d : repo.dailyTransactions()) {
            processed++;
            Optional<CardXref> xref = repo.xrefByCard(d.cardNumber());
            Optional<Account> account = xref.flatMap(x -> repo.account(x.accountId()));
            Optional<RejectReason> reason = PostingRules.validate(d, xref, account);
            if (reason.isPresent()) {
                rejected++;
                repo.reject(d, reason.get().code(), reason.get().description());
                continue;
            }
            long acctId = xref.get().accountId();
            repo.addToCategoryBalance(acctId, d.typeCode(), d.categoryCode(), d.amount());
            repo.rewrite(PostingRules.post(account.get(), d.amount()));
            repo.write(Datasets.POSTED, new Transaction(d.id(), d.typeCode(), d.categoryCode(), d.source(),
                    d.description(), d.amount(), d.merchantId(), d.merchantName(), d.merchantCity(),
                    d.merchantZip(), d.cardNumber(), d.originTimestamp(), run.runTimestamp()));
        }
        contribution.incrementReadCount();
        contribution.getStepExecution().getExecutionContext().putInt("processed", processed);
        contribution.getStepExecution().getExecutionContext().putInt("rejected", rejected);
        return RepeatStatus.FINISHED;
    }
}
