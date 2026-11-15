package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.jdbc.calc.ReplicaTables;

/**
 * interestCalcJob with its three steps (profile tables) when calcInterestStep stops after a
 * committed chunk, PostgreSQL 15 with the schema of FI-4. Input: the accounts, cards and balances
 * of i01-baseline, i05-multi-category and i07-nontie-rounding together, as in CalcInterestRestartIT;
 * with two accounts per chunk the forced failure at account 2009 comes after the commit of 2001
 * and 2002.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.calc.commitInterval=2",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@ActiveProfiles("tables")
@Testcontainers
class InterestJobRestartIT {

    private static final List<String> ALL_TRANSACTIONS = List.of(
            "1 2022071800000001 Int. for a/c 00000002001 10.00 4000000000002001",
            "2 2022071800000002 Int. for a/c 00000002002 37.00 4000000000002002",
            "3 2022071800000003 Int. for a/c 00000002007 10.00 4000000000002007",
            "4 2022071800000004 Int. for a/c 00000002007 7.50 4000000000002007",
            "5 2022071800000005 Int. for a/c 00000002009 15.42 4000000000002009",
            "6 2022071800000006 Int. for a/c 00000002010 13.61 4000000000002010");
    private static final List<String> ALL_ACCOUNTS = List.of(
            "2001 1010.00 0.00 0.00",
            "2002 2437.00 0.00 0.00",
            "2007 1517.50 0.00 0.00",
            "2009 1015.75 0.00 0.00",
            "2010 777.77 777.77 0.00");

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

    private Path input;

    @BeforeEach
    void fiveAccounts() throws IOException {
        input = Files.createDirectories(temp.resolve("input"));
        for (String file : List.of("AWS.M2.CARDDEMO.ACCTDATA.PS.txt", "AWS.M2.CARDDEMO.CARDXREF.PS.txt",
                "AWS.M2.CARDDEMO.TCATBALF.PS.txt")) {
            for (String caseName : List.of("i01-baseline", "i05-multi-category", "i07-nontie-rounding")) {
                Files.write(input.resolve(file), Files.readAllBytes(Fixtures.input(caseName).resolve(file)),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        }
        Files.copy(Fixtures.input("i01-baseline").resolve("AWS.M2.CARDDEMO.DISCGRP.PS.txt"),
                input.resolve("AWS.M2.CARDDEMO.DISCGRP.PS.txt"));
        jdbc.execute("DROP TRIGGER IF EXISTS test_forced_failure ON carddemo.account");
    }

    private JobParameters parametersOfANewRun(Path outputDir) {
        return new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, input.toString())
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

    private List<String> stepsOf(JobExecution execution) {
        return jdbc.queryForList("SELECT step_name || ':' || status FROM batch.batch_step_execution"
                + " WHERE job_execution_id = ? ORDER BY step_execution_id", String.class, execution.getId());
    }

    private List<String> transactionsOf(long run) {
        return jdbc.queryForList("SELECT seq_no || ' ' || tran_id || ' ' || rtrim(tran_desc) || ' ' || tran_amt || ' '"
                + " || tran_card_num FROM carddemo.interest_transaction WHERE run_id = ? ORDER BY seq_no", String.class, run);
    }

    private List<String> accounts() {
        return jdbc.queryForList("SELECT acct_id || ' ' || acct_curr_bal || ' ' || acct_curr_cyc_credit || ' '"
                + " || acct_curr_cyc_debit FROM carddemo.account ORDER BY acct_id", String.class);
    }

    /**
     * What a restart of the three steps does today, a finding of ticket 09 for REL-11 and not the
     * target behaviour. architecture.md section 6 repeats loadExtractsStep on a restart and lets
     * calcInterestStep continue after its last committed account. The repeated load opens a new
     * interest_run row and replaces the account table, so the updates the first execution committed
     * are gone, while the calculation continues the first run. The export is handed the new row,
     * which is LOADED, and refuses it: the restarted job ends FAILED and exports nothing.
     */
    @Test
    void rel11_restartOfTheThreeStepsExportsNothing() throws Exception {
        Path output = temp.resolve("actual");
        JobParameters parameters = parametersOfANewRun(output);
        refuseTheUpdateOfAccount(2009);
        JobExecution first = jobLauncher.run(interestCalcJob, parameters);
        assertEquals(BatchStatus.FAILED, first.getStatus());
        assertEquals(List.of("loadExtractsStep:COMPLETED", "calcInterestStep:FAILED"), stepsOf(first));
        jdbc.execute("DROP TRIGGER test_forced_failure ON carddemo.account");

        JobExecution restarted = jobLauncher.run(interestCalcJob, parameters);

        assertEquals(BatchStatus.FAILED, restarted.getStatus());
        assertEquals(List.of("loadExtractsStep:COMPLETED", "calcInterestStep:COMPLETED", "exportDatasetsStep:FAILED"),
                stepsOf(restarted));
        assertFalse(Files.exists(output.resolve("datasets")), "no dataset of a job that did not complete");
        assertFalse(Files.exists(output.resolve("rc.json")), "no rc.json of a job that did not complete");
    }

    /**
     * The alternative architecture.md section 6 keeps open: after a stopped run, launch again from
     * the start with a new runId. The load replaces the tables, so the result is that of an
     * uninterrupted run (the amounts are those of CalcInterestRestartIT, from the recorded cases).
     */
    @Test
    void rel11_newLaunchAfterAStopGivesTheRowsOfAnUninterruptedRun() throws Exception {
        refuseTheUpdateOfAccount(2009);
        assertEquals(BatchStatus.FAILED,
                jobLauncher.run(interestCalcJob, parametersOfANewRun(temp.resolve("stopped"))).getStatus());
        jdbc.execute("DROP TRIGGER test_forced_failure ON carddemo.account");

        JobExecution again = jobLauncher.run(interestCalcJob, parametersOfANewRun(temp.resolve("actual")));

        assertEquals(BatchStatus.COMPLETED, again.getStatus());
        long run = again.getExecutionContext().getLong("interestRunId");
        assertEquals(ALL_TRANSACTIONS, transactionsOf(run));
        assertEquals(ALL_ACCOUNTS, accounts());
        assertEquals("EXPORTED", jdbc.queryForObject("SELECT status FROM carddemo.interest_run WHERE run_id = ?",
                String.class, run));
        assertEquals(6, Fixtures.lines(temp.resolve("actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt")).size());
    }
}
