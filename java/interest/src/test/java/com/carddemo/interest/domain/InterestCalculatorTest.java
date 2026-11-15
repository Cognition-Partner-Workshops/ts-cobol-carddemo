package com.carddemo.interest.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.carddemo.interest.policy.InterestPolicy.InactiveAccounts;
import com.carddemo.interest.policy.InterestPolicy.MultiCard;
import com.carddemo.interest.policy.InterestPolicy.Overflow;
import com.carddemo.interest.policy.InterestPolicy.NegativeBalance;
import com.carddemo.interest.policy.InterestPolicy.ZeroInterest;
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
            2001L, new Account(2001L, "Y", new BigDecimal("1000.00"), "GOLD"),
            2002L, new Account(2002L, "Y", new BigDecimal("2400.00"), "STANDARD")));
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
        // i04-default-group-fallback data: group SILVER has no rate, DEFAULT/01/0001 is 21.00 %
        // 1000.00 * 21.00 / 1200 = 17.50
        accounts.put(2001L, new Account(2001L, "Y", new BigDecimal("1000.00"), "SILVER"));
        rates.put("DEFAULT/01/1", new BigDecimal("21.00"));

        AccountInterest result = calculator().calculate(account2001(false));

        assertEquals(new BigDecimal("17.50"), result.transactions().get(0).amount());
        // CBACT04C:193, 418-419: the record, then the two lines of the fallback
        assertEquals(List.of("image of 2001/01/1", "DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE"),
                runLogLines);
    }

    @Test
    void intR03_noStaleRateAfterAFailedLookup() {
        // standards section 4 row x: the first balance finds 12.00 %, the second has no group rate and
        // must take DEFAULT (21.00 %), not the 12.00 % of the balance before it: 500.00 * 21.00 / 1200 = 8.75
        rates.put("DEFAULT/01/3", new BigDecimal("21.00"));
        AccountBalances item = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "1000.00"), balance(2001L, "01", 3, "500.00")), false);

        assertEquals(List.of(new BigDecimal("10.00"), new BigDecimal("8.75")),
                calculator().calculate(item).transactions().stream().map(InterestTransaction::amount).toList());
    }

    @Test
    void intR03_ownGroupRateWinsOverDefault() {
        rates.put("DEFAULT/01/1", new BigDecimal("21.00"));

        // GOLD has 12.00 %: 10.00, and no fallback line
        assertEquals(new BigDecimal("10.00"), calculator().calculate(account2001(false)).transactions().get(0).amount());
        assertEquals(List.of("image of 2001/01/1"), runLogLines);
    }

    @Test
    void intR08_missingDefaultRateStops() {
        accounts.put(2001L, new Account(2001L, "Y", new BigDecimal("1000.00"), "SILVER"));

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.DEFAULT_RATE_MISSING, stop.reason());
        assertEquals("INT-R08", stop.reason().ruleId());
        // CBACT04C:418-419, then 455 and the status line of status 23 (635-648)
        assertEquals(List.of("image of 2001/01/1", "DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE",
                "ERROR READING DEFAULT DISCLOSURE GROUP", "FILE STATUS IS: NNNN0023"), runLogLines);
    }

    @Test
    void intR04_zeroRateWritesNoTransaction() {
        // i03-zero-rate-skipped data: 1000.00 at 12.00 % = 10.00; -300.00 at 0.00 % gives nothing
        accounts.put(2001L, new Account(2001L, "Y", new BigDecimal("700.00"), "GOLD"));
        AccountBalances item = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "1000.00"), balance(2001L, "02", 1, "-300.00")), false);

        AccountInterest result = calculator().calculate(item);

        assertEquals(List.of(new BigDecimal("10.00")),
                result.transactions().stream().map(InterestTransaction::amount).toList());
        // the zero-rate balance is still read and displayed, and the account still gets its update: 700.00 + 10.00
        assertEquals(List.of("image of 2001/01/1", "image of 2001/02/1"), runLogLines);
        assertEquals(Optional.of(new BigDecimal("710.00")), result.newBalance());
    }

    @Test
    void intR04_accountWithOnlyZeroRatesIsStillUpdatedAtItsBreak() {
        // CBACT04C:194-197: the break does not ask whether interest was computed; 1000.00 + 0.00 = 1000.00
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "02", 1, "1000.00")), false);

        AccountInterest result = calculator().calculate(item);

        assertEquals(List.of(), result.transactions());
        assertEquals(Optional.of(new BigDecimal("1000.00")), result.newBalance());
        assertEquals(0, counter.issued());
    }

    @Test
    void intQ05_negativeBalanceGivesNegativeInterest() {
        // i06-negative-balance data: -300.00 * 12.00 / 1200 = -3.00; balance -300.00 + -3.00 = -303.00
        accounts.put(2001L, new Account(2001L, "Y", new BigDecimal("-300.00"), "GOLD"));
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "-300.00")), false);

        AccountInterest result = calculator().calculate(item);

        assertEquals(new BigDecimal("-3.00"), result.transactions().get(0).amount());
        assertEquals(Optional.of(new BigDecimal("-303.00")), result.newBalance());
    }

    @Test
    void intQ05_creditBalanceSkippedWhenSwitched() {
        InterestPolicy policy = new InterestPolicy(null, null, null, false, NegativeBalance.SKIP, null, null, null, null);
        accounts.put(2001L, new Account(2001L, "Y", new BigDecimal("700.00"), "GOLD"));
        AccountBalances item = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "-300.00"), balance(2001L, "01", 2, "500.00")), false);

        AccountInterest result = calculator(policy, FROZEN).calculate(item);

        // only the debit balance is charged: 500.00 * 18.00 / 1200 = 7.50, with the first id of the run
        assertEquals(List.of(new BigDecimal("7.50")),
                result.transactions().stream().map(InterestTransaction::amount).toList());
        assertEquals("2022071800000001", result.transactions().get(0).id());
        assertEquals(Optional.of(new BigDecimal("707.50")), result.newBalance());
    }

    @Test
    void intQ06_zeroInterestIsWritten() {
        // no fixture: 0.50 * 12.00 / 1200 = 0.005, cut to 0.00, and the 0.00 transaction is written (CBACT04C:214-217)
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "0.50")), false);

        AccountInterest result = calculator().calculate(item);

        assertEquals(List.of(new BigDecimal("0.00")),
                result.transactions().stream().map(InterestTransaction::amount).toList());
        assertEquals("2022071800000001", result.transactions().get(0).id());
        assertEquals(Optional.of(new BigDecimal("1000.00")), result.newBalance());
    }

    @Test
    void intQ06_zeroBalanceAtNonZeroRateIsWritten() {
        // 0.00 * 12.00 / 1200 = 0.00: the rate is not zero, so INT-R04 does not apply and a transaction is written
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "0.00")), false);

        assertEquals(List.of(new BigDecimal("0.00")),
                calculator().calculate(item).transactions().stream().map(InterestTransaction::amount).toList());
    }

    @Test
    void intQ06_zeroInterestSkippedWhenSwitched() {
        InterestPolicy policy = new InterestPolicy(null, null, null, false, null, ZeroInterest.SKIP, null, null, null);
        // 0.50 * 12.00 / 1200 = 0.005 -> 0.00, not written; 500.00 * 18.00 / 1200 = 7.50 gets the first id
        AccountBalances item = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "0.50"), balance(2001L, "01", 2, "500.00")), false);

        AccountInterest result = calculator(policy, FROZEN).calculate(item);

        assertEquals(List.of(new BigDecimal("7.50")),
                result.transactions().stream().map(InterestTransaction::amount).toList());
        assertEquals("2022071800000001", result.transactions().get(0).id());
        assertEquals(Optional.of(new BigDecimal("1007.50")), result.newBalance());
    }

    @Test
    void intH01_noFeeTransaction() {
        // i01-baseline: one balance, one transaction, and it is the interest (category 0005), nothing else
        AccountInterest result = calculator().calculate(account2001(false));

        assertEquals(1, result.transactions().size());
        assertEquals(new BigDecimal("10.00"), result.transactions().get(0).amount());
        assertEquals(1, counter.issued());
        // the hook charges nothing whatever the balance
        assertEquals(List.of(), FeeHook.intH01FeesAfterInterestOf(
                new CategoryBalance(2001L, "01", 1, new BigDecimal("1000.00"))));
    }

    @Test
    void intH04_inactiveAccountIsCharged() {
        // no fixture: the active flag is N and the account is charged like any other: 10.00 and 1010.00
        accounts.put(2001L, new Account(2001L, "N", new BigDecimal("1000.00"), "GOLD"));

        AccountInterest result = calculator().calculate(account2001(false));

        assertEquals(List.of(new BigDecimal("10.00")),
                result.transactions().stream().map(InterestTransaction::amount).toList());
        assertEquals(Optional.of(new BigDecimal("1010.00")), result.newBalance());
    }

    @Test
    void intH04_inactiveAccountSkippedWhenSwitched() {
        InterestPolicy policy = new InterestPolicy(null, null, null, false, null, null, InactiveAccounts.SKIP, null, null);
        accounts.put(2001L, new Account(2001L, "N", new BigDecimal("1000.00"), "GOLD"));
        InterestCalculator calculator = calculator(policy, FROZEN);

        AccountInterest skipped = calculator.calculate(account2001(false));

        // no transaction and no update for the inactive account; its balance is still read and displayed
        assertEquals(List.of(), skipped.transactions());
        assertEquals(Optional.empty(), skipped.newBalance());
        assertEquals(List.of("image of 2001/01/1"), runLogLines);
        // the active account after it is charged and gets the first id of the run: 2400.00 + 37.00
        AccountInterest charged = calculator.calculate(account2002(false));
        assertEquals("2022071800000001", charged.transactions().get(0).id());
        assertEquals(Optional.of(new BigDecimal("2437.00")), charged.newBalance());
    }

    @Test
    void intH04_activeAccountChargedWhenSwitched() {
        InterestPolicy policy = new InterestPolicy(null, null, null, false, null, null, InactiveAccounts.SKIP, null, null);

        assertEquals(Optional.of(new BigDecimal("1010.00")),
                calculator(policy, FROZEN).calculate(account2001(false)).newBalance());
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
        assertEquals("INT-R08", stop.reason().ruleId());
        // CBACT04C:193 comes before 202-203: the balance is displayed before the account is looked up;
        // then CBACT04C:375, 386 and the status line of status 23 (635-648). ABENDING PROGRAM is the step's line.
        assertEquals(List.of("image of 2001/01/1", "ACCOUNT NOT FOUND: 00000002001", "ERROR READING ACCOUNT FILE",
                "FILE STATUS IS: NNNN0023"), runLogLines);
        assertTrue(!stop.getMessage().contains("2001"), "no account id in a message for the application log");
    }

    @Test
    void intR08_missingCardStops() {
        cards.remove(2001L);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.CARD_MISSING, stop.reason());
        // CBACT04C:397, 408 and the status line of status 23
        assertEquals(List.of("image of 2001/01/1", "ACCOUNT NOT FOUND: 00000002001", "ERROR READING XREF FILE",
                "FILE STATUS IS: NNNN0023"), runLogLines);
        assertTrue(!stop.getMessage().contains("2001"), "no account id in a message for the application log");
    }

    @Test
    void intH03_twoCardsStops() {
        cards.put(2001L, List.of("4000000000002001", "4000000000009001"));

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.MULTI_CARD, stop.reason());
        assertEquals("INT-H03", stop.reason().ruleId());
        assertTrue(stop.getMessage().contains("INT-H03"), stop.getMessage());
        // no legacy text exists for this stop: the run log names the decision and the account
        assertEquals(List.of("image of 2001/01/1", "INT-H03 ACCOUNT HAS SEVERAL CARDS: 00000002001"), runLogLines);
        assertTrue(!stop.getMessage().contains("2001"), "no account id or card number in a message for the application log");
    }

    @Test
    void intH03_firstByCardNumberUsesLowerCard() {
        // the lookup hands the cards over in any order; 4000000000002001 is below 4000000000009001 and above 4000000000001001
        cards.put(2001L, List.of("4000000000009001", "4000000000002001"));
        InterestPolicy policy = new InterestPolicy(null, null, null, false, null, null, null, MultiCard.FIRST_BY_CARD_NUM, null);

        assertEquals("4000000000002001",
                calculator(policy, FROZEN).calculate(account2001(false)).transactions().get(0).cardNumber());

        cards.put(2001L, List.of("4000000000009001", "4000000000002001", "4000000000001001"));
        assertEquals("4000000000001001",
                calculator(policy, FROZEN).calculate(account2001(false)).transactions().get(0).cardNumber());
        assertEquals(List.of("image of 2001/01/1", "image of 2001/01/1"), runLogLines);
    }

    @Test
    void intH05_interestAboveFieldStops() {
        // by hand: 999999999.99 * 1200.01 / 1200 = 1000008333.32 (cut), one cent and more above 999999999.99
        rates.put("GOLD/01/1", new BigDecimal("1200.01"));
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "999999999.99")), false);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(item));

        assertEquals(StopReason.OVERFLOW, stop.reason());
        assertEquals("INT-H05", stop.reason().ruleId());
        assertTrue(stop.getMessage().contains("INT-H05"), stop.getMessage());
        assertEquals(List.of("image of 2001/01/1", "INT-H05 VALUE DOES NOT FIT ITS FIELD: INTEREST OF A BALANCE"), runLogLines);
        assertEquals(0, counter.issued(), "no transaction number is used for a transaction that is not written");
    }

    @Test
    void intH05_largestInterestThatFitsIsWrittenUncut() {
        // by hand: 999999999.99 * 1200.00 / 1200 = 999999999.99, the largest value of the field
        rates.put("GOLD/01/1", new BigDecimal("1200.00"));
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "999999999.99")), true);

        assertEquals(new BigDecimal("999999999.99"), calculator().calculate(item).transactions().get(0).amount());
    }

    @Test
    void intH05_negativeInterestBelowFieldStops() {
        // INT-Q05 as coded charges a credit balance: -999999999.99 * 1200.01 / 1200 = -1000008333.32
        rates.put("GOLD/01/1", new BigDecimal("1200.01"));
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "-999999999.99")), false);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(item));

        assertEquals(StopReason.OVERFLOW, stop.reason());
    }

    @Test
    void intH05_interestOfAnAccountAboveFieldStops() {
        // by hand: 600000000.00 * 12.00 / 1200 = 6000000.00 ... each balance fits; at 1200.00 % each is 600000000.00
        // and the two together are 1200000000.00, above the 999999999.99 of the account's total
        rates.put("GOLD/01/1", new BigDecimal("1200.00"));
        rates.put("GOLD/01/2", new BigDecimal("1200.00"));
        AccountBalances item = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "600000000.00"), balance(2001L, "01", 2, "600000000.00")), false);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(item));

        assertEquals(StopReason.OVERFLOW, stop.reason());
        assertEquals("INT-H05 VALUE DOES NOT FIT ITS FIELD: INTEREST OF AN ACCOUNT", runLogLines.get(runLogLines.size() - 1));
    }

    @Test
    void intH05_accountBalanceAboveFieldStops() {
        // by hand: 9999999995.00 + 10.00 = 10000000005.00, above the 9999999999.99 of the balance
        accounts.put(2001L, new Account(2001L, "Y", new BigDecimal("9999999995.00"), "GOLD"));

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.OVERFLOW, stop.reason());
        assertEquals("INT-H05 VALUE DOES NOT FIT ITS FIELD: ACCOUNT BALANCE", runLogLines.get(runLogLines.size() - 1));
    }

    @Test
    void intH05_millionthTransactionStops() {
        // 999,999 transactions were written: 999999 is the last number six digits hold
        String last = null;
        for (int issued = 0; issued < 999_999; issued++) {
            last = counter.next();
        }
        assertEquals("999999", last);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.OVERFLOW, stop.reason());
        assertEquals("INT-H05", stop.reason().ruleId());
        assertTrue(stop.getMessage().contains("INT-H05"), stop.getMessage());
        assertEquals(List.of("image of 2001/01/1", "INT-H05 VALUE DOES NOT FIT ITS FIELD: TRANSACTION NUMBER"), runLogLines);
        assertEquals(999_999, counter.issued(), "the counter does not start again at 000000");
    }

    @Test
    void intH05_transaction999999IsTheLastOneWritten() {
        for (int issued = 0; issued < 999_998; issued++) {
            counter.next();
        }

        assertEquals("2022071800999999", calculator().calculate(account2001(false)).transactions().get(0).id());
    }

    @Test
    void intH05_widenHandsTheValueOnWithEveryDigit() {
        // by hand: 999999999.99 * 1200.01 / 1200 = 1000008333.3233... cut to 1000008333.32; transaction 1,000,000
        InterestPolicy policy = new InterestPolicy(null, null, null, false, null, null, null, null, Overflow.WIDEN);
        rates.put("GOLD/01/1", new BigDecimal("1200.01"));
        for (int issued = 0; issued < 999_999; issued++) {
            counter.next();
        }
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "999999999.99")), true);

        InterestTransaction transaction = calculator(policy, FROZEN).calculate(item).transactions().get(0);

        assertEquals(new BigDecimal("1000008333.32"), transaction.amount());
        assertEquals("20220718001000000", transaction.id());
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
        InterestPolicy policy = new InterestPolicy(null, null, null, true, null, null, null, null, null);

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
        InterestPolicy policy = new InterestPolicy(null, IdPrefix.RUN_DATE, null, false, null, null, null, null, null);

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
    void intQ03_cycleDateWhenSwitched() {
        InterestPolicy policy = new InterestPolicy(null, null, TimestampSource.CYCLE_DATE, false, null, null, null, null, null);

        InterestTransaction transaction = calculator(policy, FROZEN).calculate(account2001(false)).transactions().get(0);

        // PARM 2022071800: the cycle date 2022-07-18 at 00.00.00, on both timestamps; the id is unchanged
        assertEquals("2022-07-18-00.00.00.000000", transaction.originTimestamp());
        assertEquals("2022-07-18-00.00.00.000000", transaction.processedTimestamp());
        assertEquals("2022071800000001", transaction.id());
    }

    @Test
    void intQ03_cycleDateOfAParmThatIsNotDigitsStops() {
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> RunTimestamp.ofCycleDate("JULY 2022 "));

        assertEquals(StopReason.CYCLE_DATE_UNREADABLE, stop.reason());
        assertEquals("INT-Q03", stop.reason().ruleId());
    }

    @Test
    void intQ03_defaultIsRunClock() {
        assertEquals(TimestampSource.RUN_CLOCK,
                new InterestPolicy(null, null, null, false, null, null, null, null, null).timestampSource());
        assertFalse(InterestPolicy.legacy().intQ03StampsTheCycleDate());
    }
}
