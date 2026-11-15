package com.carddemo.interest.batch;

import java.nio.file.Path;
import java.time.Clock;
import java.util.OptionalLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.AccountInterest;
import com.carddemo.interest.domain.HoleDecisions;
import com.carddemo.interest.domain.InterestCalculator;
import com.carddemo.interest.domain.RunLogLines;
import com.carddemo.interest.domain.TransactionCounter;
import com.carddemo.interest.jdbc.calc.CalcRunControl;
import com.carddemo.interest.jdbc.calc.JdbcAccountStore;
import com.carddemo.interest.jdbc.calc.JdbcBalanceSource;
import com.carddemo.interest.jdbc.calc.JdbcCardLookup;
import com.carddemo.interest.jdbc.calc.JdbcRateLookup;
import com.carddemo.interest.jdbc.calc.JdbcTransactionSink;
import com.carddemo.interest.policy.InterestPolicy;
import com.carddemo.interest.port.AccountStore;
import com.carddemo.interest.port.BalanceSource;
import com.carddemo.interest.port.TransactionSink;

/**
 * One execution of calcInterestStep on the tables of schema carddemo (architecture.md sections 5
 * and 6). Reader, processor and writer of the step delegate here. A chunk is whole accounts: the
 * INSERTs of an account's transactions, the UPDATE of its account and the restart state commit
 * together. A restarted execution continues after the last committed account with the saved
 * counter, seq_no and run log length (REL-11 as proposed); a launch with a new runId is a new run
 * and is not refused (INT-H02, rerunGuard OFF). A stop of the calculation or a database failure
 * fails the step (INT-R08): the chunk in work is rolled back, the chunks committed before it stay,
 * the run log keeps the legacy lines of the stop and interest_run.status becomes FAILED.
 */
public class CalcInterestTableRun implements StepExecutionListener, ItemStream {

    /** The step writes the legacy SYSOUT here while it runs (FI-2, INT-T02). */
    static final String RUN_LOG_FILE = "sysout/INTCALC.STEP15.txt";
    /** Job execution context: the interest_run row of the run, put there by a step that runs before this one. */
    public static final String INTEREST_RUN_ID = "interestRunId";

    private static final Logger log = LoggerFactory.getLogger(CalcInterestTableRun.class);

    private final JdbcTemplate jdbc;
    private final InterestPolicy policy;
    private final HoleDecisions holes;
    private final Clock clock;
    private final int balancePageRows;
    private final CalcRunControl runControl;

    private StepExecution stepExecution;
    private long interestRunId;
    private BalanceSource balances;
    private AccountStore accounts;
    private TransactionSink transactions;
    /** Kept for the seq_no it saves with the commit. */
    private JdbcTransactionSink transactionRows;
    private RestartableRunLog runLog;
    private InterestCalculator calculator;
    private TransactionCounter counter;
    private OptionalLong lastAccountId = OptionalLong.empty();
    private long accountsUpdated;
    /** The account being calculated or written; named in the exit description when the step stops. */
    private OptionalLong accountInWork = OptionalLong.empty();

    public CalcInterestTableRun(JdbcTemplate jdbc, InterestPolicy policy, HoleDecisions holes, Clock clock,
            int balancePageRows) {
        this.jdbc = jdbc;
        this.policy = policy;
        this.holes = holes;
        this.clock = clock;
        this.balancePageRows = balancePageRows;
        this.runControl = new CalcRunControl(jdbc);
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        this.runLog = null;
        this.accountInWork = OptionalLong.empty();
    }

