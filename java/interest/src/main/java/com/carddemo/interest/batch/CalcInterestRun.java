package com.carddemo.interest.batch;

import java.nio.file.Path;
import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;

import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.AccountInterest;
import com.carddemo.interest.domain.InterestCalculator;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.RunLogLines;
import com.carddemo.interest.domain.TransactionCounter;
import com.carddemo.interest.file.FileRunWorkspace;
import com.carddemo.interest.policy.InterestPolicy;

/**
 * The state of one execution of calcInterestStep: the file adapters of the ports, the calculation
 * and its transaction counter are created when the step starts, so nothing is carried from one
 * run to the next. Reader, processor and writer of the step delegate here.
 */
public class CalcInterestRun implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(CalcInterestRun.class);

    private final InterestPolicy policy;
    private final Clock clock;

    private FileRunWorkspace workspace;
    private InterestCalculator calculator;
    private TransactionCounter counter;

    public CalcInterestRun(InterestPolicy policy, Clock clock) {
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        JobParameters parameters = stepExecution.getJobParameters();
        workspace = null;
        workspace = FileRunWorkspace.open(
                Path.of(parameters.getString(InterestJobParameters.INPUT_DIR)),
                Path.of(parameters.getString(InterestJobParameters.OUTPUT_DIR)),
                parameters.getString(InterestJobParameters.RUN_ID));
        counter = new TransactionCounter();
        calculator = new InterestCalculator(workspace.accountStore(), workspace.cardLookup(), workspace.rateLookup(),
                workspace.runLog(), policy, clock, counter, parameters.getString(InterestJobParameters.PARM_DATE));
        workspace.runLog().line(RunLogLines.START);
    }

    /** Reader: one item is one account with all its balances; null ends the input. */
    public AccountBalances read() {
        return workspace.balanceSource().nextAccount().orElse(null);
    }

    public AccountInterest process(AccountBalances item) {
        return calculator.calculate(item);
    }

    /** Writer: the transactions of each account, then the update of its account break (INT-R07). */
    public void write(Chunk<? extends AccountInterest> chunk) {
        for (AccountInterest result : chunk) {
            result.transactions().forEach(workspace.transactionSink()::write);
            result.newBalance().ifPresent(balance -> workspace.accountStore().postInterest(result.accountId(), balance));
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String runId = stepExecution.getJobParameters().getString(InterestJobParameters.RUN_ID);
        if (workspace == null) {
            log.error("run {} step {} stopped before any work", runId, stepExecution.getStepName());
            return ExitStatus.FAILED;
        }
        if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
            workspace.discard();
            log.error("run {} step {} stopped: accounts={} transactions={}; no output kept", runId,
                    stepExecution.getStepName(), stepExecution.getWriteCount(), workspace.transactionsWritten());
            return ExitStatus.FAILED;
        }
        try {
            workspace.runLog().line(RunLogLines.END);
            workspace.commit(InterestExitCode.COMPLETED);
        } catch (InterestRunStoppedException e) {
            stepExecution.setStatus(BatchStatus.FAILED);
            stepExecution.addFailureException(e);
            log.error("run {} step {} stopped: {}", runId, stepExecution.getStepName(), e.getMessage());
            return ExitStatus.FAILED;
        }
        log.info("run {} step {} completed: accounts={} transactions={}", runId, stepExecution.getStepName(),
                stepExecution.getWriteCount(), counter.issued());
        return ExitStatus.COMPLETED;
    }
}
