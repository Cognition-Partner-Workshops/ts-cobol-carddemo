package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.jdbc.InterestTables;

/**
 * INT-H03 with the switch at its other value, {@code interest.policy.multiCard=FIRST_BY_CARD_NUM},
 * through the job on the tables (PostgreSQL 15 in a container, standards S7.3). The default, STOP,
 * is in {@link ErrorPathsTablesIT}. No recorded legacy run exists for an account with two cards.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.calc.adapters=jdbc",
        "interest.policy.multiCard=FIRST_BY_CARD_NUM",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class MultiCardFirstByCardNumIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = InterestTables.postgres();

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job interestCalcJob;
    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path temp;

    @Test
    void intH03_firstByCardNumberUsesLowerCard() throws Exception {
        // i01-baseline: account 2001 has card 4000000000002001, account 2002 card 4000000000002002.
        // 2001 gets a second card below its own, 2002 a second card above its own.
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));
        jdbc.update("""
                INSERT INTO carddemo.card_xref
                SELECT '4000000000001001', xref_cust_id, xref_acct_id, xref_filler
                  FROM carddemo.card_xref WHERE xref_acct_id = 2001""");
        jdbc.update("""
                INSERT INTO carddemo.card_xref
                SELECT '4000000000009002', xref_cust_id, xref_acct_id, xref_filler
                  FROM carddemo.card_xref WHERE xref_acct_id = 2002""");

        JobExecution execution = jobLauncher.run(interestCalcJob, new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, "not-read-by-the-table-step")
                .addString(InterestJobParameters.OUTPUT_DIR, temp.resolve("actual").toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters());

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        // the lower card number of each account; amounts by hand: 1000.00 * 12.00 / 1200 and 2400.00 * 18.50 / 1200
        assertEquals(List.of("2022071800000001 10.00 4000000000001001", "2022071800000002 37.00 4000000000002002"),
                jdbc.queryForList("SELECT tran_id || ' ' || tran_amt || ' ' || tran_card_num"
                        + " FROM carddemo.interest_transaction ORDER BY seq_no", String.class));
    }
}
