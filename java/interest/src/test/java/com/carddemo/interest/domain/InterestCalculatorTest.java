package com.carddemo.interest.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.domain.AccountBalances.BalanceRead;
import com.carddemo.interest.policy.InterestPolicy;
import com.carddemo.interest.policy.InterestPolicy.IdPrefix;
import com.carddemo.interest.policy.InterestPolicy.Rounding;
import com.carddemo.interest.policy.InterestPolicy.TimestampSource;
import com.carddemo.interest.port.AccountStore;
import com.carddemo.interest.port.RunLog;

/**
 * The calculation of one account against in-memory ports. Data are those of i01-baseline unless a
 * test says otherwise; expected values are the recorded ones or worked by hand in the comment.
 */
class InterestCalculatorTest {

    private static final String PARM_DATE = "2022071800";
    private static final Clock FROZEN = Clock.fixed(Instant.parse("2022-07-05T10:00:00Z"), ZoneOffset.UTC);

    private final Map<Long, Account> accounts = new HashMap<>(Map.of(
            2001L, new Account(2001L, new BigDecimal("1000.00"), "GOLD", "Y"),
            2002L, new Account(2002L, new BigDecimal("2400.00"), "STANDARD", "Y")));
    private final Map<Long, List<String>> cards = new HashMap<>(Map.of(
            2001L, List.of("4000000000002001"),
            2002L, List.of("4000000000002002")));
    private final Map<String, BigDecimal> rates = new HashMap<>(Map.of(
            "GOLD/01/1", new BigDecimal("12.00"),
            "GOLD/01/2", new BigDecimal("18.00"),
            "GOLD/02/1", new BigDecimal("0.00"),
            "STANDARD/01/1", new BigDecimal("18.50")));
    private final List<String> runLogLines = new ArrayList<>();
    private final TransactionCounter counter = new TransactionCounter();

    private InterestCalculator calculator(InterestPolicy policy, Clock clock) {
        AccountStore accountStore = new AccountStore() {
            @Override
            public Optional<Account> find(long accountId) {
                return Optional.ofNullable(accounts.get(accountId));
            }

            @Override
            public void postInterest(long accountId, BigDecimal newBalance) {
                throw new AssertionError("the calculation returns the update; it does not write it");
            }
        };
        RunLog runLog = new RunLog() {
            @Override
            public void line(String text) {
                runLogLines.add(text);
            }

            @Override
            public void echo(String recordImage) {
                runLogLines.add(recordImage);
            }
        };
        return new InterestCalculator(accountStore,
                accountId -> cards.getOrDefault(accountId, List.of()),
                (groupId, typeCode, categoryCode) ->
                        Optional.ofNullable(rates.get(groupId + "/" + typeCode + "/" + categoryCode)),
                runLog, policy, clock, counter, PARM_DATE);
    }

    private InterestCalculator calculator() {
        return calculator(InterestPolicy.legacy(), FROZEN);
    }

    private static BalanceRead balance(long accountId, String typeCode, int categoryCode, String amount) {
        return new BalanceRead(new CategoryBalance(accountId, typeCode, categoryCode, new BigDecimal(amount)),
                "image of " + accountId + "/" + typeCode + "/" + categoryCode);
    }

