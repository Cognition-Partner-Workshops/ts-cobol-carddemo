package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.JobExecutionEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.jdbc.InterestTables;

/**
 * Ticket 08: each stop condition through the step on PostgreSQL 15, one account per chunk. No
 * fixture has an abend, so the data are synthetic: the input of i01-baseline (accounts 2001 GOLD
 * 12.00 % and 2002 STANDARD 18.50 %) plus a third account 2003, a copy of 2002, so that the
 * failing account 2002 is not the one that ends the input and would have been updated. Account
 * 2001 commits first (1000.00 + 10.00). The run-log lines are the DISPLAY texts of CBACT04C; for
 * INT-H03 and INT-H05 no parity with the host is claimed.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.calc.store=jdbc",
        "interest.calc.commitInterval=1",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class ErrorPathsJdbcIT {

    private static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";
    private static final List<String> LOG_UP_TO_ACCOUNT_2002 = List.of("START OF EXECUTION OF PROGRAM CBACT04C",
            "000000020010100010000010000{", "000000020020100010000024000{");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("carddemo")
            .withUsername("carddemo")
            .withPassword("carddemo")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("..", "..", "db", "postgres", "01-transaction-type.sql")),
                    "/docker-entrypoint-initdb.d/01-transaction-type.sql")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("..", "..", "db", "postgres", "02-interest.sql")),
                    "/docker-entrypoint-initdb.d/02-interest.sql");

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job interestCalcJob;
    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path temp;

    @BeforeEach
    void threeAccounts() {
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));
        InterestTables.insertBalance(jdbc, "000000020030100010000024000{");
        InterestTables.insertCard(jdbc, "400000000000200300000200300000002003");
        jdbc.update("""
                INSERT INTO carddemo.account (acct_id, acct_active_status, acct_curr_bal, acct_credit_limit,
                       acct_cash_credit_limit, acct_open_date, acct_expiraion_date, acct_reissue_date, acct_curr_cyc_credit,
                       acct_curr_cyc_debit, acct_addr_zip, acct_group_id, acct_filler)
                SELECT 2003, acct_active_status, acct_curr_bal, acct_credit_limit, acct_cash_credit_limit, acct_open_date,
                       acct_expiraion_date, acct_reissue_date, acct_curr_cyc_credit, acct_curr_cyc_debit, acct_addr_zip,
                       acct_group_id, acct_filler
                  FROM carddemo.account WHERE acct_id = 2002""");
    }

    private JobExecution run() throws Exception {
        return jobLauncher.run(interestCalcJob, new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, Fixtures.input("i01-baseline").toString())
                .addString(InterestJobParameters.OUTPUT_DIR, temp.resolve("actual").toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters());
    }

    private List<String> runLogLines() {
        return Fixtures.lines(temp.resolve("actual").resolve(RUN_LOG)).stream().map(String::stripTrailing).toList();
    }

    private static List<String> logUpToAccount2002Then(String... lines) {
        return java.util.stream.Stream.concat(LOG_UP_TO_ACCOUNT_2002.stream(), List.of(lines).stream()).toList();
    }

    /**
     * What every stop must leave (ticket 08, Database assertions; standards S6.2, S6.3): the step and
     * the job FAILED with the one stop code; account 2001, committed before the failing chunk, keeps
     * its transaction and its update; accounts 2002 and 2003 have neither; interest_run is FAILED.
     */
    private void assertStoppedAtAccount2002(JobExecution execution, String stopReasonAndRule) {
        StepExecution step = execution.getStepExecutions().iterator().next();
        long interestRunId = execution.getExecutionContext().getLong(JdbcCalcInterestRun.INTEREST_RUN_ID);
        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertEquals(BatchStatus.FAILED, step.getStatus());
        assertTrue(step.getExitStatus().getExitDescription().contains("calcInterestStep failed at account 00000002002"));
        assertTrue(step.getFailureExceptions().stream().anyMatch(failure -> failure.getMessage().contains(stopReasonAndRule)),
                "the stop names " + stopReasonAndRule);
        // the message that reaches the application log names no account (standards S6.5)
        assertFalse(step.getFailureExceptions().stream().anyMatch(failure -> failure.getMessage().contains("2002")));

        InterestExitCodes exitCodes = new InterestExitCodes();
        exitCodes.onJobExecution(new JobExecutionEvent(execution));
        assertEquals(12, exitCodes.getExitCode());

        assertEquals(List.of("2022071800000001/4000000000002001/10.00"), jdbc.queryForList("""
                SELECT tran_id || '/' || tran_card_num || '/' || tran_amt FROM carddemo.interest_transaction
                 WHERE run_id = ? ORDER BY seq_no""", String.class, interestRunId));
        assertEquals(List.of("2001/1010.00", "2002/2400.00", "2003/2400.00"), jdbc.queryForList(
                "SELECT acct_id || '/' || acct_curr_bal FROM carddemo.account ORDER BY acct_id", String.class));
        assertEquals("FAILED", jdbc.queryForObject("SELECT status FROM carddemo.interest_run WHERE run_id = ?",
                String.class, interestRunId));
    }

    @Test
    void intR08_missingAccountStops() throws Exception {
        jdbc.update("DELETE FROM carddemo.account WHERE acct_id = 2002");

        JobExecution execution = run();

        // account 2002 has no row at all, so the balance list of assertStoppedAtAccount2002 does not apply
        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertEquals(List.of("2001/1010.00", "2003/2400.00"), jdbc.queryForList(
                "SELECT acct_id || '/' || acct_curr_bal FROM carddemo.account ORDER BY acct_id", String.class));
        long interestRunId = execution.getExecutionContext().getLong(JdbcCalcInterestRun.INTEREST_RUN_ID);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_transaction WHERE run_id = ?",
                Integer.class, interestRunId));
        assertEquals("FAILED", jdbc.queryForObject("SELECT status FROM carddemo.interest_run WHERE run_id = ?",
                String.class, interestRunId));
        InterestExitCodes exitCodes = new InterestExitCodes();
        exitCodes.onJobExecution(new JobExecutionEvent(execution));
        assertEquals(12, exitCodes.getExitCode());
        // the log holds ACCOUNT NOT FOUND and the account id (CBACT04C:375, 386, 643-646, 629)
        assertEquals(logUpToAccount2002Then("ACCOUNT NOT FOUND: 00000002002", "ERROR READING ACCOUNT FILE",
                "FILE STATUS IS: NNNN0023", "ABENDING PROGRAM"), runLogLines());
    }

    @Test
    void intR08_missingCardStops() throws Exception {
        jdbc.update("DELETE FROM carddemo.card_xref WHERE xref_acct_id = 2002");

        JobExecution execution = run();

        assertStoppedAtAccount2002(execution, "CARD_MISSING (INT-R08)");
        assertEquals(logUpToAccount2002Then("ACCOUNT NOT FOUND: 00000002002", "ERROR READING XREF FILE",
                "FILE STATUS IS: NNNN0023", "ABENDING PROGRAM"), runLogLines());
    }

    @Test
    void intR08_defaultRateMissingStops() throws Exception {
        // account 2002's group has no rates, and the group DEFAULT has none for type 01 category 1
        jdbc.update("UPDATE carddemo.account SET acct_group_id = 'SILVER' WHERE acct_id = 2002");
        jdbc.update("DELETE FROM carddemo.disclosure_group WHERE dis_acct_group_id = 'DEFAULT' AND dis_tran_cat_cd = 1");

        JobExecution execution = run();

        assertStoppedAtAccount2002(execution, "DEFAULT_RATE_MISSING (INT-R08)");
        assertEquals(logUpToAccount2002Then("DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE",
                "ERROR READING DEFAULT DISCLOSURE GROUP", "FILE STATUS IS: NNNN0023", "ABENDING PROGRAM"), runLogLines());
    }

    @Test
    void intH03_twoCardsStops() throws Exception {
        InterestTables.insertCard(jdbc, "300000000000900200000200200000002002");

        JobExecution execution = run();

        assertStoppedAtAccount2002(execution, "MULTI_CARD (INT-H03)");
        assertEquals(logUpToAccount2002Then("INT-H03: THE ACCOUNT HAS SEVERAL CARDS", "ABENDING PROGRAM"), runLogLines());
    }

    @Test
    void intH05_interestAboveFieldStops() throws Exception {
        // 999999999.99 * 9999.99 / 1200 is above 999999999.99; the STANDARD rate is account 2002's and 2003's
        jdbc.update("UPDATE carddemo.tran_cat_balance SET tran_cat_bal = 999999999.99 WHERE trancat_acct_id = 2002");
        jdbc.update("UPDATE carddemo.disclosure_group SET dis_int_rate = 9999.99 WHERE dis_acct_group_id = 'STANDARD'");

        JobExecution execution = run();

        assertStoppedAtAccount2002(execution, "OVERFLOW (INT-H05)");
        // nothing is truncated: no transaction of any amount was written for account 2002 (asserted above).
        // 999999999.99 as S9(09)V99 with the sign on the last digit: 99999999999 -> 9999999999I
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C", "000000020010100010000010000{",
                "00000002002010001" + "9999999999I", "INT-H05: A VALUE DOES NOT FIT ITS FIELD", "ABENDING PROGRAM"),
                runLogLines());
    }

    @Test
    void intR08_accountUpdateFailureStops() throws Exception {
        // a database failure of the update of account 2002: the legacy REWRITE error (CBACT04C:365, 629); no status line,
        // because a database failure does not determine a legacy file status
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION carddemo.forced_failure() RETURNS trigger LANGUAGE plpgsql AS
                $$ BEGIN RAISE EXCEPTION 'forced failure of the test'; END $$""");
        jdbc.execute("CREATE TRIGGER forced_failure BEFORE UPDATE ON carddemo.account FOR EACH ROW "
                + "WHEN (OLD.acct_id = 2002) EXECUTE FUNCTION carddemo.forced_failure()");
        try {
            JobExecution execution = run();

            assertStoppedAtAccount2002(execution, "UPDATE_ACCOUNT (INT-R08)");
            assertEquals(logUpToAccount2002Then("ERROR RE-WRITING ACCOUNT FILE", "ABENDING PROGRAM"), runLogLines());
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS forced_failure ON carddemo.account");
        }
    }
}
