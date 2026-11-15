package com.carddemo.interest.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
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
import com.carddemo.interest.policy.InterestPolicy.NegativeBalance;
import com.carddemo.interest.policy.InterestPolicy.Overflow;
import com.carddemo.interest.policy.InterestPolicy.Rounding;
import com.carddemo.interest.policy.InterestPolicy.TimestampSource;
import com.carddemo.interest.policy.InterestPolicy.ZeroInterest;
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
            2001L, new Account(2001L, new BigDecimal("1000.00"), "GOLD"),
            2002L, new Account(2002L, new BigDecimal("2400.00"), "STANDARD")));
    private final Map<Long, List<String>> cards = new HashMap<>(Map.of(
            2001L, List.of("4000000000002001"),
            2002L, List.of("4000000000002002")));
    private final Map<String, BigDecimal> rates = new HashMap<>(Map.of(
            "GOLD/01/1", new BigDecimal("12.00"),
            "GOLD/01/2", new BigDecimal("18.00"),
            "GOLD/02/1", new BigDecimal("0.00"),
            "STANDARD/01/1", new BigDecimal("18.50")));
    private final List<String> runLogLines = new ArrayList<>();
    private final List<String> rateLookups = new ArrayList<>();
    private TransactionCounter counter = new TransactionCounter();
    private HoleDecisions holes = HoleDecisions.stop();

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
                (groupId, typeCode, categoryCode) -> {
                    rateLookups.add(groupId + "/" + typeCode + "/" + categoryCode);
                    return Optional.ofNullable(rates.get(groupId + "/" + typeCode + "/" + categoryCode));
                },
                runLog, policy, holes, clock, counter, PARM_DATE);
    }

    private static InterestPolicy policy(IdPrefix idPrefix, TimestampSource timestampSource,
            NegativeBalance negativeBalance, ZeroInterest zeroInterest, InactiveAccounts inactiveAccounts) {
        return new InterestPolicy(Rounding.TRUNCATE, idPrefix, timestampSource, false,
                negativeBalance, zeroInterest, inactiveAccounts);
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
        // i04-default-group-fallback: PLATINUM has no rate, DEFAULT has 21.00 %; 1000.00 * 21.00 / 1200 = 17.50
        accounts.put(2001L, new Account(2001L, new BigDecimal("1000.00"), "PLATINUM"));
        rates.put("DEFAULT/01/1", new BigDecimal("21.00"));

        AccountInterest result = calculator().calculate(account2001(false));

        assertEquals(List.of(new BigDecimal("17.50")),
                result.transactions().stream().map(InterestTransaction::amount).toList());
        assertEquals(List.of("image of 2001/01/1", "DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE"),
                runLogLines);
    }

    @Test
    void intR03_defaultGroupKeyPadded() {
        // CBACT04C:437 moves 'DEFAULT' to the X(10) key; the domain asks for the trimmed constant and
        // keeps the type and category of the balance. The padding to CHAR(10) is the codec's.
        accounts.put(2001L, new Account(2001L, new BigDecimal("1000.00"), "PLATINUM"));
        rates.put("DEFAULT/01/2", new BigDecimal("21.00"));

        calculator().calculate(new AccountBalances(2001L, List.of(balance(2001L, "01", 2, "1000.00")), false));

        assertEquals(List.of("PLATINUM/01/2", "DEFAULT/01/2"), rateLookups);
    }

    @Test
    void intR03_noStaleRateAfterAFailedLookup() {
        // behaviour x: the second balance has its own group rate; the DEFAULT rate of the first is not carried over
        accounts.put(2001L, new Account(2001L, new BigDecimal("1000.00"), "SILVER"));
        rates.put("DEFAULT/01/1", new BigDecimal("21.00"));
        rates.put("SILVER/01/2", new BigDecimal("6.00"));
        AccountBalances item = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "1000.00"), balance(2001L, "01", 2, "1000.00")), false);

        // 1000.00 * 21.00 / 1200 = 17.50 and 1000.00 * 6.00 / 1200 = 5.00
        assertEquals(List.of(new BigDecimal("17.50"), new BigDecimal("5.00")),
                calculator().calculate(item).transactions().stream().map(InterestTransaction::amount).toList());
    }

    @Test
    void intR03_missingDefaultRateStops() {
        // CBACT04C:416-419, 443-459, 628-648: no DEFAULT rate either. The two fallback lines are displayed
        // first, then the message of the DEFAULT read, status 23 and the abend line.
        accounts.put(2001L, new Account(2001L, new BigDecimal("1000.00"), "SILVER"));

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.DEFAULT_RATE_MISSING, stop.reason());
        assertEquals("INT-R08", stop.reason().ruleId());
        assertEquals(List.of("image of 2001/01/1", "DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE",
                "ERROR READING DEFAULT DISCLOSURE GROUP", "FILE STATUS IS: NNNN0023", "ABENDING PROGRAM"), runLogLines);
    }

    @Test
    void intR04_zeroRateWritesNoTransaction() {
        // i03-zero-rate-skipped: purchases 1000.00 at 12.00 % = 10.00, payments -300.00 at 0.00 % = nothing
        AccountBalances item = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "1000.00"), balance(2001L, "02", 1, "-300.00")), false);

        AccountInterest result = calculator().calculate(item);

        assertEquals(List.of(new BigDecimal("10.00")),
                result.transactions().stream().map(InterestTransaction::amount).toList());
        // 1000.00 + 10.00: the zero-rate balance adds nothing to the account
        assertEquals(Optional.of(new BigDecimal("1010.00")), result.newBalance());
        assertEquals(List.of("image of 2001/01/1", "image of 2001/02/1"), runLogLines);
        assertEquals(1, counter.issued());
    }

    @Test
    void intR04_accountWithOnlyZeroRatesIsStillUpdatedAtTheBreak() {
        // CBACT04C:194-201, 350-356: the break adds a total of 0.00 and resets the cycle amounts
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "02", 1, "500.00")), false);

        AccountInterest result = calculator().calculate(item);

        assertEquals(List.of(), result.transactions());
        assertEquals(Optional.of(new BigDecimal("1000.00")), result.newBalance());
    }

    @Test
    void intQ05_negativeBalanceGivesNegativeInterest() {
        // i06-negative-balance: -300.00 * 12.00 / 1200 = -3.00
        accounts.put(2001L, new Account(2001L, new BigDecimal("-300.00"), "GOLD"));
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "-300.00")), false);

        AccountInterest result = calculator().calculate(item);

        assertEquals(List.of(new BigDecimal("-3.00")),
                result.transactions().stream().map(InterestTransaction::amount).toList());
        // -300.00 + -3.00
        assertEquals(Optional.of(new BigDecimal("-303.00")), result.newBalance());
    }

    @Test
    void intQ05_negativeBalanceSkippedWhenSwitched() {
        // 1000.00 at 12.00 % = 10.00; the credit balance -300.00 at 18.00 % gets nothing under SKIP
        AccountBalances item = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "1000.00"), balance(2001L, "01", 2, "-300.00")), false);

        AccountInterest result = calculator(policy(null, null, NegativeBalance.SKIP, null, null), FROZEN).calculate(item);

        assertEquals(List.of(new BigDecimal("10.00")),
                result.transactions().stream().map(InterestTransaction::amount).toList());
        assertEquals(Optional.of(new BigDecimal("1010.00")), result.newBalance());
        assertEquals(1, counter.issued());
    }

    @Test
    void intQ05_defaultIsCharge() {
        assertEquals(NegativeBalance.CHARGE, policy(null, null, null, null, null).negativeBalance());
        assertEquals(NegativeBalance.CHARGE, InterestPolicy.legacy().negativeBalance());
    }

    @Test
    void intQ06_zeroInterestStillWritten() {
        // no fixture: 0.50 * 12.00 / 1200 = 0.005, cut to 0.00, and the rate is not zero
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "0.50")), false);

        AccountInterest result = calculator().calculate(item);

        assertEquals(List.of(new BigDecimal("0.00")),
                result.transactions().stream().map(InterestTransaction::amount).toList());
        assertEquals("2022071800000001", result.transactions().get(0).id());
        assertEquals(Optional.of(new BigDecimal("1000.00")), result.newBalance());
    }

    @Test
    void intQ06_zeroInterestSkippedWhenSwitched() {
        // 0.50 * 12.00 / 1200 = 0.005 -> 0.00: nothing written; 500.00 at 18.00 % = 7.50 gets the first id
        AccountBalances item = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "0.50"), balance(2001L, "01", 2, "500.00")), false);

        AccountInterest result = calculator(policy(null, null, null, ZeroInterest.SKIP, null), FROZEN).calculate(item);

        assertEquals(List.of(new BigDecimal("7.50")),
                result.transactions().stream().map(InterestTransaction::amount).toList());
        assertEquals("2022071800000001", result.transactions().get(0).id());
        assertEquals(Optional.of(new BigDecimal("1007.50")), result.newBalance());
    }

    @Test
    void intQ06_defaultIsWrite() {
        assertEquals(ZeroInterest.WRITE, policy(null, null, null, null, null).zeroInterest());
        assertEquals(ZeroInterest.WRITE, InterestPolicy.legacy().zeroInterest());
    }

    @Test
    void intH01_noFeeTransaction() {
        // i01-baseline: one balance, one transaction of type 01 category 5 and nothing else
        List<InterestTransaction> transactions = calculator().calculate(account2001(false)).transactions();

        assertEquals(1, transactions.size());
        assertEquals(new BigDecimal("10.00"), transactions.get(0).amount());
        assertEquals(1, counter.issued());
    }

    @Test
    void intH04_inactiveAccountChargedLikeAnyOther() {
        // no fixture: the active flag is N and is not looked at; 1000.00 * 12.00 / 1200 = 10.00
        accounts.put(2001L, new Account(2001L, new BigDecimal("1000.00"), "GOLD", Optional.of(false)));

        AccountInterest result = calculator().calculate(account2001(false));

        assertEquals(List.of(new BigDecimal("10.00")),
                result.transactions().stream().map(InterestTransaction::amount).toList());
        assertEquals(Optional.of(new BigDecimal("1010.00")), result.newBalance());
    }

    @Test
    void intH04_flagIsNotAskedForAsCoded() {
        // the pilot's adapter supplies no flag; as coded the calculation never needs it
        assertEquals(Optional.empty(), accounts.get(2001L).active());
        assertEquals(1, calculator().calculate(account2001(false)).transactions().size());
    }

    @Test
    void intH04_inactiveAccountSkippedWhenSwitched() {
        accounts.put(2001L, new Account(2001L, new BigDecimal("1000.00"), "GOLD", Optional.of(false)));
        accounts.put(2002L, new Account(2002L, new BigDecimal("2400.00"), "STANDARD", Optional.of(true)));
        InterestCalculator calculator = calculator(policy(null, null, null, null, InactiveAccounts.SKIP), FROZEN);

        AccountInterest inactive = calculator.calculate(account2001(false));
        AccountInterest active = calculator.calculate(account2002(false));

        assertEquals(List.of(), inactive.transactions());
        assertEquals(Optional.empty(), inactive.newBalance());
        // 2400.00 * 18.50 / 1200 = 37.00; the skipped account used no transaction number
        assertEquals("2022071800000001", active.transactions().get(0).id());
        assertEquals(Optional.of(new BigDecimal("2437.00")), active.newBalance());
        assertEquals(List.of("image of 2001/01/1", "image of 2002/01/1"), runLogLines);
    }

    @Test
    void intH04_skipWithoutTheFlagStops() {
        InterestCalculator calculator = calculator(policy(null, null, null, null, InactiveAccounts.SKIP), FROZEN);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator.calculate(account2001(false)));

        assertEquals(StopReason.ACTIVE_FLAG_NOT_SUPPLIED, stop.reason());
        assertEquals("INT-H04", stop.reason().ruleId());
    }

    @Test
    void intH04_defaultIsCharge() {
        assertEquals(InactiveAccounts.CHARGE, policy(null, null, null, null, null).inactiveAccounts());
        assertEquals(InactiveAccounts.CHARGE, InterestPolicy.legacy().inactiveAccounts());
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
        // CBACT04C:193 comes before 202-203: the balance is displayed before the account is looked up.
        // CBACT04C:373-390, 628-648: the id as FD-ACCT-ID PIC 9(11), the message, status 23, the abend line.
        assertEquals(List.of("image of 2001/01/1", "ACCOUNT NOT FOUND: 00000002001", "ERROR READING ACCOUNT FILE",
                "FILE STATUS IS: NNNN0023", "ABENDING PROGRAM"), runLogLines);
        assertTrue(!stop.getMessage().contains("2001"), "no account id in a message for the application log");
    }

    @Test
    void intR08_missingCardStops() {
        cards.remove(2001L);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.CARD_MISSING, stop.reason());
        assertEquals("INT-R08", stop.reason().ruleId());
        // CBACT04C:394-412: the legacy displays ACCOUNT NOT FOUND with FD-XREF-ACCT-ID for a missing card too
        assertEquals(List.of("image of 2001/01/1", "ACCOUNT NOT FOUND: 00000002001", "ERROR READING XREF FILE",
                "FILE STATUS IS: NNNN0023", "ABENDING PROGRAM"), runLogLines);
        assertTrue(!stop.getMessage().contains("2001"), "no account id in a message for the application log");
    }

    @Test
    void intR08_nothingIsReturnedForAnAccountThatStops() {
        // S6.2: the stop is an exception out of calculate, so no transaction and no new balance exist
        // for the account; a second balance of the account is not even displayed.
        accounts.remove(2001L);
        AccountBalances twoBalances = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "1000.00"), balance(2001L, "01", 2, "500.00")), false);

        assertThrows(InterestRunStoppedException.class, () -> calculator().calculate(twoBalances));

        assertEquals("ABENDING PROGRAM", runLogLines.get(runLogLines.size() - 1));
        assertTrue(!runLogLines.contains("image of 2001/01/2"), "nothing runs after the abend line");
        assertEquals(0, counter.issued());
    }

    @Test
    void intH03_twoCardsStops() {
        cards.put(2001L, List.of("4000000000002001", "4000000000009001"));

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.MULTI_CARD, stop.reason());
        assertEquals("INT-H03", stop.reason().ruleId());
        assertTrue(stop.getMessage().contains("INT-H03"), stop.getMessage());
        // no legacy text exists for this stop: the line names the open decision
        assertEquals(List.of("image of 2001/01/1", "INT-H03 ACCOUNT HAS SEVERAL CARDS: 00000002001", "ABENDING PROGRAM"),
                runLogLines);
        assertTrue(!stop.getMessage().contains("2001") && !stop.getMessage().contains("4000"),
                "no account id or card number in a message for the application log");
    }

    @Test
    void intH03_lowerCardNumberWhenSwitched() {
        // multiCard=FIRST_BY_CARD_NUM: the port returns the cards in card-number order; the first is used
        holes = new HoleDecisions(MultiCard.FIRST_BY_CARD_NUM, Overflow.STOP);
        cards.put(2001L, List.of("4000000000002001", "4000000000009001"));

        AccountInterest result = calculator().calculate(account2001(false));

        assertEquals("4000000000002001", result.transactions().get(0).cardNumber());
        assertEquals(List.of("image of 2001/01/1"), runLogLines);
    }

    @Test
    void intH03_defaultIsStop() {
        assertEquals(MultiCard.STOP, HoleDecisions.stop().multiCard());
        assertEquals(MultiCard.STOP, new HoleDecisions(null, null).multiCard());
    }

    @Test
    void intH03_missingCardStillStopsWhenSwitched() {
        // FIRST_BY_CARD_NUM decides between several cards only; no card is INT-R08 as before
        holes = new HoleDecisions(MultiCard.FIRST_BY_CARD_NUM, Overflow.STOP);
        cards.remove(2001L);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.CARD_MISSING, stop.reason());
    }

    @Test
    void intH05_interestAboveFieldStops() {
        // TRAN-AMT is S9(09)V99: 999999999.99 at most.
        // 999999999.99 x 1500.00 / 1200 = 1249999999.9875 -> 1249999999.98, ten integer digits.
        rates.put("GOLD/01/1", new BigDecimal("1500.00"));
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "999999999.99")), false);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(item));

        assertEquals(StopReason.OVERFLOW, stop.reason());
        assertEquals("INT-H05", stop.reason().ruleId());
        assertTrue(stop.getMessage().contains("INT-H05"), stop.getMessage());
        assertEquals(List.of("image of 2001/01/1", "INT-H05 VALUE DOES NOT FIT FIELD: TRAN-AMT", "ABENDING PROGRAM"),
                runLogLines);
        assertTrue(!stop.getMessage().contains("1249999999") && !stop.getMessage().contains("249999999.98"),
                "neither the amount nor a truncated amount in the message");
        assertEquals(0, counter.issued(), "no transaction id was issued for an amount that does not fit");
    }

    @Test
    void intH05_largestInterestThatFitsIsWritten() {
        // 999999999.99 x 1200.00 / 1200 = 999999999.99: exactly the largest value of S9(09)V99
        rates.put("GOLD/01/1", new BigDecimal("1200.00"));
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "999999999.99")), true);

        AccountInterest result = calculator().calculate(item);

        assertEquals(new BigDecimal("999999999.99"), result.transactions().get(0).amount());
    }

    @Test
    void intH05_negativeInterestBelowFieldStops() {
        // INT-Q05 charges a credit balance: -999999999.99 x 1500.00 / 1200 = -1249999999.98 (towards zero)
        rates.put("GOLD/01/1", new BigDecimal("1500.00"));
        AccountBalances item = new AccountBalances(2001L, List.of(balance(2001L, "01", 1, "-999999999.99")), false);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(item));

        assertEquals(StopReason.OVERFLOW, stop.reason());
    }

    @Test
    void intH05_accountTotalAboveFieldStops() {
        // WS-TOTAL-INT is S9(09)V99 (CBACT04C:168-169, 467): each interest fits, their sum does not.
        // 600000000.00 x 1200.00 / 1200 = 600000000.00, twice = 1200000000.00.
        rates.put("GOLD/01/1", new BigDecimal("1200.00"));
        rates.put("GOLD/01/2", new BigDecimal("1200.00"));
        AccountBalances item = new AccountBalances(2001L,
                List.of(balance(2001L, "01", 1, "600000000.00"), balance(2001L, "01", 2, "600000000.00")), false);

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(item));

        assertEquals(StopReason.OVERFLOW, stop.reason());
        assertEquals("INT-H05 VALUE DOES NOT FIT FIELD: WS-TOTAL-INT", runLogLines.get(runLogLines.size() - 2));
    }

    @Test
    void intH05_newBalanceAboveFieldStops() {
        // ACCT-CURR-BAL is S9(10)V99 (CBACT04C:352): 9999999999.99 + 10.00 = 10000000009.99 does not fit.
        accounts.put(2001L, new Account(2001L, new BigDecimal("9999999999.99"), "GOLD"));

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2001(false)));

        assertEquals(StopReason.OVERFLOW, stop.reason());
        assertEquals("INT-H05 VALUE DOES NOT FIT FIELD: ACCT-CURR-BAL", runLogLines.get(runLogLines.size() - 2));
    }

    @Test
    void intH05_millionthTransactionStops() {
        // TRAN-ID holds six digits of the counter (CBACT04C:173, 474): 999999 is the last id of a run.
        counter = new TransactionCounter(999_998);

        AccountInterest last = calculator().calculate(account2001(false));
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator().calculate(account2002(false)));

        assertEquals(PARM_DATE + "999999", last.transactions().get(0).id());
        assertEquals(StopReason.OVERFLOW, stop.reason());
        assertEquals("INT-H05", stop.reason().ruleId());
        assertEquals(List.of("image of 2001/01/1", "image of 2002/01/1", "INT-H05 VALUE DOES NOT FIT FIELD: TRAN-ID",
                "ABENDING PROGRAM"), runLogLines);
        assertEquals(999_999, counter.issued(), "the counter does not start again at 000000");
    }

    @Test
    void intH05_defaultIsStop() {
        assertEquals(Overflow.STOP, HoleDecisions.stop().overflow());
        assertEquals(Overflow.STOP, new HoleDecisions(null, null).overflow());
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
    void intQ03_cycleDateOfTheParmWhenSwitched() {
        // option B of INT-Q03: PARM 2022071800 is cycle date 2022-07-18, at 00.00.00
        InterestPolicy policy = policy(null, TimestampSource.CYCLE_DATE, null, null, null);

        InterestTransaction transaction = calculator(policy, FROZEN).calculate(account2001(false)).transactions().get(0);

        assertEquals("2022-07-18-00.00.00.000000", transaction.originTimestamp());
        assertEquals("2022-07-18-00.00.00.000000", transaction.processedTimestamp());
        assertEquals("2022071800000001", transaction.id());
    }

    @Test
    void intQ03_cycleDateNeedsAParmThatStartsWithADate() {
        InterestPolicy policy = policy(null, TimestampSource.CYCLE_DATE, null, null, null);

        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> policy.intQ03TransactionMoment(LocalDateTime.of(2022, 7, 5, 10, 0), "20221332AB"));

        assertTrue(refused.getMessage().contains("INT-Q03"));
    }

    @Test
    void intQ03_defaultIsRunClock() {
        assertEquals(TimestampSource.RUN_CLOCK, new InterestPolicy(null, null, null, false, null, null, null).timestampSource());
        assertEquals(LocalDateTime.of(2022, 7, 5, 10, 0),
                InterestPolicy.legacy().intQ03TransactionMoment(LocalDateTime.of(2022, 7, 5, 10, 0), PARM_DATE));
    }
}