    private static AccountBalances account2001(boolean endsInput) {
        return new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "1000.00")), endsInput);
    }

    private static AccountBalances account2002(boolean endsInput) {
        return new AccountBalances(2002L, List.of(balance(2002L, "01", 1, "2400.00")), endsInput);
    }

    @Test
    void intR01_everyBalanceOfTheAccountGetsOneDecision() {
        // i05-multi-category data: 1000.00 at 12.00 % = 10.00 and 500.00 at 18.00 % = 7.50
        AccountBalances item = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "1000.00"), balance(2001L, "01", 2, "500.00")), false);

        AccountInterest result = calculator().calculate(item);

        assertEquals(List.of(new BigDecimal("10.00"), new BigDecimal("7.50")),
                result.transactions().stream().map(InterestTransaction::amount).toList());
        assertEquals(List.of("image of 2001/01/1", "image of 2001/01/2"), runLogLines);
    }

    @Test
    void intR02_rateByGroupTypeCategory() {
        // i01-baseline: account 2001 is GOLD (12.00 %), account 2002 is STANDARD (18.50 %)
        assertEquals(new BigDecimal("10.00"), calculator().calculate(account2001(false)).transactions().get(0).amount());
        assertEquals(new BigDecimal("37.00"), calculator().calculate(account2002(true)).transactions().get(0).amount());
    }

    @Test
    void intR03_fallsBackToDefault() {
        // i04-default-group-fallback data: group PLATINUM has no rate; DEFAULT 21.00 %: 1000.00 * 21.00 / 1200 = 17.50
        accounts.put(2001L, new Account(2001L, new BigDecimal("1000.00"), "PLATINUM", "Y"));
        rates.put("DEFAULT/01/1", new BigDecimal("21.00"));

        AccountInterest result = calculator().calculate(account2001(false));

        assertEquals(new BigDecimal("17.50"), result.transactions().get(0).amount());
        // CBACT04C:418-419: the two lines follow the record echo
        assertEquals(List.of("image of 2001/01/1", "DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE"),
                runLogLines);
    }

    @Test
    void intR03_noStaleRateAfterAFailedLookup() {
        // behaviour x: the balance after a fallback gets its own group's rate again, and the one before does not leak.
        // GOLD has 01/1 at 12.00 % but no 01/3; DEFAULT has 01/3 at 21.00 %: 10.00, then 600.00 * 21.00 / 1200 = 10.50,
        // then 500.00 * 18.00 / 1200 = 7.50
        rates.put("DEFAULT/01/3", new BigDecimal("21.00"));
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "1000.00"),
                balance(2001L, "01", 3, "600.00"), balance(2001L, "01", 2, "500.00")), false);

        assertEquals(List.of(new BigDecimal("10.00"), new BigDecimal("10.50"), new BigDecimal("7.50")),
                calculator().calculate(item).transactions().stream().map(InterestTransaction::amount).toList());
    }

    @Test
    void intR08_defaultRateMissingStops() {
        // neither SILVER nor DEFAULT has a rate for 01/1 (CBACT04C:443-460); the legacy messages are ticket 08
        accounts.put(2001L, new Account(2001L, new BigDecimal("1000.00"), "SILVER", "Y"));

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.DEFAULT_RATE_MISSING, stop.reason());
        assertEquals("INT-R08", stop.reason().ruleId());
    }

    @Test
    void intR04_zeroRateWritesNoTransaction() {
        // i03-zero-rate-skipped data: 1000.00 at 12.00 % = 10.00, then a payment balance at 0.00 %: nothing
        AccountBalances item = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "1000.00"), balance(2001L, "02", 1, "-300.00")), false);

        AccountInterest result = calculator().calculate(item);

        assertEquals(1, result.transactions().size());
        assertEquals(new BigDecimal("10.00"), result.transactions().get(0).amount());
        // 1000.00 + 10.00: the zero-rate balance adds nothing to the account total
        assertEquals(Optional.of(new BigDecimal("1010.00")), result.newBalance());
        // ... and uses no transaction id: the next transaction is 000002
        assertEquals("2022071800000002", calculator().calculate(account2002(true)).transactions().get(0).id());
    }

    @Test
    void intR04_accountWithOnlyZeroRatesIsStillUpdatedAtTheBreak() {
        // CBACT04C:194-201, 350-356: the break adds a total of 0.00 and resets the cycle amounts all the same
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "02", 1, "500.00")), false);

        AccountInterest result = calculator().calculate(item);

        assertEquals(List.of(), result.transactions());
        assertEquals(Optional.of(new BigDecimal("1000.00")), result.newBalance());
    }

    @Test
    void intR06_postedAsSystemTransaction() {
        InterestTransaction transaction = calculator().calculate(account2001(false)).transactions().get(0);

        // CBACT04C:482-495: type 01, source System, description, no merchant
        assertEquals("01", transaction.typeCode());
        assertEquals("System", transaction.source());
        assertEquals("Int. for a/c 00000002001", transaction.description());
        assertEquals(0L, transaction.merchantId());
    }

    @Test
    void intR06_categoryCodeIs0005() {
        // CBACT04C:483: '05' moved to PIC 9(04) is 0005, the number 5
        assertEquals(5, calculator().calculate(account2001(false)).transactions().get(0).categoryCode());
    }

    @Test
    void intR06_cardNumberOfAccount() {
        assertEquals("4000000000002001", calculator().calculate(account2001(false)).transactions().get(0).cardNumber());
        assertEquals("4000000000002002", calculator().calculate(account2002(true)).transactions().get(0).cardNumber());
    }

    @Test
    void intR08_missingAccountStops() {
        accounts.remove(2001L);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.ACCOUNT_MISSING, stop.reason());
        // CBACT04C:193 comes before 202-203: the balance is displayed before the account is looked up
        assertEquals(List.of("image of 2001/01/1"), runLogLines);
        assertTrue(!stop.getMessage().contains("2001"), "no account id in a message for the application log");
    }

    @Test
    void intR08_missingCardStops() {
        cards.remove(2001L);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.CARD_MISSING, stop.reason());
    }

    @Test
    void intH03_twoCardsStops() {
        cards.put(2001L, List.of("4000000000002001", "4000000000009001"));

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.MULTI_CARD, stop.reason());
        assertEquals("INT-H03", stop.reason().ruleId());
    }

    @Test
    void intR07_updatesAccountAtBreak() {
        // i01-baseline: 1000.00 + 10.00 = 1010.00 for account 2001, which does not end the input
        assertEquals(Optional.of(new BigDecimal("1010.00")), calculator().calculate(account2001(false)).newBalance());
    }

    @Test
    void intR07_totalIsPerAccount() {
        // i05-multi-category data: 10.00 + 7.50 = 17.50 on 1000.00 -> 1017.50; the next account starts from zero
        AccountBalances first = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "1000.00"), balance(2001L, "01", 2, "500.00")), false);
        InterestCalculator calculator = calculator();

        assertEquals(Optional.of(new BigDecimal("1017.50")), calculator.calculate(first).newBalance());
        // 2400.00 + 37.00 = 2437.00, not 2454.50
        assertEquals(Optional.of(new BigDecimal("2437.00")), calculator.calculate(account2002(false)).newBalance());
    }

    @Test
    void intQ04_lastAccountNotUpdated() {
        // i01-baseline: account 2002 ends the input; it gets its 37.00 transaction and no update (stays 2400.00)
        AccountInterest result = calculator().calculate(account2002(true));

        assertEquals(1, result.transactions().size());
        assertEquals(new BigDecimal("37.00"), result.transactions().get(0).amount());
        assertEquals(Optional.empty(), result.newBalance());
    }

    @Test
    void intQ04_lastAccountUpdatedWhenSwitched() {
        InterestPolicy policy = new InterestPolicy(Rounding.TRUNCATE, IdPrefix.PARM, TimestampSource.RUN_CLOCK, true, null, null, null);

        // 2400.00 + 37.00 = 2437.00
        assertEquals(Optional.of(new BigDecimal("2437.00")),
                calculator(policy, FROZEN).calculate(account2002(true)).newBalance());
    }

    @Test
    void intQ02_idIsParmPlusCounter() {
        // i01-baseline TRAN-ID of the first record: PARM 2022071800 + 000001; the clock says 2022-07-05
        assertEquals("2022071800000001", calculator().calculate(account2001(false)).transactions().get(0).id());
    }

    @Test
    void intQ02_counterRunsAcrossAccounts() {
        // i01-baseline: 2022071800000001 for account 2001, 2022071800000002 for account 2002
        InterestCalculator calculator = calculator();

        assertEquals("2022071800000001", calculator.calculate(account2001(false)).transactions().get(0).id());
        assertEquals("2022071800000002", calculator.calculate(account2002(true)).transactions().get(0).id());
    }

    @Test
    void intQ02_runDatePrefixWhenSwitched() {
        InterestPolicy policy = new InterestPolicy(Rounding.TRUNCATE, IdPrefix.RUN_DATE, TimestampSource.RUN_CLOCK, false, null, null, null);

        // date of the frozen clock, 2022-07-05, followed by 00, then the counter
        assertEquals("2022070500000001",
                calculator(policy, FROZEN).calculate(account2001(false)).transactions().get(0).id());
    }

    @Test
    void intQ03_timestampsFromRunClock() {
        // i01-baseline: PARM says 2022-07-18, both timestamps say the run clock 2022-07-05 10:00:00.00
        InterestTransaction transaction = calculator().calculate(account2001(false)).transactions().get(0);

        assertEquals("2022-07-05-10.00.00.000000", transaction.originTimestamp());
        assertEquals("2022-07-05-10.00.00.000000", transaction.processedTimestamp());
    }

    @Test
    void intQ03_clockIsReadOncePerTransaction() {
        // a clock that advances 1.5 s on every reading: the two transactions differ, the two stamps of one do not
        Clock ticking = new Clock() {
            private Instant next = Instant.parse("2022-07-05T10:00:00Z");

            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                Instant now = next;
                next = next.plus(Duration.ofMillis(1500));
                return now;
            }
        };
        AccountBalances item = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "1000.00"), balance(2001L, "01", 2, "500.00")), false);

        List<InterestTransaction> transactions = calculator(InterestPolicy.legacy(), ticking).calculate(item).transactions();

        assertEquals("2022-07-05-10.00.00.000000", transactions.get(0).originTimestamp());
        assertEquals("2022-07-05-10.00.00.000000", transactions.get(0).processedTimestamp());
        assertEquals("2022-07-05-10.00.01.500000", transactions.get(1).originTimestamp());
        assertEquals("2022-07-05-10.00.01.500000", transactions.get(1).processedTimestamp());
    }

    @Test
    void intQ03_defaultIsRunClock() {
        assertEquals(TimestampSource.RUN_CLOCK, new InterestPolicy(null, null, null, false, null, null, null).timestampSource());
        assertEquals(false, InterestPolicy.legacy().intQ03StampsCycleDate());
    }
}
