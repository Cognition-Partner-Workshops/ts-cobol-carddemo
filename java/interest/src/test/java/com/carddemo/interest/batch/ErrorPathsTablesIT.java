package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
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
 * The stop conditions of ticket 08 (INT-R08, INT-H03, INT-H05) through the job on the tables, on
 * PostgreSQL 15 in a container (standards S7.3), with every switch at its default.
 *
 * <p>Data: the inputs of i01-baseline and i07-nontie-rounding in one set of tables - accounts
 * 2001, 2002, 2009 and 2010 in key order, one balance each. The commit interval is one account, so
 * 2001 and 2002 are committed chunks when the run stops at 2009, the account each test breaks.
 * No recorded legacy run exists for any of these inputs (INT-R08: "No fixture drives an error
 * branch"); the expected rows of 2001 and 2002 are worked by hand below, the run-log lines are the
 * literals of CBACT04C.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.calc.adapters=jdbc",
        "interest.calc.commitInterval=1",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class ErrorPathsTablesIT {

    private static final String TRANSACTION_TABLE = "carddemo.interest_transaction";

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
    @Autowired
    private JobRepository jobRepository;

    @TempDir
    Path temp;

    private String runId;
    private String untouchedAccounts;

    @BeforeEach
    void fourAccounts() {
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"), Fixtures.input("i07-nontie-rounding"));
        runId = UUID.randomUUID().toString();
    }

    @AfterEach
    void removeTriggers() {
        InterestTables.stopFailingOn(jdbc, TRANSACTION_TABLE);
        InterestTables.removeWriteAudit(jdbc);
    }

    private JobParameters parameters() {
        return new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, runId)
                .addString(InterestJobParameters.INPUT_DIR, "not-read-by-the-table-step")
                .addString(InterestJobParameters.OUTPUT_DIR, temp.resolve("actual").toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters();
    }

    /** Runs the job on the tables as the test left them; the write audit starts with the run. */
    private JobExecution launch() throws Exception {
        untouchedAccounts = accountRows("acct_id >= 2009");
        InterestTables.installWriteAudit(jdbc);
        return jobLauncher.run(interestCalcJob, parameters());
    }

    private String accountRows(String where) {
        return String.join("\n", jdbc.queryForList(
                "SELECT to_jsonb(t)::text FROM carddemo.account t WHERE " + where + " ORDER BY acct_id", String.class));
    }

    private String stepExit(JobExecution execution) {
        return jdbc.queryForObject("""
                SELECT s.status || ' commits=' || s.commit_count || ' rollbacks=' || s.rollback_count || ' ' || e.exit_message
                  FROM batch.batch_step_execution s
                  JOIN batch.batch_job_execution e ON e.job_execution_id = s.job_execution_id
                 WHERE s.job_execution_id = ?""", String.class, execution.getId());
    }

    private List<String> runLog() {
        return Fixtures.lines(RunLogFile.workFileOf(temp.resolve("actual"), runId));
    }

    /** The balance record CBACT04C:193 displays, as the recorded run log of the case holds it after its start line. */
    private static String balanceRecord(String caseName, int index) {
        return Fixtures.lines(Fixtures.expected(caseName).resolve(InterestTables.RUN_LOG)).get(1 + index);
    }

    /**
     * What every stop at account 2009 must leave (ticket 08, acceptance "Database assertions"; architecture.md
     * sections 5 and 6): the step FAILED, a non-zero exit code, the two chunks before it committed, nothing of 2009.
     */
    private void assertStoppedAt2009KeepingTheCommittedAccounts(JobExecution execution, String stop) {
        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertEquals(BatchStatus.FAILED, execution.getStepExecutions().iterator().next().getStatus());
        assertEquals("FAILED commits=2 rollbacks=1 calcInterestStep stopped at account 00000002009: " + stop,
                stepExit(execution));
        exitCodes.onJobExecution(new JobExecutionEvent(execution));
        assertNotEquals(0, exitCodes.getExitCode());
        assertEquals(InterestExitCode.STOPPED, exitCodes.getExitCode());

        long run = execution.getExecutionContext().getLong(CalcInterestTableRun.INTEREST_RUN_ID);
        assertEquals("FAILED", jdbc.queryForObject(
                "SELECT status FROM carddemo.interest_run WHERE run_id = ?", String.class, run));
        // by hand, accounts 2001 (GOLD, 12.00 %) and 2002 (STANDARD, 18.50 %) of i01-baseline:
        // 1000.00 * 12.00 / 1200 = 10.00 and 2400.00 * 18.50 / 1200 = 37.00; ids 2022071800 + 000001, 000002
        assertEquals(List.of("2022071800000001 10.00 4000000000002001", "2022071800000002 37.00 4000000000002002"),
                jdbc.queryForList("SELECT tran_id || ' ' || tran_amt || ' ' || tran_card_num FROM " + TRANSACTION_TABLE
                        + " ORDER BY run_id, seq_no", String.class));
        // the committed accounts keep their update: 1000.00 + 10.00 and 2400.00 + 37.00, cycle amounts 0 (INT-R07)
        assertEquals(List.of("2001 1010.00 0.00 0.00", "2002 2437.00 0.00 0.00"), jdbc.queryForList("""
                SELECT acct_id || ' ' || acct_curr_bal || ' ' || acct_curr_cyc_credit || ' ' || acct_curr_cyc_debit
                  FROM carddemo.account WHERE acct_id < 2009 ORDER BY acct_id""", String.class));
        // the failing account and the one after it: no transaction (above) and no update
        assertEquals(untouchedAccounts, accountRows("acct_id >= 2009"));
        assertEquals(List.of("account UPDATE 00000002001 false", "account UPDATE 00000002002 false"),
                InterestTables.writeAudit(jdbc));
        // a failed run leaves no output directory, only the run log of the stop
        assertFalse(Files.exists(temp.resolve("actual")));
    }

    @Test
    void intR08_accountMissingStops() throws Exception {
        jdbc.update("DELETE FROM carddemo.account WHERE acct_id = 2009");

        JobExecution execution = launch();

        assertStoppedAt2009KeepingTheCommittedAccounts(execution,
                "ACCOUNT_MISSING (INT-R08): a balance names an account that is not on the account file");
        // CBACT04C:181, 193 (three balances read), 375, 386, 642 with status 23, 629
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C",
                balanceRecord("i01-baseline", 0), balanceRecord("i01-baseline", 1), balanceRecord("i07-nontie-rounding", 0),
                "ACCOUNT NOT FOUND: 00000002009", "ERROR READING ACCOUNT FILE", "FILE STATUS IS: NNNN0023",
                "ABENDING PROGRAM"), runLog());
    }

    @Test
    void intR08_cardMissingStops() throws Exception {
        jdbc.update("DELETE FROM carddemo.card_xref WHERE xref_acct_id = 2009");

        JobExecution execution = launch();

        assertStoppedAt2009KeepingTheCommittedAccounts(execution,
                "CARD_MISSING (INT-R08): an account has no card cross-reference");
        // CBACT04C:397, 408, 642 with status 23, 629
        assertEquals(List.of("ACCOUNT NOT FOUND: 00000002009", "ERROR READING XREF FILE", "FILE STATUS IS: NNNN0023",
                "ABENDING PROGRAM"), runLog().subList(4, runLog().size()));
    }

    @Test
    void intR08_defaultRateMissingStops() throws Exception {
        // no group has a rate for category 9: neither STANDARD, the group of 2009, nor DEFAULT
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM carddemo.disclosure_group WHERE dis_tran_cat_cd = 9", Integer.class));
        jdbc.update("UPDATE carddemo.tran_cat_balance SET trancat_cd = 9 WHERE trancat_acct_id = 2009");

        JobExecution execution = launch();

        assertStoppedAt2009KeepingTheCommittedAccounts(execution,
                "DEFAULT_RATE_MISSING (INT-R08): neither the account's group nor the group DEFAULT has a rate for a balance");
        // CBACT04C:418-419, then 455, 642 with status 23, 629
        assertEquals(List.of("DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE",
                "ERROR READING DEFAULT DISCLOSURE GROUP", "FILE STATUS IS: NNNN0023", "ABENDING PROGRAM"),
                runLog().subList(4, runLog().size()));
    }

    @Test
    void intH03_twoCardsStops() throws Exception {
        // a second card for 2009, below its own 4000000000002009
        jdbc.update("""
                INSERT INTO carddemo.card_xref
                SELECT '4000000000001009', xref_cust_id, xref_acct_id, xref_filler
                  FROM carddemo.card_xref WHERE xref_acct_id = 2009""");

        JobExecution execution = launch();

        assertStoppedAt2009KeepingTheCommittedAccounts(execution, "MULTI_CARD (INT-H03): an account has several cards");
        // no legacy text exists: the run log names the decision
        assertEquals(List.of("INT-H03 ACCOUNT HAS SEVERAL CARDS: 00000002009", "ABENDING PROGRAM"),
                runLog().subList(4, runLog().size()));
    }

    @Test
    void intH05_interestAboveFieldStops() throws Exception {
        // 2009 alone gets category 2 and STANDARD/01/2 a rate of 1300.00 %:
        // 999999999.99 * 1300.00 / 1200 = 1083333333.32 (cut), above the 999999999.99 the amount holds
        jdbc.update("UPDATE carddemo.tran_cat_balance SET trancat_cd = 2, tran_cat_bal = 999999999.99 WHERE trancat_acct_id = 2009");
        assertEquals(1, jdbc.update("""
                UPDATE carddemo.disclosure_group SET dis_int_rate = 1300.00
                 WHERE dis_acct_group_id = 'STANDARD' AND dis_tran_type_cd = '01' AND dis_tran_cat_cd = 2"""));

        JobExecution execution = launch();

        assertStoppedAt2009KeepingTheCommittedAccounts(execution, "OVERFLOW (INT-H05): a value is larger than its field");
        assertEquals(List.of("INT-H05 VALUE DOES NOT FIT ITS FIELD: INTEREST OF A BALANCE", "ABENDING PROGRAM"),
                runLog().subList(4, runLog().size()));
        // nothing is truncated: no row holds the amount with or without its leading digit
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM " + TRANSACTION_TABLE
                + " WHERE tran_amt IN (1083333333.32, 83333333.32)", Integer.class));
    }

    @Test
    void intH05_millionthTransactionStops() throws Exception {
        // A run that has written 999,999 transactions is not built from rows here: the run is stopped at 2009 after
        // its two committed chunks, and its saved restart state (architecture.md section 6) is set to 999,999
        // transactions issued. The restart continues at 2009, whose transaction would be number 1,000,000.
        InterestTables.failOn(jdbc, "INSERT", TRANSACTION_TABLE, "NEW.seq_no = 3");
        JobExecution first = launch();
        assertEquals(BatchStatus.FAILED, first.getStatus());
        InterestTables.stopFailingOn(jdbc, TRANSACTION_TABLE);
        StepExecution stopped = first.getStepExecutions().iterator().next();
        assertEquals(2, stopped.getExecutionContext().getInt(CalcInterestTableRun.STATE_COUNTER));
        stopped.getExecutionContext().putInt(CalcInterestTableRun.STATE_COUNTER, 999_999);
        jobRepository.updateExecutionContext(stopped);

        JobExecution restarted = jobLauncher.run(interestCalcJob, parameters());

        assertEquals(first.getJobInstance().getId(), restarted.getJobInstance().getId());
        assertEquals(BatchStatus.FAILED, restarted.getStatus());
        assertEquals("FAILED commits=0 rollbacks=1 calcInterestStep stopped at account 00000002009: "
                + "OVERFLOW (INT-H05): a run has more transactions than its six-digit counter holds", stepExit(restarted));
        exitCodes.onJobExecution(new JobExecutionEvent(restarted));
        assertEquals(InterestExitCode.STOPPED, exitCodes.getExitCode());
        long run = restarted.getExecutionContext().getLong(CalcInterestTableRun.INTEREST_RUN_ID);
        assertEquals("FAILED", jdbc.queryForObject(
                "SELECT status FROM carddemo.interest_run WHERE run_id = ?", String.class, run));
        // the committed accounts keep their rows; no id ...000000 or ...1000000 was written for 2009
        assertEquals(List.of("2022071800000001 10.00", "2022071800000002 37.00"), jdbc.queryForList(
                "SELECT tran_id || ' ' || tran_amt FROM " + TRANSACTION_TABLE + " ORDER BY run_id, seq_no", String.class));
        assertEquals(List.of("2001 1010.00", "2002 2437.00"), jdbc.queryForList(
                "SELECT acct_id || ' ' || acct_curr_bal FROM carddemo.account WHERE acct_id < 2009 ORDER BY acct_id",
                String.class));
        assertEquals(untouchedAccounts, accountRows("acct_id >= 2009"));
        assertEquals(List.of("account UPDATE 00000002001 false", "account UPDATE 00000002002 false"),
                InterestTables.writeAudit(jdbc));
        // the restart cut the log back to its committed length, then the stop wrote its lines
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C",
                balanceRecord("i01-baseline", 0), balanceRecord("i01-baseline", 1), balanceRecord("i07-nontie-rounding", 0),
                "INT-H05 VALUE DOES NOT FIT ITS FIELD: TRANSACTION NUMBER", "ABENDING PROGRAM"), runLog());
    }

    @Test
    void intR08_noStopLeavesTheRunLogWithoutAStopLine() throws Exception {
        // the same four accounts, nothing broken: the run completes and its log has no line of the failure path
        JobExecution execution = launch();

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        List<String> lines = Fixtures.lines(temp.resolve("actual").resolve(InterestTables.RUN_LOG));
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C",
                balanceRecord("i01-baseline", 0), balanceRecord("i01-baseline", 1),
                balanceRecord("i07-nontie-rounding", 0), balanceRecord("i07-nontie-rounding", 1),
                "END OF EXECUTION OF PROGRAM CBACT04C"), lines);
        assertTrue(lines.stream().noneMatch(line -> line.contains("ABENDING")));
    }
}
