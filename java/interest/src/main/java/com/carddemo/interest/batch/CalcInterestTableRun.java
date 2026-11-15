package com.carddemo.interest.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.jdbc.core.JdbcOperations;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.AccountInterest;
import com.carddemo.interest.domain.InterestCalculator;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.InterestTransaction;
import com.carddemo.interest.domain.RunLogLines;
import com.carddemo.interest.domain.TransactionCounter;
import com.carddemo.interest.jdbc.JdbcAccountStore;
import com.carddemo.interest.jdbc.JdbcBalanceSource;
import com.carddemo.interest.jdbc.JdbcCardLookup;
import com.carddemo.interest.jdbc.JdbcInterestRuns;
import com.carddemo.interest.jdbc.JdbcRateLookup;
import com.carddemo.interest.jdbc.JdbcTransactionSink;
import com.carddemo.interest.policy.InterestPolicy;
import com.carddemo.interest.port.AccountStore;
import com.carddemo.interest.port.BalanceSource;

/**
 * The state of one execution of calcInterestStep on the tables of schema carddemo (ticket 06,
 * architecture.md sections 2, 5 and 6). Reader, processor and writer of the step delegate here;
 * the calculation is the one of the file adapters and sees the ports of FI-3 only.
 *
 * <p>Transaction (section 5): the step's chunk is whole accounts; the inserts of an account's
 * transactions and the update of its three columns are written in the chunk's transaction, and the
 * restart state is saved in the same transaction ({@link #update}).
 *
 * <p>Restart (section 6, REL-11 as proposed): the state is the last committed account, the
 * transaction counter (INT-Q02), the last seq_no, the length of the run log, the interest not
 * added so far (INT-Q04) and the run's row in interest_run. A restarted execution reads the
 * accounts after the last committed one. A new runId is a new run and is never refused
 * (INT-H02, rerunGuard OFF).
 */
public class CalcInterestTableRun implements ItemStream, StepExecutionListener, JobExecutionListener {

    /** Job execution context: interest_run.run_id of this run, for the steps before and after this one. */
    public static final String INTEREST_RUN_ID = "interestRunId";

    static final String STATE_LAST_ACCOUNT = "calc.lastCommittedAccountId";
    static final String STATE_COUNTER = "calc.transactionCounter";
    static final String STATE_SEQ_NO = "calc.lastSeqNo";
    static final String STATE_RUN_LOG_LENGTH = "calc.runLogLength";
    static final String STATE_NOT_ADDED = "calc.interestNotAdded";
    static final String STATE_RUN = "calc.interestRunId";

    private static final Logger log = LoggerFactory.getLogger(CalcInterestTableRun.class);

    private final JdbcOperations jdbc;
    private final InterestPolicy policy;
    private final Clock clock;
    /** The one place the table adapters choose the code page of the record echo (S3.7); the job accepts format=fixture only. */
    private final CodePage codePage = CodePage.fixture();

    private StepExecution stepExecution;
    private JdbcInterestRuns runs;
    private Long interestRunId;
    private RunLogFile runLog;
    private BalanceSource balances;
    private AccountStore accounts;
    private JdbcTransactionSink transactions;
    private InterestCalculator calculator;
    private TransactionCounter counter;
    private Long lastCommittedAccountId;
    private BigDecimal interestNotAdded;
    /** Where the step is, for the exit description of a failed step (never for the application log, S6.5). */
    private String position;
    private String stopDescription;

    public CalcInterestTableRun(JdbcOperations jdbc, InterestPolicy policy, Clock clock) {
        this.jdbc = jdbc;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        this.interestRunId = null;
        this.runLog = null;
        this.position = "before the first account";
        this.stopDescription = null;
    }

    /** Opens the run: new, or after the last commit of the execution that is being restarted. */
    @Override
    public void open(ExecutionContext state) {
        JobParameters parameters = stepExecution.getJobParameters();
        ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
        boolean restart = state.containsKey(STATE_RUN_LOG_LENGTH);

        runs = new JdbcInterestRuns(jdbc);
        runLog = new RunLogFile(Path.of(parameters.getString(InterestJobParameters.OUTPUT_DIR)),
                parameters.getString(InterestJobParameters.RUN_ID), codePage.charset());
        counter = new TransactionCounter();
        int lastSeqNo = 0;
        if (restart) {
            interestRunId = state.getLong(STATE_RUN);
            lastCommittedAccountId = state.containsKey(STATE_LAST_ACCOUNT) ? state.getLong(STATE_LAST_ACCOUNT) : null;
            lastSeqNo = state.getInt(STATE_SEQ_NO);
            interestNotAdded = new BigDecimal(state.getString(STATE_NOT_ADDED));
            issueAgain(counter, state.getInt(STATE_COUNTER));
            runs.calculating(interestRunId);
            runLog.resumeAt(state.getLong(STATE_RUN_LOG_LENGTH));
        } else {
            lastCommittedAccountId = null;
            interestNotAdded = BigDecimal.ZERO.setScale(2);
            if (jobContext.containsKey(INTEREST_RUN_ID)) {
                interestRunId = jobContext.getLong(INTEREST_RUN_ID);
                runs.calculating(interestRunId);
            } else {
                interestRunId = runs.start(parameters.getString(InterestJobParameters.PARM_DATE));
            }
            runLog.startEmpty();
            runLog.line(RunLogLines.START);
        }
        jobContext.putLong(INTEREST_RUN_ID, interestRunId);

        balances = new JdbcBalanceSource(jdbc, codePage, lastCommittedAccountId, JdbcBalanceSource.DEFAULT_PAGE_ROWS);
        accounts = new JdbcAccountStore(jdbc);
        transactions = new JdbcTransactionSink(jdbc, codePage, interestRunId, lastSeqNo);
        calculator = new InterestCalculator(accounts, new JdbcCardLookup(jdbc), new JdbcRateLookup(jdbc), runLog,
                policy, clock, counter, parameters.getString(InterestJobParameters.PARM_DATE));
        log.info("run {} step {} {}: interest_run {} transactions so far={}",
                parameters.getString(InterestJobParameters.RUN_ID), stepExecution.getStepName(),
                restart ? "restarted after its last commit" : "started", interestRunId, lastSeqNo);
    }

