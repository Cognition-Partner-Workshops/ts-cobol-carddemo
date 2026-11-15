package com.carddemo.interest.batch;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;

import javax.sql.DataSource;

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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.domain.Account;
import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.AccountInterest;
import com.carddemo.interest.domain.InterestCalculator;
import com.carddemo.interest.domain.LegacyFile;
import com.carddemo.interest.domain.RunLogLines;
import com.carddemo.interest.domain.RunStop;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.domain.TransactionCounter;
import com.carddemo.interest.jdbc.InterestRunTable;
import com.carddemo.interest.jdbc.JdbcAccessFailure;
import com.carddemo.interest.jdbc.JdbcAccountStore;
import com.carddemo.interest.jdbc.JdbcBalanceSource;
import com.carddemo.interest.jdbc.JdbcCardLookup;
import com.carddemo.interest.jdbc.JdbcRateLookup;
import com.carddemo.interest.jdbc.JdbcTransactionSink;
import com.carddemo.interest.policy.InterestPolicy;
import com.carddemo.interest.port.AccountStore;
import com.carddemo.interest.port.CardLookup;
import com.carddemo.interest.port.RateLookup;

/**
 * The state of one execution of calcInterestStep on PostgreSQL (ticket 06): the JDBC adapters of
 * the ports, the calculation and the restart state. Reader, processor and writer of the step
 * delegate here; a chunk is whole accounts, and the inserts of an account's transactions and the
 * update of that account are in the chunk's transaction (architecture.md section 5).
 *
 * <p>Restart state, saved with every commit (architecture.md section 6, REL-11 as proposed): the
 * last account written, the transaction counter, seq_no, the accounts updated and the length of
 * the run log. A restarted execution reads on after that account with that counter and cuts the
 * run log back to that length. The row of the run in carddemo.interest_run is kept in the job's
 * execution context, so a restart writes under the same run.
 */
public class JdbcCalcInterestRun implements StepExecutionListener, ItemStream {

    /** Key in the job execution context: run_id of the run's row in carddemo.interest_run. */
    public static final String INTEREST_RUN_ID = "interest.runId";

    static final String LAST_ACCOUNT = "calc.lastAccountId";
    static final String COUNTER = "calc.transactionCounter";
    static final String SEQ_NO = "calc.seqNo";
    static final String ACCOUNTS_UPDATED = "calc.accountsUpdated";
    static final String RUN_LOG_LENGTH = "calc.runLogLength";
    /** Account ids are 9(11), never negative (CVACT01Y/ACCT-ID): every account comes after this one. */
    private static final long BEFORE_THE_FIRST_ACCOUNT = -1L;

    private static final Logger log = LoggerFactory.getLogger(JdbcCalcInterestRun.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final InterestRunTable runs;
    private final InterestPolicy policy;
    private final Clock clock;

    private Path outputDir;
    private String parmDate;
    private long interestRunId;
    private JdbcBalanceSource balances;
    private JdbcAccountStore accounts;
    private AccountStore accountUpdates;
    private JdbcTransactionSink transactions;
    private RunLogFile runLog;
    private TransactionCounter counter;
    private InterestCalculator calculator;
    private long lastAccountId;
    /** The account being calculated or written; named in the exit description of a failed step. */
    private Long accountInWork;

    public JdbcCalcInterestRun(DataSource dataSource, InterestPolicy policy, Clock clock) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
        this.runs = new InterestRunTable(jdbc);
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        JobParameters parameters = stepExecution.getJobParameters();
        outputDir = Path.of(parameters.getString(InterestJobParameters.OUTPUT_DIR));
        parmDate = parameters.getString(InterestJobParameters.PARM_DATE);
        accountInWork = null;
        ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
        if (!jobContext.containsKey(INTEREST_RUN_ID)) {
            jobContext.putLong(INTEREST_RUN_ID, runs.create(parmDate));
        }
        interestRunId = jobContext.getLong(INTEREST_RUN_ID);
    }

    @Override
    public void open(ExecutionContext restartState) {
        boolean restart = restartState.containsKey(RUN_LOG_LENGTH);
        lastAccountId = restartState.getLong(LAST_ACCOUNT, BEFORE_THE_FIRST_ACCOUNT);
        counter = new TransactionCounter(restartState.getInt(COUNTER, 0));
        accounts = new JdbcAccountStore(jdbc, restartState.getInt(ACCOUNTS_UPDATED, 0));
        accountUpdates = stoppingOnFailure(accounts);
        transactions = new JdbcTransactionSink(jdbc, interestRunId, restartState.getInt(SEQ_NO, 0));
        if (restart) {
            runLog = RunLogFile.cutBackTo(outputDir, restartState.getLong(RUN_LOG_LENGTH));
        } else {
            runLog = RunLogFile.create(outputDir);
            runLog.line(RunLogLines.START);
        }
        try {
            balances = JdbcBalanceSource.openAfter(dataSource, lastAccountId);
        } catch (JdbcAccessFailure e) {
            throw RunStop.openFailed(runLog, LegacyFile.BALANCES, "the balances cannot be reached", e);
        }
        calculator = new InterestCalculator(accountUpdates, stoppingOnFailure(new JdbcCardLookup(jdbc)),
                stoppingOnFailure(new JdbcRateLookup(jdbc)), runLog, policy, clock, counter, parmDate);
    }

