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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.jdbc.calc.ReplicaTables;

/**
 * Commit, rollback and restart of calcInterestStep on the tables (architecture.md sections 5 and
 * 6, REL-11 as proposed), PostgreSQL 15, two accounts per chunk.
 *
 * <p>Input: the balances, accounts and cards of i01-baseline, i05-multi-category and
 * i07-nontie-rounding together (the rates are the same file in all three). In key order:
 *
 * <pre>
 * chunk 1  2001 GOLD      01/0001 1000.00 x 12.00 / 1200 = 10.00              id ..000001  balance 1010.00
 *          2002 STANDARD  01/0001 2400.00 x 18.50 / 1200 = 37.00              id ..000002  balance 2437.00
 * chunk 2  2007 GOLD      01/0001 1000.00 x 12.00 / 1200 = 10.00              id ..000003
 *                         01/0002  500.00 x 18.00 / 1200 =  7.50              id ..000004  balance 1517.50
 *          2009 STANDARD  01/0001 1000.33 x 18.50 / 1200 = 15.4217.. -> 15.42 id ..000005  balance 1015.75
 * chunk 3  2010 DEFAULT   01/0001  777.77 x 21.00 / 1200 = 13.6109.. -> 13.61 id ..000006  ends the input:
 *                                                                              not updated (INT-Q04), 777.77
 * </pre>
 * The amounts are those of the recorded SYSTRAN records of the three cases. The failure is forced
 * by a trigger that refuses the UPDATE of account 2009, after the INSERTs of chunk 2.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.calc.store=jdbc",
        "interest.calc.commitInterval=2",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class CalcInterestRestartIT {

    private static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";
    private static final String FROZEN_TIMESTAMP = "2022-07-05-10.00.00.000000";

    /** What an uninterrupted run leaves, from the table above: tran_id, account of the description, amount. */
    private static final List<String> ALL_TRANSACTIONS = List.of(
            "1 2022071800000001 Int. for a/c 00000002001 10.00 4000000000002001",
            "2 2022071800000002 Int. for a/c 00000002002 37.00 4000000000002002",
            "3 2022071800000003 Int. for a/c 00000002007 10.00 4000000000002007",
            "4 2022071800000004 Int. for a/c 00000002007 7.50 4000000000002007",
            "5 2022071800000005 Int. for a/c 00000002009 15.42 4000000000002009",
            "6 2022071800000006 Int. for a/c 00000002010 13.61 4000000000002010");
    /** acct_id, acct_curr_bal, acct_curr_cyc_credit, acct_curr_cyc_debit. */
    private static final List<String> ALL_ACCOUNTS = List.of(
            "2001 1010.00 0.00 0.00",
            "2002 2437.00 0.00 0.00",
            "2007 1517.50 0.00 0.00",
            "2009 1015.75 0.00 0.00",
            "2010 777.77 777.77 0.00");
    private static final List<String> WHOLE_RUN_LOG = List.of(
            "START OF EXECUTION OF PROGRAM CBACT04C",
            "000000020010100010000010000{                      ",
            "000000020020100010000024000{                      ",
            "000000020070100010000010000{                      ",
            "000000020070100020000005000{                      ",
            "000000020090100010000010003C                      ",
            "000000020100100010000007777G                      ",
            "END OF EXECUTION OF PROGRAM CBACT04C");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = ReplicaTables.postgres();

    @Autowired
    private JobLauncher jobLauncher;
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
        ReplicaTables.refuseUpdatesOfOtherAccountColumns(jdbc);
        jdbc.execute("DROP TRIGGER IF EXISTS test_forced_failure ON carddemo.account");
    }

    private JobParameters parametersOfANewRun(Path outputDir) {
        return new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, temp.toString())
                .addString(InterestJobParameters.OUTPUT_DIR, outputDir.toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters();
    }

    private void refuseTheUpdateOfAccount(long accountId) {
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION carddemo.test_forced_failure() RETURNS trigger AS $$
                BEGIN RAISE EXCEPTION 'forced failure between the inserts and the update'; END
                $$ LANGUAGE plpgsql""");
        jdbc.execute("CREATE TRIGGER test_forced_failure BEFORE UPDATE ON carddemo.account FOR EACH ROW WHEN (OLD.acct_id = "
                + accountId + ") EXECUTE FUNCTION carddemo.test_forced_failure()");
    }

    private List<String> transactions() {
        return jdbc.queryForList("SELECT seq_no || ' ' || tran_id || ' ' || rtrim(tran_desc) || ' ' || tran_amt || ' '"
                + " || tran_card_num FROM carddemo.interest_transaction ORDER BY run_id, seq_no", String.class);
    }

    private List<String> accounts() {
        return jdbc.queryForList("SELECT acct_id || ' ' || acct_curr_bal || ' ' || acct_curr_cyc_credit || ' '"
                + " || acct_curr_cyc_debit FROM carddemo.account ORDER BY acct_id", String.class);
    }

    private String runStatus() {
        return jdbc.queryForObject("SELECT status FROM carddemo.interest_run", String.class);
    }

    @Test
    void intR07_transactionsAndUpdateOfAnAccountCommitTogether() throws Exception {
        assertEquals(BatchStatus.COMPLETED, jobLauncher.run(interestCalcJob, parametersOfANewRun(temp.resolve("actual")))
                .getStatus());

        assertEquals(ALL_TRANSACTIONS, transactions());
        assertEquals(ALL_ACCOUNTS, accounts());
        assertEquals(WHOLE_RUN_LOG, Fixtures.lines(temp.resolve("actual").resolve(RUN_LOG)));
        assertEquals(List.of(FROZEN_TIMESTAMP), jdbc.queryForList(
                "SELECT DISTINCT tran_orig_ts FROM carddemo.interest_transaction WHERE tran_orig_ts = tran_proc_ts",
                String.class));
        assertEquals("CALCULATED", runStatus());
        // 10.00 + 37.00 + 10.00 + 7.50 + 15.42 + 13.61 = 93.53; four accounts updated
        assertEquals(new BigDecimal("93.53"), jdbc.queryForObject(
                "SELECT written_tran_amt_sum FROM carddemo.interest_run WHERE written_account_count = 4", BigDecimal.class));
    }

    @Test
    void intR07_forcedFailureBetweenInsertsAndUpdateLeavesNeither(CapturedOutput applicationLog) throws Exception {
        refuseTheUpdateOfAccount(2009);

        JobExecution failed = jobLauncher.run(interestCalcJob, parametersOfANewRun(temp.resolve("actual")));

        // the step ends FAILED and names the account in its exit description
        assertEquals(BatchStatus.FAILED, failed.getStatus());
        StepExecution step = failed.getStepExecutions().iterator().next();
        assertEquals(BatchStatus.FAILED, step.getStatus());
        assertTrue(step.getExitStatus().getExitDescription().contains("calcInterestStep stopped at account 00000002009"),
                step.getExitStatus().getExitDescription());
        // chunk 1 stays committed; of chunk 2 nothing stays: neither the three inserted transactions
        // (two of 2007, one of 2009) nor the update of 2007 that ran before the failure
        assertEquals(ALL_TRANSACTIONS.subList(0, 2), transactions());
        assertEquals(List.of(
                "2001 1010.00 0.00 0.00",
                "2002 2437.00 0.00 0.00",
                "2007 1500.00 1500.00 0.00",
                "2009 1000.33 1000.33 0.00",
                "2010 777.77 777.77 0.00"), accounts());
        assertEquals("FAILED", runStatus());
        // standards S6.5: no account id in the application log
        assertFalse(applicationLog.getAll().contains("00000002009"), "account id in the application log");
    }

    @Test
    void rel11_restartEndsWithTheRowsOfAnUninterruptedRun() throws Exception {
        Path output = temp.resolve("actual");
        JobParameters parameters = parametersOfANewRun(output);
        refuseTheUpdateOfAccount(2009);
        assertEquals(BatchStatus.FAILED, jobLauncher.run(interestCalcJob, parameters).getStatus());
        // the run log of the failed execution holds the lines of the chunk that was rolled back
        assertEquals(WHOLE_RUN_LOG.subList(0, 6), Fixtures.lines(output.resolve(RUN_LOG)));
        jdbc.execute("DROP TRIGGER test_forced_failure ON carddemo.account");

        JobExecution restarted = jobLauncher.run(interestCalcJob, parameters);

        assertEquals(BatchStatus.COMPLETED, restarted.getStatus());
        // same transaction ids and seq_no as an uninterrupted run; accounts 2001 and 2002 were not charged twice
        assertEquals(ALL_TRANSACTIONS, transactions());
        assertEquals(ALL_ACCOUNTS, accounts());
        // no line of the run log is missing or written twice
        assertEquals(WHOLE_RUN_LOG, Fixtures.lines(output.resolve(RUN_LOG)));
        // the restarted execution read the three accounts the first one had not committed
        assertEquals(3, restarted.getStepExecutions().iterator().next().getReadCount());
        assertEquals("CALCULATED", runStatus());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_run", Integer.class));
        assertEquals(4L, jdbc.queryForObject("SELECT written_account_count FROM carddemo.interest_run", Long.class));
    }

    @Test
    void intR08_failureInTheCalculationNamesTheAccountAndKeepsCommittedChunks() throws Exception {
        // the balances of 2007 have no account row: the calculation stops there (CBACT04C:373-390)
        jdbc.update("DELETE FROM carddemo.account WHERE acct_id = 2007");

        JobExecution failed = jobLauncher.run(interestCalcJob, parametersOfANewRun(temp.resolve("actual")));

        assertEquals(BatchStatus.FAILED, failed.getStatus());
        String exitDescription = failed.getStepExecutions().iterator().next().getExitStatus().getExitDescription();
        assertTrue(exitDescription.contains("calcInterestStep stopped at account 00000002007"), exitDescription);
        assertTrue(exitDescription.contains("ACCOUNT_MISSING (INT-R08)"), exitDescription);
        assertEquals(ALL_TRANSACTIONS.subList(0, 2), transactions());
        assertEquals(List.of("2001 1010.00 0.00 0.00", "2002 2437.00 0.00 0.00", "2009 1000.33 1000.33 0.00",
                "2010 777.77 777.77 0.00"), accounts());
        assertEquals("FAILED", runStatus());
    }

    @Test
    void rel11_failureInTheFirstChunkRestartsFromTheFirstAccount() throws Exception {
        Path output = temp.resolve("actual");
        JobParameters parameters = parametersOfANewRun(output);
        refuseTheUpdateOfAccount(2001);
        assertEquals(BatchStatus.FAILED, jobLauncher.run(interestCalcJob, parameters).getStatus());
        assertEquals(List.of(), transactions());
        jdbc.execute("DROP TRIGGER test_forced_failure ON carddemo.account");

        assertEquals(BatchStatus.COMPLETED, jobLauncher.run(interestCalcJob, parameters).getStatus());

        assertEquals(ALL_TRANSACTIONS, transactions());
        assertEquals(ALL_ACCOUNTS, accounts());
        assertEquals(WHOLE_RUN_LOG, Fixtures.lines(output.resolve(RUN_LOG)));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_run", Integer.class));
    }
}
