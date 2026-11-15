package com.carddemo.interest.jdbc.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.CardXrefRecord;
import com.carddemo.interest.codec.CategoryBalanceRecord;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.domain.Account;
import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.InterestTransaction;
import com.carddemo.interest.domain.StopReason;

/** The JDBC adapters of the ports (FI-3) on PostgreSQL 15 with the schema of FI-4 (standards S7.3). */
@Testcontainers
class JdbcCalcAdaptersIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = ReplicaTables.postgres();

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void connect() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword()));
    }

    @BeforeEach
    void baseline() {
        ReplicaTables.load(jdbc, Fixtures.input("i01-baseline"));
    }

    private static void balance(long accountId, String typeCode, int categoryCode, String amount) {
        ReplicaTables.insert(jdbc, ReplicaTables.BALANCE, RecordValues.blank(CategoryBalanceRecord.LAYOUT)
                .with("TRANCAT-ACCT-ID", accountId)
                .with("TRANCAT-TYPE-CD", typeCode)
                .with("TRANCAT-CD", (long) categoryCode)
                .with("TRAN-CAT-BAL", new BigDecimal(amount)));
    }

    private static List<AccountBalances> all(JdbcBalanceSource source) {
        List<AccountBalances> accounts = new ArrayList<>();
        for (Optional<AccountBalances> next = source.nextAccount(); next.isPresent(); next = source.nextAccount()) {
            accounts.add(next.get());
        }
        return accounts;
    }

    private static List<String> keys(List<AccountBalances> accounts) {
        return accounts.stream().flatMap(account -> account.balances().stream())
                .map(read -> read.recordImage().substring(0, 17)).toList();
    }

    @Test
    void intR01_readsInKeyOrder() {
        // E6: the key order of TCATBALF is the byte order of the 17 key bytes - "B1" before "a1" (COLLATE "C"),
        // category 0002 before 0010 as numbers
        jdbc.execute("TRUNCATE carddemo.tran_cat_balance");
        balance(2002, "a1", 1, "1.00");
        balance(2002, "01", 10, "2.00");
        balance(2001, "01", 1, "3.00");
        balance(2002, "B1", 1, "4.00");
        balance(2002, "01", 2, "5.00");

        List<AccountBalances> accounts = all(new JdbcBalanceSource(jdbc, 1000, OptionalLong.empty()));

        assertEquals(List.of("00000002001010001", "00000002002010002", "00000002002010010", "00000002002B10001",
                "00000002002a10001"), keys(accounts));
        assertEquals(List.of(2001L, 2002L), accounts.stream().map(AccountBalances::accountId).toList());
    }

    @Test
    void intR01_everyPageSizeGivesTheSameAccounts() {
        // pages end inside an account, at an account break and at the end of the input
        balance(2001, "01", 2, "1.00");
        balance(2001, "02", 1, "2.00");
        balance(2003, "01", 1, "3.00");
        List<AccountBalances> expected = all(new JdbcBalanceSource(jdbc, 1000, OptionalLong.empty()));
        assertEquals(List.of(3, 1, 1), expected.stream().map(account -> account.balances().size()).toList());

        for (int pageRows = 1; pageRows <= 6; pageRows++) {
            assertEquals(expected, all(new JdbcBalanceSource(jdbc, pageRows, OptionalLong.empty())), "page of " + pageRows);
        }
    }

    @Test
    void intQ04_onlyTheAccountThatEndsTheInputIsMarked() {
        for (int pageRows : new int[] {1, 2, 1000}) {
            List<AccountBalances> accounts = all(new JdbcBalanceSource(jdbc, pageRows, OptionalLong.empty()));

            assertEquals(List.of(false, true), accounts.stream().map(AccountBalances::endsInput).toList());
        }
    }

    @Test
    void intQ04_lastAccountIsKnownWithoutAQueryPerAccount() {
        // 40 accounts in one page: one query for the page, none to find the end of the input
        jdbc.execute("TRUNCATE carddemo.tran_cat_balance");
        for (int i = 0; i < 40; i++) {
            balance(3000 + i, "01", 1, "1.00");
        }
        long before = selectsOnBalances();

        List<AccountBalances> accounts = all(new JdbcBalanceSource(jdbc, 1000, OptionalLong.empty()));

        assertEquals(40, accounts.size());
        assertTrue(accounts.get(39).endsInput());
        assertEquals(1, selectsOnBalances() - before, "sequential scans of tran_cat_balance");
    }

    /** Scans of tran_cat_balance so far, sequential and by index, from the statistics of PostgreSQL. */
    private static long selectsOnBalances() {
        jdbc.execute("SELECT pg_stat_clear_snapshot()");
        return jdbc.queryForObject("SELECT pg_stat_get_numscans('carddemo.tran_cat_balance'::regclass)"
                + " + pg_stat_get_numscans('carddemo.tran_cat_balance_pkey'::regclass)", Long.class);
    }

    @Test
    void rel11_restartReadsAfterTheLastCommittedAccount() {
        List<AccountBalances> accounts = all(new JdbcBalanceSource(jdbc, 1000, OptionalLong.of(2001)));

        assertEquals(List.of(2002L), accounts.stream().map(AccountBalances::accountId).toList());
        assertTrue(accounts.get(0).endsInput());
    }

    @Test
    void intT02_echoIsTheRecordImageWithItsFiller() {
        // E4: the FILLER of the row is echoed as stored, not as spaces
        jdbc.update("UPDATE carddemo.tran_cat_balance SET trancat_filler = ? WHERE trancat_acct_id = 2001",
                (Object) "FILLER-OF-22-BYTES-XYZ".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));

        AccountBalances first = new JdbcBalanceSource(jdbc, 1000, OptionalLong.empty()).nextAccount().orElseThrow();

        assertEquals("000000020010100010000010000{FILLER-OF-22-BYTES-XYZ", first.balances().get(0).recordImage());
        assertEquals(new BigDecimal("1000.00"), first.balances().get(0).balance().balance());
        assertEquals("01", first.balances().get(0).balance().typeCode());
    }

    @Test
    void intR07_postInterestWritesThreeColumnsOnly() {
        ReplicaTables.refuseUpdatesOfOtherAccountColumns(jdbc);
        String otherColumns = ReplicaTables.accountHashWithoutPostedColumns(jdbc);
        JdbcAccountStore accounts = new JdbcAccountStore(jdbc);
        assertEquals(new Account(2001, new BigDecimal("1000.00"), "GOLD", Optional.of(true)), accounts.find(2001).orElseThrow());

        accounts.postInterest(2001, new BigDecimal("1010.00"));

        assertEquals("1010.00 0.00 0.00", jdbc.queryForObject("SELECT acct_curr_bal || ' ' || acct_curr_cyc_credit || ' '"
                + " || acct_curr_cyc_debit FROM carddemo.account WHERE acct_id = 2001", String.class));
        assertEquals(otherColumns, ReplicaTables.accountHashWithoutPostedColumns(jdbc));
        assertEquals(Optional.empty(), accounts.find(9999));
    }

    @Test
    void intR07_balanceThatDoesNotFitThePictureIsRefusedBeforeTheUpdate() {
        // CVACT01Y/ACCT-CURR-BAL is S9(10)V99; NUMERIC(12,2) would take it, the record could not (standards S3.4)
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> new JdbcAccountStore(jdbc).postInterest(2001, new BigDecimal("10000000000.00")));

        assertEquals(StopReason.RECORD_REJECTED, stop.reason());
        assertEquals(new BigDecimal("1000.00"),
                jdbc.queryForObject("SELECT acct_curr_bal FROM carddemo.account WHERE acct_id = 2001", BigDecimal.class));
    }

    @Test
    void intR07_updateOfAMissingAccountStops() {
        assertEquals(StopReason.WRITE_OUTPUT, assertThrows(InterestRunStoppedException.class,
                () -> new JdbcAccountStore(jdbc).postInterest(9999, new BigDecimal("1.00"))).reason());
    }

    @Test
    void intR02_rateByGroupTypeAndCategory() {
        JdbcRateLookup rates = new JdbcRateLookup(jdbc);

        // fixtures/interest/i01-baseline/input DISCGRP: GOLD 01 0001 = 12.00, DEFAULT 01 0002 = 27.00
        assertEquals(Optional.of(new BigDecimal("12.00")), rates.findRate("GOLD", "01", 1));
        assertEquals(Optional.of(new BigDecimal("27.00")), rates.findRate("DEFAULT", "01", 2));
        assertEquals(Optional.empty(), rates.findRate("SILVER", "01", 1));
        assertEquals(Optional.empty(), rates.findRate("GOLD", "01", 9));
    }

    @Test
    void intH03_cardNumbersOfAnAccountInCardNumberOrder() {
        JdbcCardLookup cards = new JdbcCardLookup(jdbc);
        ReplicaTables.insert(jdbc, ReplicaTables.CARD_XREF, RecordValues.blank(CardXrefRecord.LAYOUT)
                .with("XREF-CARD-NUM", "4000000000001999").with("XREF-CUST-ID", 20012L).with("XREF-ACCT-ID", 2001L));

        assertEquals(List.of("4000000000001999", "4000000000002001"), cards.cardNumbersOf(2001));
        assertEquals(List.of("4000000000002002"), cards.cardNumbersOf(2002));
        assertEquals(List.of(), cards.cardNumbersOf(9999));
    }

    @Test
    void intR06_transactionRowHoldsTheRecordFields() {
        long runId = new CalcRunControl(jdbc).open(Fixtures.PARM_DATE);
        JdbcTransactionSink sink = new JdbcTransactionSink(jdbc, runId, 0);

        sink.write(new InterestTransaction("2022071800000001", "01", 5, "System", "Int. for a/c 00000002001",
                new BigDecimal("10.00"), 0, "4000000000002001", "2022-07-05-10.00.00.000000", "2022-07-05-10.00.00.000000"));

        // the row re-encoded is the first record of the recorded SYSTRAN of i01-baseline
        assertEquals(Fixtures.lines(Fixtures.expected("i01-baseline").resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt"))
                .get(0), ReplicaTables.fileContent(jdbc, ReplicaTables.TRANSACTION, "").stripTrailing());
        assertEquals(1, sink.lastSeqNo());
        assertEquals("LOADED", jdbc.queryForObject("SELECT status FROM carddemo.interest_run", String.class));
    }

    @Test
    void rel11_seqNoContinuesAfterTheLastCommittedOne() {
        long runId = new CalcRunControl(jdbc).open(Fixtures.PARM_DATE);
        JdbcTransactionSink sink = new JdbcTransactionSink(jdbc, runId, 41);

        sink.write(new InterestTransaction("2022071800000042", "01", 5, "System", "Int. for a/c 00000002001",
                new BigDecimal("0.00"), 0, "4000000000002001", "2022-07-05-10.00.00.000000", "2022-07-05-10.00.00.000000"));

        assertEquals(List.of(42), jdbc.queryForList("SELECT seq_no FROM carddemo.interest_transaction", Integer.class));
    }

    @Test
    void intH05_amountThatDoesNotFitThePictureIsRefusedBeforeTheInsert() {
        // CVTRA05Y/TRAN-AMT is S9(9)V99 (standards S3.4); the overflow switch itself is ticket 08
        long runId = new CalcRunControl(jdbc).open(Fixtures.PARM_DATE);

        assertThrows(InterestRunStoppedException.class, () -> new JdbcTransactionSink(jdbc, runId, 0).write(
                new InterestTransaction("2022071800000001", "01", 5, "System", "Int. for a/c 00000002001",
                        new BigDecimal("1000000000.00"), 0, "4000000000002001", "2022-07-05-10.00.00.000000",
                        "2022-07-05-10.00.00.000000")));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_transaction", Integer.class));
    }
}