    /**
     * INT-R08 (standards S6.1): a database failure of a lookup stops the run with the legacy message
     * of the read it replaces; its own cause goes to the application log with the exception.
     */
    private AccountStore stoppingOnFailure(AccountStore store) {
        return new AccountStore() {
            @Override
            public Optional<Account> find(long accountId) {
                try {
                    return store.find(accountId);
                } catch (DataAccessException e) {
                    throw RunStop.of(runLog, StopReason.READ_ACCOUNT, "the accounts cannot be read", e);
                }
            }

            @Override
            public void postInterest(long accountId, BigDecimal newBalance) {
                try {
                    store.postInterest(accountId, newBalance);
                } catch (DataAccessException e) {
                    throw RunStop.of(runLog, StopReason.UPDATE_ACCOUNT, "an account cannot be updated", e);
                }
            }
        };
    }

    private CardLookup stoppingOnFailure(CardLookup cards) {
        return accountId -> {
            try {
                return cards.cardNumbersOf(accountId);
            } catch (DataAccessException e) {
                throw RunStop.of(runLog, StopReason.READ_CARD, "the card cross-reference cannot be read", e);
            }
        };
    }

    private RateLookup stoppingOnFailure(RateLookup rates) {
        return (groupId, typeCode, categoryCode) -> {
            try {
                return rates.findRate(groupId, typeCode, categoryCode);
            } catch (DataAccessException e) {
                throw RunStop.of(runLog, StopReason.READ_RATE, "the rates cannot be read", e);
            }
        };
    }

    /** Reader: one item is one account with all its balances; null ends the input. */
    public AccountBalances read() {
        try {
            return balances.nextAccount().orElse(null);
        } catch (JdbcAccessFailure e) {
            throw RunStop.of(runLog, StopReason.READ_BALANCES, "the balances cannot be read", e);
        }
    }

    public AccountInterest process(AccountBalances item) {
        accountInWork = item.accountId();
        return calculator.calculate(item);
    }

    /** Writer: the transactions of each account, then the update of its account break (INT-R07). */
    public void write(Chunk<? extends AccountInterest> chunk) {
        for (AccountInterest result : chunk) {
            accountInWork = result.accountId();
            try {
                result.transactions().forEach(transactions::write);
            } catch (DataAccessException e) {
                throw RunStop.of(runLog, StopReason.WRITE_TRANSACTION, "a transaction cannot be written", e);
            }
            result.newBalance().ifPresent(balance -> accountUpdates.postInterest(result.accountId(), balance));
            lastAccountId = result.accountId();
        }
        accountInWork = null;
    }

    /** Called inside the chunk's transaction, after the write: the restart state commits with the chunk. */
    @Override
    public void update(ExecutionContext restartState) {
        restartState.putLong(LAST_ACCOUNT, lastAccountId);
        restartState.putInt(COUNTER, counter.issued());
        restartState.putInt(SEQ_NO, transactions.lastSeqNo());
        restartState.putInt(ACCOUNTS_UPDATED, accounts.accountsUpdated());
        restartState.putLong(RUN_LOG_LENGTH, runLog.committedLength());
    }

    @Override
    public void close() {
        if (balances != null) {
            balances.close();
            balances = null;
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String runId = stepExecution.getJobParameters().getString(InterestJobParameters.RUN_ID);
        try {
            if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
                runs.failed(interestRunId);
                // the application log never holds an account id (standards S6.5); the exit description
                // of the step in the job repository does (ticket 06, Errors)
                log.error("run {} step {} stopped: accounts committed={} transactions committed={}", runId,
                        stepExecution.getStepName(), stepExecution.getWriteCount(),
                        stepExecution.getExecutionContext().getInt(SEQ_NO, 0));
                return accountInWork == null ? ExitStatus.FAILED
                        : ExitStatus.FAILED.addExitDescription(
                                String.format("calcInterestStep failed at account %011d", accountInWork));
            }
            runLog.line(RunLogLines.END);
            runLog.committedLength();
            runs.calculated(interestRunId, accounts.accountsUpdated());
            log.info("run {} step {} completed: accounts={} transactions={}", runId, stepExecution.getStepName(),
                    stepExecution.getWriteCount(), transactions.lastSeqNo());
            return ExitStatus.COMPLETED;
        } finally {
            if (runLog != null) {
                runLog.close();
                runLog = null;
            }
        }
    }
}
