package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.CardXrefRecord;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.jdbc.calc.ReplicaTables;

/**
 * Ticket 08: every stop of calcInterestStep on the tables (INT-R08, INT-H03, INT-H05), PostgreSQL
 * 15, two accounts per chunk. No fixture drives an error branch; the data is that of
 * CalcInterestRestartIT (i01-baseline, i05-multi-category and i07-nontie-rounding together),
 * changed by each test:
 *
 * <pre>
 * chunk 1  2001 GOLD      01/0001 1000.00 x 12.00 / 1200 = 10.00   id ..000001  balance 1010.00
 *          2002 STANDARD  01/0001 2400.00 x 18.50 / 1200 = 37.00   id ..000002  balance 2437.00
 * chunk 2  2007 GOLD      01/0001 1000.00, 01/0002 500.00          balance 1500.00 before the run
 *          2009 STANDARD  01/0001 1000.33                          balance 1000.33 before the run
 * chunk 3  2010 DEFAULT   01/0001  777.77
 * </pre>
 * Every test makes chunk 2 fail. After the failure chunk 1 stays committed, nothing of chunk 2
 * stays, and interest_run.status is FAILED. Parity is not claimed for any of these paths: no
 * recorded legacy output exists for an abend, and what CBACT04C does on the host for INT-H03 and
 * INT-H05 is unknown.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.calc.store=jdbc",
        "interest.calc.commitInterval=2",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class ErrorPathsIT {

    private static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";

    /** What chunk 1 commits: seq_no, tran_id, description, amount, card. */
    private static final List<String> TRANSACTIONS_OF_CHUNK_1 = List.of(
            "1 2022071800000001 Int. for a/c 00000002001 10.00 4000000000002001",
            "2 2022071800000002 Int. for a/c 00000002002 37.00 4000000000002002");
    /** acct_id, acct_curr_bal, acct_curr_cyc_credit, acct_curr_cyc_debit: chunk 1 updated, the rest as loaded. */
    private static final List<String> ACCOUNTS_AFTER_CHUNK_1 = List.of(
            "2001 1010.00 0.00 0.00",
            "2002 2437.00 0.00 0.00",
            "2007 1500.00 1500.00 0.00",
            "2009 1000.33 1000.33 0.00",
            "2010 777.77 777.77 0.00");
    private static final List<String> RUN_LOG_OF_CHUNK_1 = List.of(
            "START OF EXECUTION OF PROGRAM CBACT04C",
            "000000020010100010000010000{                      ",
            "000000020020100010000024000{                      ");
    private static final String BALANCE_2007_1 = "000000020070100010000010000{                      ";
    private static final String BALANCE_2007_2 = "000000020070100020000005000{                      ";
    private static final String BALANCE_2009_1 = "000000020090100010000010003C                      ";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = ReplicaTables.postgres();

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private Job interestCalcJob;
    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path temp;

    @BeforeEach
    void fiveAccounts() {
        ReplicaTables.load(jdbc, Fixtures.input("i01-baseline"), Fixtures.input("i05-multi-category"),
                Fixtures.input("i07-nontie-rounding"));
        jdbc.execute("DROP TRIGGER IF EXISTS test_forced_failure ON carddemo.account");
        jdbc.execute("DROP TRIGGER IF EXISTS test_forced_failure ON carddemo.interest_transaction");
    }

    private JobParameters parametersOfANewRun() {
        return new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, temp.toString())
                .addString(InterestJobParameters.OUTPUT_DIR, temp.resolve("actual").toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters();
    }

    private List<String> transactions() {
        return jdbc.queryForList("SELECT seq_no || ' ' || tran_id || ' ' || rtrim(tran_desc) || ' ' || tran_amt || ' '"
                + " || tran_card_num FROM carddemo.interest_transaction ORDER BY run_id, seq_no", String.class);
    }

    private List<String> accounts() {
        return jdbc.queryForList("SELECT acct_id || ' ' || acct_curr_bal || ' ' || acct_curr_cyc_credit || ' '"
                + " || acct_curr_cyc_debit FROM carddemo.account ORDER BY acct_id", String.class);
    }

    private List<String> runLog() {
        return Fixtures.lines(temp.resolve("actual").resolve(RUN_LOG));
    }

    private static List<String> runLogOfChunk1Then(String... lines) {
        return java.util.stream.Stream.concat(RUN_LOG_OF_CHUNK_1.stream(), List.of(lines).stream()).toList();
    }

    /** The trigger's message quotes the row, as a constraint message of PostgreSQL can. */
    private void refuse(String operation, String table, String when) {
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION carddemo.test_forced_failure() RETURNS trigger AS $$
                BEGIN RAISE EXCEPTION 'forced failure, row detail %', NEW; END
                $$ LANGUAGE plpgsql""");
        jdbc.execute("CREATE TRIGGER test_forced_failure BEFORE " + operation + " ON " + table + " FOR EACH ROW WHEN ("
                + when + ") EXECUTE FUNCTION carddemo.test_forced_failure()");
    }

    /**
     * The assertions every stop shares: step and job FAILED, the one non-zero exit code, the stop
     * reason and its rule id in the exit description, chunk 1 committed, nothing of chunk 2,
     * interest_run.status FAILED, no account id in the application log.
     */
    private void assertStopped(JobExecution failed, String reasonAndRule, long accountInWork, List<String> accountsLeft,
            CapturedOutput applicationLog) {
        assertEquals(BatchStatus.FAILED, failed.getStatus());
        StepExecution step = failed.getStepExecutions().iterator().next();
        assertEquals(BatchStatus.FAILED, step.getStatus());
        assertEquals("FAILED", step.getExitStatus().getExitCode());
        String exitDescription = step.getExitStatus().getExitDescription();
        assertTrue(exitDescription.contains(reasonAndRule), exitDescription);
        assertTrue(exitDescription.contains("calcInterestStep stopped at account " + String.format("%011d", accountInWork)),
                exitDescription);

        InterestExitCodes exitCodes = new InterestExitCodes();
        exitCodes.onJobExecution(new JobExecutionEvent(failed));
        assertEquals(InterestExitCode.STOPPED, exitCodes.getExitCode());
        assertNotEquals(0, exitCodes.getExitCode());

        assertEquals(TRANSACTIONS_OF_CHUNK_1, transactions());
        assertEquals(accountsLeft, accounts());
        assertEquals(List.of("FAILED"), jdbc.queryForList("SELECT status FROM carddemo.interest_run", String.class));

        // standards S6.5: the account ids of the failing chunk stay out of the application log
        assertFalse(applicationLog.getAll().contains("00000002007"), "account id in the application log");
        assertFalse(applicationLog.getAll().contains("00000002009"), "account id in the application log");
    }

    @Test
    void intR08_missingAccountStops(CapturedOutput applicationLog) throws Exception {
        // the balance of 2009 has no account row (CBACT04C:373-390); 2007 is in the same chunk
        jdbc.update("DELETE FROM carddemo.account WHERE acct_id = 2009");

        JobExecution failed = jobLauncher.run(interestCalcJob, parametersOfANewRun());

        assertStopped(failed, "ACCOUNT_MISSING (INT-R08)", 2009, ACCOUNTS_AFTER_CHUNK_1.stream()
                .filter(account -> !account.startsWith("2009")).toList(), applicationLog);
        assertEquals(runLogOfChunk1Then(BALANCE_2007_1, BALANCE_2007_2, BALANCE_2009_1,
                "ACCOUNT NOT FOUND: 00000002009",
                "ERROR READING ACCOUNT FILE",
                "FILE STATUS IS: NNNN0023",
                "ABENDING PROGRAM"), runLog());
    }

    @Test
    void intR08_missingCardStops(CapturedOutput applicationLog) throws Exception {
        // CBACT04C:394-412
        jdbc.update("DELETE FROM carddemo.card_xref WHERE xref_acct_id = 2007");

        JobExecution failed = jobLauncher.run(interestCalcJob, parametersOfANewRun());

        assertStopped(failed, "CARD_MISSING (INT-R08)", 2007, ACCOUNTS_AFTER_CHUNK_1, applicationLog);
        assertEquals(runLogOfChunk1Then(BALANCE_2007_1,
                "ACCOUNT NOT FOUND: 00000002007",
                "ERROR READING XREF FILE",
                "FILE STATUS IS: NNNN0023",
                "ABENDING PROGRAM"), runLog());
    }

    @Test
    void intR08_noRateForTheGroupAndNoneForDefaultStops(CapturedOutput applicationLog) throws Exception {
        // 2007 is in a group without rates and DEFAULT has no rate for 01/0001 (CBACT04C:416-419, 443-459)
        jdbc.update("UPDATE carddemo.account SET acct_group_id = 'SILVER' WHERE acct_id = 2007");
        jdbc.update("DELETE FROM carddemo.disclosure_group WHERE dis_acct_group_id = 'DEFAULT' AND dis_tran_cat_cd = 1"
                + " AND dis_tran_type_cd = '01'");
        // 2010 is in group DEFAULT and would stop too; it is in chunk 3, which is never reached

        JobExecution failed = jobLauncher.run(interestCalcJob, parametersOfANewRun());

        assertStopped(failed, "DEFAULT_RATE_MISSING (INT-R08)", 2007, ACCOUNTS_AFTER_CHUNK_1, applicationLog);
        assertEquals(runLogOfChunk1Then(BALANCE_2007_1,
                "DISCLOSURE GROUP RECORD MISSING",
                "TRY WITH DEFAULT GROUP CODE",
                "ERROR READING DEFAULT DISCLOSURE GROUP",
                "FILE STATUS IS: NNNN0023",
                "ABENDING PROGRAM"), runLog());
    }

    @Test
    void intH03_twoCardsStops(CapturedOutput applicationLog) throws Exception {
        // a second card for account 2007; multiCard is STOP, the proposed default
        ReplicaTables.insert(jdbc, ReplicaTables.CARD_XREF, RecordValues.blank(CardXrefRecord.LAYOUT)
                .with("XREF-CARD-NUM", "4000000000001007").with("XREF-CUST-ID", 20072L).with("XREF-ACCT-ID", 2007L));

        JobExecution failed = jobLauncher.run(interestCalcJob, parametersOfANewRun());

        assertStopped(failed, "MULTI_CARD (INT-H03)", 2007, ACCOUNTS_AFTER_CHUNK_1, applicationLog);
        // no legacy text exists for this stop
        assertEquals(runLogOfChunk1Then(BALANCE_2007_1,
                "INT-H03 ACCOUNT HAS SEVERAL CARDS: 00000002007",
                "ABENDING PROGRAM"), runLog());
        assertFalse(applicationLog.getAll().contains("4000000000001007"), "card number in the application log");
    }

    @Test
    void intH05_interestAboveFieldStops(CapturedOutput applicationLog) throws Exception {
        // TRAN-AMT is S9(09)V99. 999999999.99 x 1500.00 / 1200 = 1249999999.9875: ten integer digits.
        jdbc.update("INSERT INTO carddemo.disclosure_group (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd,"
                + " dis_int_rate, dis_filler) VALUES ('PLATINUM', '01', 1, 1500.00, decode(repeat('20', 28), 'hex'))");
        jdbc.update("UPDATE carddemo.account SET acct_group_id = 'PLATINUM' WHERE acct_id = 2007");
        jdbc.update("UPDATE carddemo.tran_cat_balance SET tran_cat_bal = 999999999.99"
                + " WHERE trancat_acct_id = 2007 AND trancat_cd = 1");

        JobExecution failed = jobLauncher.run(interestCalcJob, parametersOfANewRun());

        assertStopped(failed, "OVERFLOW (INT-H05)", 2007, ACCOUNTS_AFTER_CHUNK_1, applicationLog);
        // the record as displayed: the eleven digits 99999999999, the last one as the overpunch I for +9 (E2)
        assertEquals(runLogOfChunk1Then("000000020070100019999999999I                      ",
                "INT-H05 VALUE DOES NOT FIT FIELD: TRAN-AMT",
                "ABENDING PROGRAM"), runLog());
        // nothing is truncated: no transaction holds the amount without its leading digit
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_transaction"
                + " WHERE tran_amt = 249999999.98", Integer.class));
    }

    @Test
    void intH05_millionthTransactionStops(CapturedOutput applicationLog) throws Exception {
        // The counter of TRAN-ID has six digits (CBACT04C:173, 474). Writing 999,999 transactions first
        // is not practical here, so the run is stopped after chunk 1 by a refused UPDATE and the counter
        // of its saved restart state is put at 999997; the restarted execution of the same run then
        // issues 999998 and 999999 for account 2007 and has no id left for account 2009.
        JobParameters parameters = parametersOfANewRun();
        refuse("UPDATE", "carddemo.account", "OLD.acct_id = 2009");
        JobExecution first = jobLauncher.run(interestCalcJob, parameters);
        assertEquals(BatchStatus.FAILED, first.getStatus());
        jdbc.execute("DROP TRIGGER test_forced_failure ON carddemo.account");
        StepExecution firstStep = first.getStepExecutions().iterator().next();
        assertEquals(2, firstStep.getExecutionContext().getInt("calcInterestStep.transactionCounter"));
        firstStep.getExecutionContext().putInt("calcInterestStep.transactionCounter", 999_997);
        jobRepository.updateExecutionContext(firstStep);

        JobExecution failed = jobLauncher.run(interestCalcJob, parameters);

        assertStopped(failed, "OVERFLOW (INT-H05)", 2009, ACCOUNTS_AFTER_CHUNK_1, applicationLog);
        assertEquals(runLogOfChunk1Then(BALANCE_2007_1, BALANCE_2007_2, BALANCE_2009_1,
                "INT-H05 VALUE DOES NOT FIT FIELD: TRAN-ID",
                "ABENDING PROGRAM"), runLog());
        // the counter did not start again: no transaction of this run has the id ..000000
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_transaction"
                + " WHERE tran_id = '2022071800000000'", Integer.class));
    }

    @Test
    void intR08_refusedAccountUpdateStopsWithoutTheDriversRowDetail(CapturedOutput applicationLog) throws Exception {
        // CBACT04C:356-369: the database refuses the UPDATE of 2009, after the INSERTs of chunk 2
        refuse("UPDATE", "carddemo.account", "OLD.acct_id = 2009");

        JobExecution failed = jobLauncher.run(interestCalcJob, parametersOfANewRun());

        assertStopped(failed, "UPDATE_ACCOUNT (INT-R08)", 2009, ACCOUNTS_AFTER_CHUNK_1, applicationLog);
        // the legacy message of the REWRITE; no status line, because no legacy status is determined (S6.1)
        assertEquals(runLogOfChunk1Then(BALANCE_2007_1, BALANCE_2007_2, BALANCE_2009_1,
                "ERROR RE-WRITING ACCOUNT FILE",
                "ABENDING PROGRAM"), runLog());
        assertDriverTextIsNowhere(failed, applicationLog);
        assertTrue(applicationLog.getAll().contains("database failure: reason=UPDATE_ACCOUNT"), "cause in the application log");
        // P0001 is the SQLSTATE of RAISE EXCEPTION in PL/pgSQL
        assertTrue(applicationLog.getAll().contains("sqlState=P0001"), "SQLSTATE in the application log");
    }

    @Test
    void intR08_refusedTransactionInsertStopsWithoutTheDriversRowDetail(CapturedOutput applicationLog) throws Exception {
        // CBACT04C:500-514: the database refuses the INSERT of the first transaction of 2007
        refuse("INSERT", "carddemo.interest_transaction", "NEW.seq_no = 3");

        JobExecution failed = jobLauncher.run(interestCalcJob, parametersOfANewRun());

        assertStopped(failed, "WRITE_TRANSACTION (INT-R08)", 2007, ACCOUNTS_AFTER_CHUNK_1, applicationLog);
        assertEquals(runLogOfChunk1Then(BALANCE_2007_1, BALANCE_2007_2, BALANCE_2009_1,
                "ERROR WRITING TRANSACTION RECORD",
                "ABENDING PROGRAM"), runLog());
        assertDriverTextIsNowhere(failed, applicationLog);
    }

    @Test
    void intR08_unreadableBalancesStopBeforeAnyAccount(CapturedOutput applicationLog) throws Exception {
        // CBACT04C:341-345: the balances cannot be read at all
        jdbc.execute("ALTER TABLE carddemo.tran_cat_balance RENAME TO tran_cat_balance_gone");
        try {
            JobExecution failed = jobLauncher.run(interestCalcJob, parametersOfANewRun());

            assertEquals(BatchStatus.FAILED, failed.getStatus());
            String exitDescription = failed.getStepExecutions().iterator().next().getExitStatus().getExitDescription();
            assertTrue(exitDescription.contains("READ_BALANCES (INT-R08)"), exitDescription);
            assertTrue(exitDescription.contains("calcInterestStep stopped between accounts"), exitDescription);
            assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C",
                    "ERROR READING TRANSACTION CATEGORY FILE",
                    "ABENDING PROGRAM"), runLog());
            assertEquals(List.of(), transactions());
            assertEquals(List.of("FAILED"), jdbc.queryForList("SELECT status FROM carddemo.interest_run", String.class));
            // 42P01 is undefined_table
            assertTrue(applicationLog.getAll().contains("sqlState=42P01"), "SQLSTATE in the application log");
        } finally {
            jdbc.execute("ALTER TABLE carddemo.tran_cat_balance_gone RENAME TO tran_cat_balance");
        }
    }

    /** The text the trigger raises quotes the row; it reaches neither the application log nor the job repository. */
    private static void assertDriverTextIsNowhere(JobExecution failed, CapturedOutput applicationLog) {
        String exitDescription = failed.getStepExecutions().iterator().next().getExitStatus().getExitDescription();
        for (String text : new String[] {applicationLog.getAll(), exitDescription}) {
            assertFalse(text.contains("row detail"), "the driver's message was kept");
            assertFalse(text.contains("PSQLException"), "the driver's exception was kept as the cause");
        }
    }
}
