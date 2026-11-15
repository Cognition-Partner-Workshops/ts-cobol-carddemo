package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.JobExecutionEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.jdbc.InterestTables;

/**
 * Commit, rollback and restart of calcInterestStep on the tables (ticket 06; architecture.md
 * sections 5 and 6, REL-11 as proposed), on PostgreSQL 15 in a container (standards S7.3).
 * The commit interval is one account, so every account is its own chunk. Failures are forced in
 * the database by a trigger that raises an error on the statement named in the test.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.calc.adapters=jdbc",
        "interest.calc.commitInterval=1",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class CalcInterestTablesRestartIT {

    private static final String TRANSACTION_TABLE = "carddemo.interest_transaction";
    private static final String ACCOUNT_TABLE = "carddemo.account";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = InterestTables.postgres();

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job interestCalcJob;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private InterestExitCodes exitCodes;

    @TempDir
    Path temp;

    @BeforeEach
    void emptyTables() {
        InterestTables.clear(jdbc);
    }

    @AfterEach
    void removeTriggers() {
        InterestTables.stopFailingOn(jdbc, TRANSACTION_TABLE);
        InterestTables.stopFailingOn(jdbc, ACCOUNT_TABLE);
        InterestTables.removeWriteAudit(jdbc);
    }

    private JobParameters parameters(Path outputDir, String runId) {
        return new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, runId)
                .addString(InterestJobParameters.INPUT_DIR, "not-read-by-the-table-step")
                .addString(InterestJobParameters.OUTPUT_DIR, outputDir.toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters();
    }

    private static long interestRunOf(JobExecution execution) {
        return execution.getExecutionContext().getLong(CalcInterestTableRun.INTEREST_RUN_ID);
    }

    /** Status and counts of the step from the job repository, then the exit message of the job stored there. */
    private String stepExit(JobExecution execution) {
        return jdbc.queryForObject("""
                SELECT s.status || ' commits=' || s.commit_count || ' rollbacks=' || s.rollback_count || ' ' || e.exit_message
                  FROM batch.batch_step_execution s
                  JOIN batch.batch_job_execution e ON e.job_execution_id = s.job_execution_id
                 WHERE s.job_execution_id = ?""", String.class, execution.getId());
    }

    private String runStatus(long run) {
        return jdbc.queryForObject("SELECT status FROM carddemo.interest_run WHERE run_id = ?", String.class, run);
    }

    private static List<String> expectedLines(String caseName, String file) {
        return Fixtures.lines(Fixtures.expected(caseName).resolve(file));
    }

    private Path runLogWorkFile(String runId) {
        return temp.resolve("actual.calc-runlog-" + runId);
    }

    @Test
    void transaction_failureBetweenInsertAndUpdateOfAnAccountLeavesNeither() throws Exception {
        // account 2001 of i01-baseline: its transaction is inserted, then the UPDATE of its account fails
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));
        String accounts = InterestTables.rowHash(jdbc, ACCOUNT_TABLE);
        InterestTables.failOn(jdbc, "UPDATE", ACCOUNT_TABLE, "NEW.acct_id = 2001");
        String runId = UUID.randomUUID().toString();

        JobExecution execution = jobLauncher.run(interestCalcJob, parameters(temp.resolve("actual"), runId));

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        // neither the insert nor the update is there
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM " + TRANSACTION_TABLE, Integer.class));
        assertEquals(accounts, InterestTables.rowHash(jdbc, ACCOUNT_TABLE));
        // the step is FAILED and names the account in its exit message; nothing was committed but the start state
        String exit = stepExit(execution);
        assertEquals("FAILED commits=0 rollbacks=1 calcInterestStep stopped at account 00000002001:"
                + " WRITE_OUTPUT (INT-R08): cannot update table carddemo.account", exit);
        // the step's own exit description holds the same sentence after the stack trace
        assertTrue(execution.getStepExecutions().iterator().next().getExitStatus().getExitDescription()
                .endsWith("calcInterestStep stopped at account 00000002001: WRITE_OUTPUT (INT-R08): cannot update table carddemo.account"));
        assertEquals("FAILED", runStatus(interestRunOf(execution)));
        exitCodes.onJobExecution(new JobExecutionEvent(execution));
        assertEquals(InterestExitCode.STOPPED, exitCodes.getExitCode());
        // ticket 08: a stopped run keeps the lines of the chunk that did not commit and ends with ABENDING PROGRAM
        // (CBACT04C:193, 629); the echo is the balance record of account 2001 as the recorded run log holds it
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C",
                expectedLines("i01-baseline", InterestTables.RUN_LOG).get(1),
                "ABENDING PROGRAM"), Fixtures.lines(runLogWorkFile(runId)));
        assertFalse(Files.exists(temp.resolve("actual")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i07-nontie-rounding"})
    void restart_afterTheFirstCommittedChunkEndsAsTheRecordedUninterruptedRun(String caseName) throws Exception {
        // two accounts, one per chunk; the insert of the second transaction fails after the first chunk committed
        InterestTables.load(jdbc, Fixtures.input(caseName));
        InterestTables.installWriteAudit(jdbc);
        InterestTables.failOn(jdbc, "INSERT", TRANSACTION_TABLE, "NEW.seq_no = 2");
        Path output = temp.resolve("actual");
        String runId = UUID.randomUUID().toString();
        List<String> expectedTransactions = expectedLines(caseName, InterestTables.TRANSACTIONS);
        List<String> expectedAccounts = expectedLines(caseName, InterestTables.ACCOUNTS);
        List<String> expectedRunLog = expectedLines(caseName, InterestTables.RUN_LOG);
        String secondAccount = expectedAccounts.get(1).substring(0, 11);

        JobExecution failed = jobLauncher.run(interestCalcJob, parameters(output, runId));

        // errors: the step is FAILED and names the account; the committed chunk stays committed
        assertEquals(BatchStatus.FAILED, failed.getStatus());
        String exit = stepExit(failed);
        assertTrue(exit.startsWith("FAILED commits=1 rollbacks=1 calcInterestStep stopped at account " + secondAccount), exit);
        long run = interestRunOf(failed);
        assertEquals("FAILED", runStatus(run));
        assertEquals(expectedTransactions.subList(0, 1), InterestTables.transactionLines(jdbc, run));
        assertEquals(expectedAccounts.get(0), InterestTables.accountLines(jdbc).get(0));
        // the run log holds the committed part - start line and the record of the first account - and, since
        // ticket 08, the lines of the stop: the record of the second account and ABENDING PROGRAM (CBACT04C:629).
        // The restart below cuts them off again: the completed log is the recorded one.
        List<String> stoppedRunLog = new ArrayList<>(expectedRunLog.subList(0, 3));
        stoppedRunLog.add("ABENDING PROGRAM");
        assertEquals(stoppedRunLog, Fixtures.lines(runLogWorkFile(runId)));
        assertFalse(Files.exists(output));

        InterestTables.stopFailingOn(jdbc, TRANSACTION_TABLE);
        JobExecution restarted = jobLauncher.run(interestCalcJob, parameters(output, runId));

        // restart: same job instance, same run, and the rows of the recorded uninterrupted run - same transaction ids
        assertEquals(BatchStatus.COMPLETED, restarted.getStatus());
        assertEquals(failed.getJobInstance().getId(), restarted.getJobInstance().getId());
        assertNotEquals(failed.getId(), restarted.getId());
        assertEquals(run, interestRunOf(restarted));
        assertEquals(expectedTransactions, InterestTables.transactionLines(jdbc, run));
        assertEquals(expectedAccounts, InterestTables.accountLines(jdbc));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_run", Integer.class));
        assertEquals(expectedTransactions.size(),
                jdbc.queryForObject("SELECT count(*) FROM " + TRANSACTION_TABLE, Integer.class));
        // no account updated twice: the first account once, before the failure; the second ends the input (INT-Q04)
        assertEquals(List.of("account UPDATE " + expectedAccounts.get(0).substring(0, 11) + " false"),
                InterestTables.writeAudit(jdbc));
        // SYSOUT after the restart: no line missing, none written twice
        assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve(InterestTables.RUN_LOG)),
                Fixtures.text(output.resolve(InterestTables.RUN_LOG)));
        assertFalse(Files.exists(runLogWorkFile(runId)));
        // the restarted execution read one account and committed it; RC5 covers the whole run
        assertTrue(stepExit(restarted).startsWith("COMPLETED commits=2 rollbacks=0"), stepExit(restarted));
        assertEquals("CALCULATED " + expectedTransactions.size(), jdbc.queryForObject(
                "SELECT status || ' ' || written_transaction_count FROM carddemo.interest_run WHERE run_id = ?",
                String.class, run));
    }

    @Test
    void restart_fiveAccountsStoppedAtTheFourthEndAsAnUninterruptedRun() throws Exception {
        // i01 (2001, 2002), i05 (2007, two balances) and i07 (2009, 2010) in one input: five accounts, six transactions.
        // No recorded run exists for this input; the acceptance compares with an uninterrupted run of the same tables.
        Path[] inputs = {Fixtures.input("i01-baseline"), Fixtures.input("i05-multi-category"),
                Fixtures.input("i07-nontie-rounding")};
        InterestTables.load(jdbc, inputs);
        JobExecution uninterrupted = jobLauncher.run(interestCalcJob,
                parameters(temp.resolve("uninterrupted"), UUID.randomUUID().toString()));
        assertEquals(BatchStatus.COMPLETED, uninterrupted.getStatus());
        List<String> transactions = InterestTables.transactionLines(jdbc, interestRunOf(uninterrupted));
        List<String> accounts = InterestTables.accountLines(jdbc);
        String figures = figuresOf(interestRunOf(uninterrupted));
        // by hand: ids run across accounts (INT-Q02) and 2010 ends the input, so 2002 and 2007 are updated here
        assertEquals(List.of("2022071800000001", "2022071800000002", "2022071800000003", "2022071800000004",
                "2022071800000005", "2022071800000006"), transactions.stream().map(line -> line.substring(0, 16)).toList());

        InterestTables.load(jdbc, inputs);
        InterestTables.installWriteAudit(jdbc);
        // seq_no 5 is the transaction of account 2009, the fourth account: three chunks are committed before it
        InterestTables.failOn(jdbc, "INSERT", TRANSACTION_TABLE, "NEW.seq_no = 5");
        Path output = temp.resolve("actual");
        String runId = UUID.randomUUID().toString();

        JobExecution failed = jobLauncher.run(interestCalcJob, parameters(output, runId));

        assertEquals(BatchStatus.FAILED, failed.getStatus());
        String exit = stepExit(failed);
        assertTrue(exit.startsWith("FAILED commits=3 rollbacks=1 calcInterestStep stopped at account 00000002009"), exit);
        long run = interestRunOf(failed);
        assertEquals(transactions.subList(0, 4), InterestTables.transactionLines(jdbc, run));
        assertEquals(accounts.subList(0, 3), InterestTables.accountLines(jdbc).subList(0, 3));

        InterestTables.stopFailingOn(jdbc, TRANSACTION_TABLE);
        JobExecution restarted = jobLauncher.run(interestCalcJob, parameters(output, runId));

        assertEquals(BatchStatus.COMPLETED, restarted.getStatus());
        assertEquals(transactions, InterestTables.transactionLines(jdbc, run));
        assertEquals(accounts, InterestTables.accountLines(jdbc));
        assertEquals(figures, figuresOf(run));
        assertEquals(Fixtures.text(temp.resolve("uninterrupted").resolve(InterestTables.RUN_LOG)),
                Fixtures.text(output.resolve(InterestTables.RUN_LOG)));
        // each of the four accounts before the last is updated exactly once; 2010 ends the input (INT-Q04)
        assertEquals(List.of("account UPDATE 00000002001 false", "account UPDATE 00000002002 false",
                "account UPDATE 00000002007 false", "account UPDATE 00000002009 false"), InterestTables.writeAudit(jdbc));
    }

    private String figuresOf(long run) {
        return jdbc.queryForObject("""
                SELECT status || ' ' || written_transaction_count || ' ' || written_tran_amt_sum || ' '
                       || written_interest_not_added_sum
                  FROM carddemo.interest_run WHERE run_id = ?""", String.class, run);
    }

    @Test
    void restart_aStoppedRunIsNotResumedByANewRunId() throws Exception {
        // INT-H02, rerunGuard OFF: another runId is another run from the first account, with its own rows
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));
        InterestTables.failOn(jdbc, "INSERT", TRANSACTION_TABLE, "NEW.seq_no = 2");
        JobExecution failed = jobLauncher.run(interestCalcJob, parameters(temp.resolve("actual"), UUID.randomUUID().toString()));
        assertEquals(BatchStatus.FAILED, failed.getStatus());
        InterestTables.stopFailingOn(jdbc, TRANSACTION_TABLE);

        JobExecution other = jobLauncher.run(interestCalcJob, parameters(temp.resolve("other"), UUID.randomUUID().toString()));

        assertEquals(BatchStatus.COMPLETED, other.getStatus());
        assertNotEquals(interestRunOf(failed), interestRunOf(other));
        assertEquals(List.of("2022071800000001", "2022071800000002"), jdbc.queryForList(
                "SELECT tran_id FROM " + TRANSACTION_TABLE + " WHERE run_id = ? ORDER BY seq_no", String.class,
                interestRunOf(other)));
        assertEquals("FAILED", runStatus(interestRunOf(failed)));
    }
}
