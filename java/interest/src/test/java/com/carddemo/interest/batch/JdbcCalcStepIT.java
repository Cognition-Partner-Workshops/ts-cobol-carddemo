package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.jdbc.InterestTables;
import com.carddemo.interest.jdbc.JdbcBalanceSource;
import com.carddemo.interest.jdbc.JdbcCardLookup;
import com.carddemo.interest.jdbc.JdbcRateLookup;

/**
 * Ticket 06: calcInterestStep on the tables of db/postgres/02-interest.sql, PostgreSQL 15 in a
 * container with the init scripts of the db service (standards S7.3). The replica tables hold the
 * input files of a recorded case; the expected side is the recorded legacy output of that case
 * (standards S7.4). One account per chunk, so every account is its own commit.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.calc.store=jdbc",
        "interest.calc.commitInterval=1",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class JdbcCalcStepIT {

    private static final String TRANSACTIONS = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt";
    private static final String ACCOUNTS = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt";
    private static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";
    /** Every column of account the calculation may not write, and the three replica tables it only reads. */
    private static final String UNTOUCHED = """
            SELECT (SELECT md5(coalesce(string_agg(ROW(acct_id, acct_active_status, acct_credit_limit, acct_cash_credit_limit,
                           acct_open_date, acct_expiraion_date, acct_reissue_date, acct_addr_zip, acct_group_id, acct_filler)::text,
                           '|' ORDER BY acct_id), '')) FROM carddemo.account)
                || (SELECT md5(coalesce(string_agg(t::text, '|' ORDER BY t::text), '')) FROM carddemo.tran_cat_balance t)
                || (SELECT md5(coalesce(string_agg(t::text, '|' ORDER BY t::text), '')) FROM carddemo.card_xref t)
                || (SELECT md5(coalesce(string_agg(t::text, '|' ORDER BY t::text), '')) FROM carddemo.disclosure_group t)""";

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
    @Autowired
    private DataSource dataSource;

    @TempDir
    Path temp;

    private JobParameters parameters(String caseName) {
        return new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, Fixtures.input(caseName).toString())
                .addString(InterestJobParameters.OUTPUT_DIR, temp.resolve("actual").toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters();
    }

    private static long interestRunId(JobExecution execution) {
        return execution.getExecutionContext().getLong(JdbcCalcInterestRun.INTEREST_RUN_ID);
    }

    private static StepExecution calcStep(JobExecution execution) {
        return execution.getStepExecutions().iterator().next();
    }

    private String runStatus(long interestRunId) {
        return jdbc.queryForObject("SELECT status FROM carddemo.interest_run WHERE run_id = ?", String.class, interestRunId);
    }

    private BigDecimal balanceOf(long accountId) {
        return jdbc.queryForObject("SELECT acct_curr_bal FROM carddemo.account WHERE acct_id = ?", BigDecimal.class, accountId);
    }

    private int transactionsOf(long interestRunId) {
        return jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_transaction WHERE run_id = ?", Integer.class,
                interestRunId);
    }

    private void assertRecordedOutput(String caseName, JobExecution execution) {
        InterestTables.assertTransactionsEqual(jdbc, interestRunId(execution),
                Fixtures.lines(Fixtures.expected(caseName).resolve(TRANSACTIONS)));
        InterestTables.assertAccountsEqual(jdbc, Fixtures.lines(Fixtures.expected(caseName).resolve(ACCOUNTS)));
        assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve(RUN_LOG)),
                Fixtures.text(temp.resolve("actual").resolve(RUN_LOG)), caseName + " run log");
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void fixtureCase_tablesAndRunLogEqualTheRecordedOutput(String caseName) throws Exception {
        // ticket 06: i05-multi-category is the acceptance case; i01 and i07 are named under Verification
        InterestTables.load(jdbc, Fixtures.input(caseName));
        String untouchedBefore = jdbc.queryForObject(UNTOUCHED, String.class);

        JobExecution execution = jobLauncher.run(interestCalcJob, parameters(caseName));

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertRecordedOutput(caseName, execution);
        // database assertions: only acct_curr_bal, acct_curr_cyc_credit, acct_curr_cyc_debit of account may change;
        // tran_cat_balance, card_xref and disclosure_group are unchanged (row hash before and after)
        assertEquals(untouchedBefore, jdbc.queryForObject(UNTOUCHED, String.class));
        assertEquals("CALCULATED", runStatus(interestRunId(execution)));
    }

    @Test
    void intR07_interestRunHoldsWhatTheCalculationWrote() throws Exception {
        // i01-baseline: two transactions, 10.00 + 37.00 = 47.00; one account updated (2002 ends the input, INT-Q04);
        // balances after the run 1010.00 + 2400.00 = 3410.00
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));

        JobExecution execution = jobLauncher.run(interestCalcJob, parameters("i01-baseline"));

        assertEquals("2022071800/2/47.00/1/3410.00", jdbc.queryForObject("""
                SELECT parm_date || '/' || written_transaction_count || '/' || written_tran_amt_sum || '/'
                       || written_account_count || '/' || written_acct_curr_bal_sum
                  FROM carddemo.interest_run WHERE run_id = ?""", String.class, interestRunId(execution)));
        // one item is one account, one chunk is one account: two commits with items
        assertEquals(2, calcStep(execution).getReadCount());
        assertEquals(2, calcStep(execution).getWriteCount());
    }

    @Test
    void intR01_readsInKeyOrder() {
        // synthetic rows inserted out of key order: account, then type code in "C" order, then category.
        // Digits sort before letters in the C collation (rule E6, G06): 01 < 0A < A1
        jdbc.execute("TRUNCATE carddemo.tran_cat_balance");
        for (String line : List.of(
                "00000002002A100010000024000{",
                "000000020020100020000000500{",
                "000000020010A00010000005000{",
                "000000020020100010000010000{",
                "000000020010100010000010000{")) {
            InterestTables.insertBalance(jdbc, line);
        }

        try (JdbcBalanceSource balances = JdbcBalanceSource.openAfter(dataSource, -1L)) {
            AccountBalances first = balances.nextAccount().orElseThrow();
            AccountBalances second = balances.nextAccount().orElseThrow();

            assertEquals(2001L, first.accountId());
            assertEquals(List.of("01/1", "0A/1"), keys(first));
            assertEquals(2002L, second.accountId());
            assertEquals(List.of("01/1", "01/2", "A1/1"), keys(second));
            // the account that ends the input is known from the row read ahead, with no further query (INT-Q04)
            assertFalse(first.endsInput());
            assertTrue(second.endsInput());
            assertEquals(Optional.empty(), balances.nextAccount());
            // INT-T02: the record image is the 50-byte record, FILLER included
            assertEquals("000000020010100010000010000{" + " ".repeat(22), first.balances().get(0).recordImage());
        }
    }

    private static List<String> keys(AccountBalances account) {
        return account.balances().stream()
                .map(read -> read.balance().typeCode() + "/" + read.balance().categoryCode()).toList();
    }

    @Test
    void intR01_restartReadsOnAfterTheLastCommittedAccount() {
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));

        try (JdbcBalanceSource balances = JdbcBalanceSource.openAfter(dataSource, 2001L)) {
            AccountBalances next = balances.nextAccount().orElseThrow();

            assertEquals(2002L, next.accountId());
            assertTrue(next.endsInput());
        }
    }

    @Test
    void intR03_defaultGroupKeyPadded() {
        // i04: dis_acct_group_id is CHAR(10); the unpadded constant DEFAULT finds 21.00 %, PLATINUM finds nothing
        InterestTables.load(jdbc, Fixtures.input("i04-default-group-fallback"));
        JdbcRateLookup rates = new JdbcRateLookup(jdbc);

        assertEquals(Optional.of(new BigDecimal("21.00")), rates.findRate("DEFAULT", "01", 1));
        assertEquals(Optional.empty(), rates.findRate("PLATINUM", "01", 1));
    }

    @Test
    void intR06_cardNumbersOfAccountInCardNumberOrder() {
        // synthetic second card for account 2001, lower than the fixture's 4000000000002001
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));
        InterestTables.insertCard(jdbc, "300000000000900100000200100000002001");
        JdbcCardLookup cards = new JdbcCardLookup(jdbc);

        assertEquals(List.of("3000000000009001", "4000000000002001"), cards.cardNumbersOf(2001L));
        assertEquals(List.of("4000000000002002"), cards.cardNumbersOf(2002L));
        assertEquals(List.of(), cards.cardNumbersOf(2999L));
    }

    @Test
    void intR07_insertsAndUpdateOfAnAccountCommitTogether() throws Exception {
        // a forced failure between the inserts of account 2001's transaction and the update of account 2001:
        // neither stays. The step ends FAILED and names the account in its exit description, not in a log message
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));
        failOn("carddemo.account", "UPDATE", "OLD.acct_id = 2001");
        try {
            JobExecution execution = jobLauncher.run(interestCalcJob, parameters("i01-baseline"));

            assertEquals(BatchStatus.FAILED, execution.getStatus());
            assertEquals(BatchStatus.FAILED, calcStep(execution).getStatus());
            assertEquals(0, transactionsOf(interestRunId(execution)));
            assertEquals(new BigDecimal("1000.00"), balanceOf(2001L));
            InterestTables.assertAccountsEqual(jdbc,
                    Fixtures.lines(Fixtures.input("i01-baseline").resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt")));
            assertTrue(calcStep(execution).getExitStatus().getExitDescription()
                    .contains("calcInterestStep failed at account 00000002001"));
            assertEquals("FAILED", runStatus(interestRunId(execution)));
        } finally {
            stopFailing("carddemo.account");
        }
    }

    @Test
    void intH02_failedRunRestartsWithoutChargingTwice() throws Exception {
        // i01-baseline, one account per chunk. The first execution commits account 2001 and fails on the
        // insert of account 2002's transaction; the restart must end with the recorded output of an
        // uninterrupted legacy run: same transaction ids, account 2001 charged once, run log complete
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));
        JobParameters parameters = parameters("i01-baseline");
        failOn("carddemo.interest_transaction", "INSERT", "NEW.tran_desc LIKE 'Int. for a/c 00000002002%'");
        JobExecution failed;
        try {
            failed = jobLauncher.run(interestCalcJob, parameters);
        } finally {
            stopFailing("carddemo.interest_transaction");
        }
        long interestRunId = interestRunId(failed);

        // errors: the step is FAILED, names the account, and the committed chunk stays committed
        assertEquals(BatchStatus.FAILED, failed.getStatus());
        assertTrue(calcStep(failed).getExitStatus().getExitDescription()
                .contains("calcInterestStep failed at account 00000002002"));
        assertEquals(1, transactionsOf(interestRunId));
        assertEquals(new BigDecimal("1010.00"), balanceOf(2001L));
        assertEquals("FAILED", runStatus(interestRunId));
        // the run log holds what was displayed before the failure, the record of account 2002 included
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C", "000000020010100010000010000{",
                "000000020020100010000024000{"), runLogLines());

        JobExecution restarted = jobLauncher.run(interestCalcJob, parameters);

        assertEquals(BatchStatus.COMPLETED, restarted.getStatus());
        assertEquals(failed.getJobInstance().getInstanceId(), restarted.getJobInstance().getInstanceId());
        assertEquals(interestRunId, interestRunId(restarted));
        // same rows as an uninterrupted run: ids 2022071800000001 and ...000002, seq_no 1 and 2
        assertRecordedOutput("i01-baseline", restarted);
        // no account updated twice: 1000.00 + 10.00, not 1020.00
        assertEquals(new BigDecimal("1010.00"), balanceOf(2001L));
        assertEquals("CALCULATED/2/1", jdbc.queryForObject("""
                SELECT status || '/' || written_transaction_count || '/' || written_account_count
                  FROM carddemo.interest_run WHERE run_id = ?""", String.class, interestRunId));
        // the restarted execution read only the account that was not committed
        assertEquals(1, calcStep(restarted).getReadCount());
    }

    @Test
    void intH02_secondRunWithTheSameParmIsNotRefused() throws Exception {
        // rerunGuard stays OFF (INT-H02): a new runId is a new run and charges again, as the legacy would.
        // Interest is computed on the category balance, which no run changes: 1000.00 + 10.00 + 10.00 = 1020.00
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));

        JobExecution first = jobLauncher.run(interestCalcJob, parameters("i01-baseline"));
        JobExecution second = jobLauncher.run(interestCalcJob, parameters("i01-baseline"));

        assertEquals(BatchStatus.COMPLETED, second.getStatus());
        assertTrue(interestRunId(first) != interestRunId(second));
        assertEquals(2, transactionsOf(interestRunId(first)));
        assertEquals(2, transactionsOf(interestRunId(second)));
        assertEquals(new BigDecimal("1020.00"), balanceOf(2001L));
        // TRAN-ID is unique inside a run only: both runs wrote 2022071800000001
        assertEquals(2, jdbc.queryForObject(
                "SELECT count(*) FROM carddemo.interest_transaction WHERE tran_id = '2022071800000001' AND run_id IN (?, ?)",
                Integer.class, interestRunId(first), interestRunId(second)));
    }

    @Test
    void intT09_emptyBalanceTableWritesNothing() throws Exception {
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));
        jdbc.execute("TRUNCATE carddemo.tran_cat_balance");

        JobExecution execution = jobLauncher.run(interestCalcJob, parameters("i01-baseline"));

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(0, transactionsOf(interestRunId(execution)));
        InterestTables.assertAccountsEqual(jdbc,
                Fixtures.lines(Fixtures.input("i01-baseline").resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt")));
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C", "END OF EXECUTION OF PROGRAM CBACT04C"), runLogLines());
    }

    private List<String> runLogLines() {
        return Fixtures.lines(temp.resolve("actual").resolve(RUN_LOG)).stream().map(String::stripTrailing).toList();
    }

    /** Makes one statement on a table fail for the rows a condition selects, the way a database error would. */
    private void failOn(String table, String operation, String condition) {
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION carddemo.forced_failure() RETURNS trigger LANGUAGE plpgsql AS
                $$ BEGIN RAISE EXCEPTION 'forced failure of the test'; END $$""");
        jdbc.execute("CREATE TRIGGER forced_failure BEFORE " + operation + " ON " + table
                + " FOR EACH ROW WHEN (" + condition + ") EXECUTE FUNCTION carddemo.forced_failure()");
    }

    private void stopFailing(String table) {
        jdbc.execute("DROP TRIGGER IF EXISTS forced_failure ON " + table);
    }
}
