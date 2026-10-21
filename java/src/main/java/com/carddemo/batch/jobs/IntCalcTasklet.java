package com.carddemo.batch.jobs;

import java.math.BigDecimal;

import com.carddemo.batch.RunProperties;
import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.CategoryBalance;
import com.carddemo.batch.domain.InterestRules;
import com.carddemo.batch.domain.PurchasePromotion;
import com.carddemo.batch.domain.Transaction;
import com.carddemo.batch.store.BatchRepository;
import com.carddemo.batch.store.Datasets;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * INTCALC: walk the category balances in key order, compute monthly interest per category and
 * add each account's total to its balance when the account number changes.
 * GOLDPROMO1 purchase interest uses the R19 promotional rate for the account's first three cycles.
 *
 * <p>The account update happens only on a change of account number. The COBOL loop exits
 * before its end-of-file branch runs, so the last account's interest is written as a
 * transaction but never added to that account's balance. This port keeps the observed
 * behavior; see the CBACT04C specification.
 */
public class IntCalcTasklet implements Tasklet {

    private final BatchRepository repo;
    private final RunProperties run;

    public IntCalcTasklet(BatchRepository repo, RunProperties run) {
        this.repo = repo;
        this.run = run;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        Long lastAccountId = null;
        Account account = null;
        CardXref xref = null;
        BigDecimal totalInterest = BigDecimal.ZERO;
        int promoCycle = 0;
        int suffix = 0;
        for (CategoryBalance cat : repo.categoryBalancesInKeyOrder()) {
            if (lastAccountId == null || cat.accountId() != lastAccountId) {
                if (account != null) {
                    repo.rewrite(InterestRules.applyToAccount(account, totalInterest));
                }
                totalInterest = BigDecimal.ZERO;
                lastAccountId = cat.accountId();
                final long id = cat.accountId();
                account = repo.account(id).orElseThrow(() -> new IllegalStateException("account " + id + " missing"));
                xref = repo.xrefByAccount(id).orElseThrow(() -> new IllegalStateException("no card for account " + id));
                promoCycle = PurchasePromotion.isMember(account.groupId()) ? repo.startPromotionCycle(account.id()) : 0;
            }
            BigDecimal rate = PurchasePromotion.rate(account.groupId(), cat, promoCycle,
                    rateFor(account.groupId(), cat));
            if (rate.signum() != 0) {
                BigDecimal monthly = InterestRules.monthlyInterest(cat.balance(), rate, run.interestRounding());
                totalInterest = totalInterest.add(monthly);
                suffix++;
                repo.write(Datasets.INTEREST, new Transaction(
                        InterestRules.interestTransactionId(run.interestParmDate(), suffix), "01", 5, "System",
                        InterestRules.interestDescription(account.id()), monthly, 0L, "", "", "",
                        xref.cardNumber(), run.runTimestamp(), run.runTimestamp()));
            }
        }
        return RepeatStatus.FINISHED;
    }

    /** The account's group rate, or the DEFAULT group's rate for the same type and category. */
    private BigDecimal rateFor(String groupId, CategoryBalance cat) {
        return repo.interestRate(groupId, cat.typeCode(), cat.categoryCode())
                .or(() -> repo.interestRate(InterestRules.DEFAULT_GROUP, cat.typeCode(), cat.categoryCode()))
                .orElseThrow(() -> new IllegalStateException("no DEFAULT disclosure rate for " + cat));
    }
}