    /** Starts the run, or continues it from the state the last commit saved. */
    @Override
    public void open(ExecutionContext context) {
        JobParameters parameters = stepExecution.getJobParameters();
        Path runLogFile = Path.of(parameters.getString(InterestJobParameters.OUTPUT_DIR)).resolve(RUN_LOG_FILE);
        CalcRestartState state;
        if (CalcRestartState.savedIn(context)) {
            state = CalcRestartState.from(context);
            runLog = RestartableRunLog.resume(runLogFile, state.runLogLength());
        } else {
            long runId = interestRunOf(stepExecution, parameters);
            runLog = RestartableRunLog.create(runLogFile);
            runLog.line(RunLogLines.START);
            state = CalcRestartState.beforeTheFirstAccount(runId, runLog.committedLength());
            state.saveIn(context);
        }
        interestRunId = state.interestRunId();
        lastAccountId = state.lastAccountId();
        accountsUpdated = state.accountsUpdated();
        // INT-Q02: the counter continues after the last id a committed chunk issued.
        counter = new TransactionCounter(state.transactionCounter());
        DatabaseFailureStops stopping = new DatabaseFailureStops(runLog);
        balances = stopping.balances(new JdbcBalanceSource(jdbc, balancePageRows, state.lastAccountId()));
        accounts = stopping.accounts(new JdbcAccountStore(jdbc));
        transactionRows = new JdbcTransactionSink(jdbc, interestRunId, state.lastSeqNo());
        transactions = stopping.transactions(transactionRows);
        calculator = new InterestCalculator(accounts, stopping.cards(new JdbcCardLookup(jdbc)),
                stopping.rates(new JdbcRateLookup(jdbc)), runLog, policy, holes, clock, counter,
                parameters.getString(InterestJobParameters.PARM_DATE));
    }

    private long interestRunOf(StepExecution stepExecution, JobParameters parameters) {
        ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
        if (jobContext.containsKey(INTEREST_RUN_ID)) {
            return jobContext.getLong(INTEREST_RUN_ID);
        }
        return runControl.open(parameters.getString(InterestJobParameters.PARM_DATE));
    }

    /** Reader: one item is one account with all its balances; null ends the input. */
    public AccountBalances read() {
        accountInWork = OptionalLong.empty();
        return balances.nextAccount().orElse(null);
    }

    public AccountInterest process(AccountBalances item) {
        accountInWork = OptionalLong.of(item.accountId());
        return calculator.calculate(item);
    }

    /** Writer: the transactions of each account, then the update of its account break (INT-R07). */
    public void write(Chunk<? extends AccountInterest> chunk) {
        for (AccountInterest result : chunk) {
            accountInWork = OptionalLong.of(result.accountId());
            result.transactions().forEach(transactions::write);
            if (result.newBalance().isPresent()) {
                accounts.postInterest(result.accountId(), result.newBalance().get());
                accountsUpdated++;
            }
            lastAccountId = OptionalLong.of(result.accountId());
        }
        accountInWork = OptionalLong.empty();
    }

    /** Called inside the transaction of the chunk, after its writes: the restart state commits with them. */
    @Override
    public void update(ExecutionContext context) {
        new CalcRestartState(interestRunId, lastAccountId, counter.issued(), transactionRows.lastSeqNo(), accountsUpdated,
                runLog.committedLength()).saveIn(context);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String runId = stepExecution.getJobParameters().getString(InterestJobParameters.RUN_ID);
        if (runLog == null) {
            log.error("run {} step {} stopped before any work", runId, stepExecution.getStepName());
            return ExitStatus.FAILED;
        }
        if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
            runControl.failed(interestRunId);
            log.error("run {} step {} stopped: accounts committed={}; committed chunks stay, the step can be restarted",
                    runId, stepExecution.getStepName(), stepExecution.getWriteCount());
            // The account id goes to the job repository only, never to the application log (standards S6.5).
            return ExitStatus.FAILED.addExitDescription(accountInWork.isPresent()
                    ? "calcInterestStep stopped at account " + String.format("%011d", accountInWork.getAsLong())
                    : "calcInterestStep stopped between accounts");
        }
        runLog.line(RunLogLines.END);
        runLog.committedLength();
        runControl.calculated(interestRunId, accountsUpdated);
        log.info("run {} step {} completed: accounts={} transactions={}", runId, stepExecution.getStepName(),
                stepExecution.getWriteCount(), counter.issued());
        return ExitStatus.COMPLETED;
    }

    @Override
    public void close() {
        if (runLog != null) {
            runLog.close();
        }
    }
}
