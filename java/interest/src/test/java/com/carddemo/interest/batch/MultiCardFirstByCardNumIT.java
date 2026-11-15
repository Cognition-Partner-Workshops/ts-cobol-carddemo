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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.CardXrefRecord;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.domain.HoleDecisions;
import com.carddemo.interest.jdbc.calc.ReplicaTables;

/**
 * INT-H03 with multiCard=FIRST_BY_CARD_NUM on the tables, PostgreSQL 15: the lower card number of
 * an account with two cards goes on its interest transaction. The decision is given to the step
 * as the HoleDecisions bean: interest.policy.multiCard is not read from the configuration yet
 * (InterestPolicy does not hold the switch).
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.calc.store=jdbc",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class MultiCardFirstByCardNumIT {

    @TestConfiguration
    static class FirstByCardNum {

        @Bean
        @Primary
        HoleDecisions firstByCardNum() {
            return new HoleDecisions(HoleDecisions.MultiCard.FIRST_BY_CARD_NUM, HoleDecisions.Overflow.STOP);
        }
    }

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

    @Test
    void intH03_lowerCardNumberWhenSwitched() throws Exception {
        // i01-baseline: 2001 has card 4000000000002001; a second, lower card number is added
        ReplicaTables.load(jdbc, Fixtures.input("i01-baseline"));
        ReplicaTables.insert(jdbc, ReplicaTables.CARD_XREF, RecordValues.blank(CardXrefRecord.LAYOUT)
                .with("XREF-CARD-NUM", "4000000000001001").with("XREF-CUST-ID", 20012L).with("XREF-ACCT-ID", 2001L));

        JobExecution execution = jobLauncher.run(interestCalcJob, new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, temp.toString())
                .addString(InterestJobParameters.OUTPUT_DIR, temp.resolve("actual").toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters());

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        // 1000.00 x 12.00 / 1200 = 10.00 and 2400.00 x 18.50 / 1200 = 37.00, as recorded for i01-baseline
        assertEquals(List.of(
                "2022071800000001 Int. for a/c 00000002001 10.00 4000000000001001",
                "2022071800000002 Int. for a/c 00000002002 37.00 4000000000002002"),
                jdbc.queryForList("SELECT tran_id || ' ' || rtrim(tran_desc) || ' ' || tran_amt || ' ' || tran_card_num"
                        + " FROM carddemo.interest_transaction ORDER BY seq_no", String.class));
        assertEquals(List.of("CALCULATED"), jdbc.queryForList("SELECT status FROM carddemo.interest_run", String.class));
    }
}