    /**
     * INT-Q02: the counter goes on where the committed part of the run left it. The domain class
     * has no way to start at a value, so the issued numbers are issued again.
     */
    private static void issueAgain(TransactionCounter counter, int issued) {
        while (counter.issued() < issued) {
            counter.next();
        }
    }

    /** Reader: one item is one account with all its balances; null ends the input. */
    public AccountBalances read() {
        return balances.nextAccount().orElse(null);
    }

    public AccountInterest process(AccountBalances item) {
        position = "at account " + elevenDigits(item.accountId());
        return calculator.calculate(item);
    }

    /** Writer: the transactions of each account, then the update of its account break (INT-R07). */
    public void write(Chunk<? extends AccountInterest> chunk) {
        for (AccountInterest result : chunk) {
            position = "at account " + elevenDigits(result.accountId());
            result.transactions().forEach(transactions::write);
            if (result.newBalance().isPresent()) {
                accounts.postInterest(result.accountId(), result.newBalance().get());
            } else {
                // RC5: interest written for an account that is not updated - the one that ends the input (INT-Q04)
                for (InterestTransaction transaction : result.transactions()) {
                    interestNotAdded = interestNotAdded.add(transaction.amount()).setScale(2, RoundingMode.UNNECESSARY);
                }
            }
            lastCommittedAccountId = result.accountId();
        }
    }

    /** Called in the transaction of each chunk, after its writes: the restart state commits with them. */
    @Override
    public void update(ExecutionContext state) {
        state.putLong(STATE_RUN, interestRunId);
        if (lastCommittedAccountId != null) {
            state.putLong(STATE_LAST_ACCOUNT, lastCommittedAccountId);
        }
        state.putInt(STATE_COUNTER, counter.issued());
        state.putInt(STATE_SEQ_NO, transactions.lastSeqNo());
        state.putString(STATE_NOT_ADDED, interestNotAdded.toPlainString());
        state.putLong(STATE_RUN_LOG_LENGTH, runLog.commitLength());
    }

    @Override
    public void close() {
        if (runLog != null) {
            runLog.close();
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String runId = stepExecution.getJobParameters().getString(InterestJobParameters.RUN_ID);
        if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
            return stopped(stepExecution, runId, null);
        }
        try {
            runLog.line(RunLogLines.END);
            runLog.complete();
            runs.calculated(interestRunId, interestNotAdded);
        } catch (InterestRunStoppedException e) {
            stepExecution.setStatus(BatchStatus.FAILED);
            stepExecution.addFailureException(e);
            return stopped(stepExecution, runId, e);
        }
        log.info("run {} step {} completed: interest_run {} accounts={} transactions={}", runId,
                stepExecution.getStepName(), interestRunId, stepExecution.getWriteCount(), transactions.lastSeqNo());
        return ExitStatus.COMPLETED;
    }

    /**
     * A stopped step: FAILED, committed chunks stay committed, the run is marked FAILED in
     * interest_run. The account the step stopped at is named in the exit description of the step
     * and of the job (job repository), not in the application log (standards S6.5).
     */
    private ExitStatus stopped(StepExecution stepExecution, String runId, InterestRunStoppedException atEnd) {
        if (interestRunId != null) {
            try {
                runs.failed(interestRunId);
            } catch (InterestRunStoppedException e) {
                log.error("run {} step {}: interest_run {} could not be marked FAILED: {}", runId,
                        stepExecution.getStepName(), interestRunId, e.getMessage());
            }
        }
        log.error("run {} step {} stopped: accounts committed={}; committed chunks are kept{}", runId,
                stepExecution.getStepName(), stepExecution.getWriteCount(), atEnd == null ? "" : ": " + atEnd.getMessage());
        String cause = atEnd != null ? atEnd.getMessage() : stepExecution.getFailureExceptions().stream()
                .map(CalcInterestTableRun::stopMessage).findFirst().orElse("no failure recorded");
        stopDescription = stepExecution.getStepName() + " stopped "
                + (atEnd != null ? "after the last account" : position) + ": " + cause;
        return new ExitStatus(ExitStatus.FAILED.getExitCode(), stopDescription);
    }

    /** The message of the one stop exception; of anything else only its type, which holds no data. */
    private static String stopMessage(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof InterestRunStoppedException stopped) {
                return stopped.getMessage();
            }
        }
        return failure.getClass().getName();
    }

    /**
     * The exit message of a job whose calculation stopped is the one sentence that names the account,
     * in place of the stack trace Spring Batch stores there. The step's own exit message keeps the
     * stack trace and is cut by the job repository before the sentence appended to it.
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        if (stopDescription != null && jobExecution.getStatus() != BatchStatus.COMPLETED) {
            jobExecution.setExitStatus(new ExitStatus(ExitStatus.FAILED.getExitCode(), stopDescription));
        }
    }

    /** The eleven digits of an account id, as in the description of a transaction (standards S3.6). */
    private static String elevenDigits(long accountId) {
        return String.format("%011d", accountId);
    }
}
