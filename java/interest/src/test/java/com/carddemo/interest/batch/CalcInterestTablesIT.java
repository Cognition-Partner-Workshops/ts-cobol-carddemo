package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.jdbc.InterestTables;
import com.carddemo.interest.jdbc.JdbcBalanceSource;

/**
 * calcInterestStep on the tables of schema carddemo (ticket 06), on PostgreSQL 15 in a container
 * with db/postgres/01 and 02 applied (standards S7.3). The fixture input is put into the replica
 * tables, the job runs with {@code interest.calc.adapters=jdbc}, and the rows it leaves are
 * compared, as records, with the recorded legacy run (standards S7.4).
 *
 * <p>Each case also leaves its result under {@code target/parity-tables/<case>/actual} in the
 * recorded format, for tools/parity/compare.py.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.calc.adapters=jdbc",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class CalcInterestTablesIT {

    static final Path PARITY_OUTPUT = Path.of("target", "parity-tables");

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

    @BeforeEach
    void auditWrites() {
        InterestTables.clear(jdbc);
        InterestTables.installWriteAudit(jdbc);
    }

    @AfterEach
    void removeAudit() {
        InterestTables.removeWriteAudit(jdbc);
    }

    private JobExecution launch(Path outputDir) throws Exception {
        return jobLauncher.run(interestCalcJob, new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, "not-read-by-the-table-step")
                .addString(InterestJobParameters.OUTPUT_DIR, outputDir.toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters());
    }

    private static long interestRunOf(JobExecution execution) {
        return execution.getExecutionContext().getLong(CalcInterestTableRun.INTEREST_RUN_ID);
    }

    /** Loads the case, runs the job on the tables and returns the run's row id; the run log is under outputDir. */
    private long runCase(String caseName, Path outputDir) throws Exception {
        InterestTables.load(jdbc, Fixtures.input(caseName));
        InterestTables.installWriteAudit(jdbc);
        JobExecution execution = launch(outputDir);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus(), caseName);
        return interestRunOf(execution);
    }

    private static String expected(String caseName, String file) {
        return Fixtures.text(Fixtures.expected(caseName).resolve(file));
    }

    @Test
    void intR01_i05TransactionsInSeqNoOrderAndAccountEqualTheRecordedRun() throws Exception {
        long run = runCase("i05-multi-category", temp.resolve("actual"));

        // acceptance: the rows of the run in seq_no order are the recorded SYSTRAN records, the account the recorded ACCTDATA
        assertEquals(Fixtures.lines(Fixtures.expected("i05-multi-category").resolve(InterestTables.TRANSACTIONS)),
                InterestTables.transactionLines(jdbc, run));
        assertEquals(Fixtures.lines(Fixtures.expected("i05-multi-category").resolve(InterestTables.ACCOUNTS)),
                InterestTables.accountLines(jdbc));
        assertEquals(List.of(1, 2), jdbc.queryForList(
                "SELECT seq_no FROM carddemo.interest_transaction WHERE run_id = ? ORDER BY seq_no", Integer.class, run));
        // by hand, from the case: 1000.00 * 12.00 / 1200 = 10.00 and 500.00 * 18.00 / 1200 = 7.50, ids 2022071800 + counter
        assertEquals(List.of("2022071800000001 10.00", "2022071800000002 7.50"), jdbc.queryForList(
                "SELECT tran_id || ' ' || tran_amt FROM carddemo.interest_transaction WHERE run_id = ? ORDER BY seq_no",
                String.class, run));
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped",
            "i04-default-group-fallback", "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void throughTheTables_everyCaseEqualsTheRecordedLegacyRun(String caseName) throws Exception {
        // the ticket names i01, i05 and i07; the other four are run as well and reported
        Path output = PARITY_OUTPUT.resolve(caseName).resolve("actual");
        deleteTree(PARITY_OUTPUT.resolve(caseName));

        long run = runCase(caseName, output);
        InterestTables.writeActual(jdbc, run, output);

        assertEquals(expected(caseName, InterestTables.TRANSACTIONS), InterestTables.text(InterestTables.transactionLines(jdbc, run)),
                caseName + " interest_transaction");
        assertEquals(expected(caseName, InterestTables.ACCOUNTS), InterestTables.text(InterestTables.accountLines(jdbc)),
                caseName + " account");
        assertEquals(expected(caseName, InterestTables.RUN_LOG), Fixtures.text(output.resolve(InterestTables.RUN_LOG)),
                caseName + " run log");
        assertEquals(expected(caseName, InterestTables.RETURN_CODES), Fixtures.text(output.resolve(InterestTables.RETURN_CODES)),
                caseName + " rc.json written by the test");
    }

    @Test
    void sysout_matchesRecorded() throws Exception {
        // INT-T02: start line, each balance record as read with its trailing spaces, end line
        Path output = temp.resolve("actual");
        runCase("i01-baseline", output);

        assertEquals(expected("i01-baseline", InterestTables.RUN_LOG), Fixtures.text(output.resolve(InterestTables.RUN_LOG)));
        assertEquals("000000020010100010000010000{                      ",
                Fixtures.lines(output.resolve(InterestTables.RUN_LOG)).get(1));
        // the work file of the step is gone and the step wrote nothing else: the datasets are the exporter's
        try (var files = Files.walk(temp)) {
            assertEquals(List.of("actual/sysout/INTCALC.STEP15.txt"),
                    files.filter(Files::isRegularFile).map(file -> temp.relativize(file).toString()).toList());
        }
    }

    @Test
    void intR03_i04LogsTheFallbackToTheDefaultGroupFromTheTables() throws Exception {
        Path output = temp.resolve("actual");
        runCase("i04-default-group-fallback", output);

        assertEquals(List.of("DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE"),
                Fixtures.lines(output.resolve(InterestTables.RUN_LOG)).subList(2, 4));
    }

    @Test
    void intR01_readsInKeyOrder() {
        // E6: account, then type code in byte order (digits before upper case before lower case), then category.
        // Inserted out of order; PostgreSQL's default collation would put 'a1' before 'B1'.
        InterestTables.insertBalances(jdbc, List.of(
                "00000000002a100010000000100{",
                "000000000020100020000000100{",
                "00000000002B100010000000100{",
                "000000000020100010000000100{",
                "000000000010100010000000100{",
                "000000000029900010000000100{"));

        JdbcBalanceSource source = new JdbcBalanceSource(jdbc, CodePage.fixture());
        List<String> keys = new ArrayList<>();
        for (Optional<AccountBalances> item = source.nextAccount(); item.isPresent(); item = source.nextAccount()) {
            item.get().balances().forEach(read -> keys.add(read.recordImage().substring(0, 17)));
        }

        assertEquals(List.of("00000000001010001", "00000000002010001", "00000000002010002", "00000000002990001",
                "00000000002B10001", "00000000002a10001"), keys);
    }

    /** Counts the queries the reader issues. */
    private static final class CountingJdbc extends JdbcTemplate {
        private int queries;

        private CountingJdbc(JdbcTemplate jdbc) {
            super(jdbc.getDataSource());
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            queries++;
            return super.query(sql, rowMapper, args);
        }
    }

    @Test
    void intQ04_lastAccountIsRecognisedWithoutAQueryPerAccount() {
        // five accounts, seven balances: i01 (2001, 2002), i05 (2007, two balances), i07 (2009, 2010) and one more of 2010
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"), Fixtures.input("i05-multi-category"),
                Fixtures.input("i07-nontie-rounding"));
        InterestTables.insertBalances(jdbc, List.of("000000020100100020000007777G"));

        CountingJdbc onePage = new CountingJdbc(jdbc);
        assertEquals(List.of("2001:1:false", "2002:1:false", "2007:2:false", "2009:1:false", "2010:2:true"),
                accountsOf(new JdbcBalanceSource(onePage, CodePage.fixture())));
        // seven rows fit one page: one query for the whole input, the end of the input included
        assertEquals(1, onePage.queries);

        CountingJdbc pagesOfThree = new CountingJdbc(jdbc);
        assertEquals(List.of("2001:1:false", "2002:1:false", "2007:2:false", "2009:1:false", "2010:2:true"),
                accountsOf(new JdbcBalanceSource(pagesOfThree, CodePage.fixture(), null, 3)));
        // pages of 3, 3 and 1 rows: three queries for five accounts, also where a page ends inside account 2007
        assertEquals(3, pagesOfThree.queries);

        CountingJdbc fullLastPage = new CountingJdbc(jdbc);
        assertEquals(List.of("2001:1:false", "2002:1:false", "2007:2:false", "2009:1:false", "2010:2:true"),
                accountsOf(new JdbcBalanceSource(fullLastPage, CodePage.fixture(), null, 7)));
        // a last page that is full needs one more, empty page to see the end
        assertEquals(2, fullLastPage.queries);

        // restart boundary: after account 2007 the reader starts at 2009
        assertEquals(List.of("2009:1:false", "2010:2:true"),
                accountsOf(new JdbcBalanceSource(jdbc, CodePage.fixture(), 2007L, 3)));
    }

    private static List<String> accountsOf(JdbcBalanceSource source) {
        List<String> accounts = new ArrayList<>();
        for (Optional<AccountBalances> item = source.nextAccount(); item.isPresent(); item = source.nextAccount()) {
            accounts.add(item.get().accountId() + ":" + item.get().balances().size() + ":" + item.get().endsInput());
        }
        return accounts;
    }

    @Test
    void intR07_updateTouchesOnlyTheThreeAccountColumnsAndNoReplicaTable() throws Exception {
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));
        InterestTables.installWriteAudit(jdbc);
        String accountOtherColumns = InterestTables.accountHashWithoutWrittenColumns(jdbc);
        String accountAllColumns = InterestTables.rowHash(jdbc, "carddemo.account");
        String balances = InterestTables.rowHash(jdbc, "carddemo.tran_cat_balance");
        String cards = InterestTables.rowHash(jdbc, "carddemo.card_xref");
        String rates = InterestTables.rowHash(jdbc, "carddemo.disclosure_group");

        assertEquals(BatchStatus.COMPLETED, launch(temp.resolve("actual")).getStatus());

        // row hash before and after: the three replica tables are unchanged ...
        assertEquals(balances, InterestTables.rowHash(jdbc, "carddemo.tran_cat_balance"));
        assertEquals(cards, InterestTables.rowHash(jdbc, "carddemo.card_xref"));
        assertEquals(rates, InterestTables.rowHash(jdbc, "carddemo.disclosure_group"));
        // ... account changed, but in no column other than acct_curr_bal, acct_curr_cyc_credit, acct_curr_cyc_debit
        assertNotEquals(accountAllColumns, InterestTables.rowHash(jdbc, "carddemo.account"));
        assertEquals(accountOtherColumns, InterestTables.accountHashWithoutWrittenColumns(jdbc));
        // every write the database saw on the four tables: one UPDATE, of account 2001, no other column changed.
        // Account 2002 ends the input and is not updated (INT-Q04).
        assertEquals(List.of("account UPDATE 00000002001 false"), InterestTables.writeAudit(jdbc));
        // INT-R07, by hand: 1000.00 + 10.00 = 1010.00; cycle credit and debit to zero
        assertEquals("1010.00 0.00 0.00", jdbc.queryForObject("""
                SELECT acct_curr_bal || ' ' || acct_curr_cyc_credit || ' ' || acct_curr_cyc_debit
                  FROM carddemo.account WHERE acct_id = 2001""", String.class));
    }

    @Test
    void intQ04_lastAccountNotUpdated() throws Exception {
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));
        String before = jdbc.queryForObject("SELECT md5(a::text) FROM carddemo.account a WHERE acct_id = 2002", String.class);

        JobExecution execution = launch(temp.resolve("actual"));

        // account 2002 ends the input: its row is untouched while its 37.00 transaction is written
        assertEquals(before, jdbc.queryForObject("SELECT md5(a::text) FROM carddemo.account a WHERE acct_id = 2002", String.class));
        assertEquals(new BigDecimal("37.00"), jdbc.queryForObject(
                "SELECT tran_amt FROM carddemo.interest_transaction WHERE run_id = ? AND seq_no = 2",
                BigDecimal.class, interestRunOf(execution)));
    }

    @Test
    void rc5_interestRunHoldsTheWrittenFiguresOfTheRun() throws Exception {
        long run = runCase("i01-baseline", temp.resolve("actual"));

        // by hand, i01-baseline: 10.00 + 37.00 = 47.00 in two transactions; the 37.00 of account 2002 is not added (INT-Q04)
        assertEquals("2022071800 CALCULATED 2 47.00 37.00", jdbc.queryForObject("""
                SELECT parm_date || ' ' || status || ' ' || written_transaction_count || ' ' || written_tran_amt_sum
                       || ' ' || written_interest_not_added_sum
                  FROM carddemo.interest_run WHERE run_id = ?""", String.class, run));
        assertTrue(jdbc.queryForObject("SELECT started_at IS NOT NULL FROM carddemo.interest_run WHERE run_id = ?",
                Boolean.class, run));
    }

    @Test
    void interestRun_aRowTheStepBeforeCreatedIsUsed() throws Exception {
        // the loader (ticket 05) creates the row and hands its id over in the job execution context
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));
        long loaded = jdbc.queryForObject("""
                INSERT INTO carddemo.interest_run (parm_date, status) VALUES ('2022071800', 'LOADED') RETURNING run_id""",
                Long.class);
        CalcInterestTableRun run = new CalcInterestTableRun(jdbc, com.carddemo.interest.policy.InterestPolicy.legacy(),
                FixtureRun.FROZEN);
        var stepExecution = new org.springframework.batch.core.StepExecution(InterestJobConfig.CALC_STEP_NAME,
                new JobExecution(1L, new JobParametersBuilder()
                        .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                        .addString(InterestJobParameters.RUN_ID, "handed-over")
                        .addString(InterestJobParameters.OUTPUT_DIR, temp.resolve("actual").toString())
                        .toJobParameters()));
        stepExecution.getJobExecution().getExecutionContext().putLong(CalcInterestTableRun.INTEREST_RUN_ID, loaded);

        run.beforeStep(stepExecution);
        run.open(stepExecution.getExecutionContext());
        run.close();

        assertEquals(List.of(loaded + " CALCULATING"), jdbc.queryForList(
                "SELECT run_id || ' ' || status FROM carddemo.interest_run", String.class));
    }

    @Test
    void intH02_secondRunWithTheSameParmIsNotRefused() throws Exception {
        // rerunGuard is OFF as coded: a new runId is a new run with its own rows (architecture.md section 4)
        long first = runCase("i01-baseline", temp.resolve("first"));
        long second = runCase("i01-baseline", temp.resolve("second"));

        assertNotEquals(first, second);
        assertEquals(Fixtures.lines(Fixtures.expected("i01-baseline").resolve(InterestTables.TRANSACTIONS)),
                InterestTables.transactionLines(jdbc, second));
    }

    @Test
    void intT09_emptyBalanceTableCompletesAndWritesNothing() throws Exception {
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));
        jdbc.execute("TRUNCATE carddemo.tran_cat_balance");
        InterestTables.installWriteAudit(jdbc);
        String accounts = InterestTables.rowHash(jdbc, "carddemo.account");
        Path output = temp.resolve("actual");

        JobExecution execution = launch(output);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_transaction", Integer.class));
        assertEquals(accounts, InterestTables.rowHash(jdbc, "carddemo.account"));
        assertEquals(List.of(), InterestTables.writeAudit(jdbc));
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C", "END OF EXECUTION OF PROGRAM CBACT04C"),
                Fixtures.lines(output.resolve(InterestTables.RUN_LOG)));
        assertEquals("CALCULATED 0 0.00 0.00", jdbc.queryForObject("""
                SELECT status || ' ' || written_transaction_count || ' ' || written_tran_amt_sum || ' '
                       || written_interest_not_added_sum
                  FROM carddemo.interest_run WHERE run_id = ?""", String.class, interestRunOf(execution)));
    }

    @Test
    void jobRepository_tableStepCompletesInOneChunkOfWholeAccounts() throws Exception {
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));

        JobExecution execution = launch(temp.resolve("actual"));

        // default commit interval 100: two accounts read and written in one commit
        assertEquals(List.of("calcInterestStep:COMPLETED 2/2/1"), jdbc.queryForList("""
                SELECT step_name || ':' || status || ' ' || read_count || '/' || write_count || '/' || commit_count
                  FROM batch.batch_step_execution WHERE job_execution_id = ?""", String.class, execution.getId()));
        assertFalse(jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class).iterator().hasNext());
    }

    private static void deleteTree(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
