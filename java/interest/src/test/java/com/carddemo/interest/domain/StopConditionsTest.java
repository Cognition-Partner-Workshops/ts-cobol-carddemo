package com.carddemo.interest.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.FileCaseRun;
import com.carddemo.interest.PolicySwitches;
import com.carddemo.interest.domain.AccountBalances.BalanceRead;
import com.carddemo.interest.policy.InterestPolicy;
import com.carddemo.interest.policy.InterestPolicy.MultiCard;
import com.carddemo.interest.policy.InterestPolicy.Overflow;
import com.carddemo.interest.port.AccountStore;
import com.carddemo.interest.port.RunLog;

/**
 * Ticket 08: every condition under which the calculation stops, against in-memory ports. No
 * fixture has an abend, so the data are synthetic; the expected run-log lines are the DISPLAY
 * texts of CBACT04C, cited by line. Nothing is claimed about what the host does for INT-H03 and
 * INT-H05.
 */
class StopConditionsTest {

    private static final String ECHO = "image of 2001";

    private final Map<Long, Account> accounts = new HashMap<>(Map.of(
            2001L, new Account(2001L, new BigDecimal("1000.00"), "GOLD", "Y")));
    private final Map<Long, List<String>> cards = new HashMap<>(Map.of(2001L, List.of("4000000000002001")));
    private final Map<String, BigDecimal> rates = new HashMap<>(Map.of("GOLD/01/1", new BigDecimal("12.00")));
    private final List<String> runLogLines = new ArrayList<>();
    private final RunLog runLog = new RunLog() {
        @Override
        public void line(String text) {
            runLogLines.add(text);
        }

        @Override
        public void echo(String recordImage) {
            runLogLines.add(recordImage);
        }
    };

    private InterestCalculator calculator(InterestPolicy policy, TransactionCounter counter) {
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
        return new InterestCalculator(accountStore, accountId -> cards.getOrDefault(accountId, List.of()),
                (groupId, typeCode, categoryCode) ->
                        Optional.ofNullable(rates.get(groupId + "/" + typeCode + "/" + categoryCode)),
                runLog, policy, FileCaseRun.FROZEN, counter, "2022071800");
    }

    private static AccountBalances balance(String amount) {
        return new AccountBalances(2001L, List.of(new BalanceRead(
                new CategoryBalance(2001L, "01", 1, new BigDecimal(amount)), ECHO)), false);
    }

    private InterestRunStoppedException stop(InterestPolicy policy, TransactionCounter counter, String amount) {
        return assertThrows(InterestRunStoppedException.class, () -> calculator(policy, counter).calculate(balance(amount)));
    }

    private InterestRunStoppedException stop() {
        return stop(InterestPolicy.legacy(), new TransactionCounter(), "1000.00");
    }

    @Test
    void intR08_missingAccountStops() {
        accounts.clear();

        InterestRunStoppedException stopped = stop();

        assertEquals(StopReason.ACCOUNT_MISSING, stopped.reason());
        assertEquals("INT-R08", stopped.reason().ruleId());
        // CBACT04C:375 (INVALID KEY), 386, 643-646 (status 23), 629
        assertEquals(List.of(ECHO, "ACCOUNT NOT FOUND: 00000002001", "ERROR READING ACCOUNT FILE",
                "FILE STATUS IS: NNNN0023", "ABENDING PROGRAM"), runLogLines);
        // the id is in the run log only, never in the message that reaches the application log (S6.5)
        assertFalse(stopped.getMessage().contains("2001"));
    }

    @Test
    void intR08_missingCardStops() {
        cards.clear();

        assertEquals(StopReason.CARD_MISSING, stop().reason());
        // CBACT04C:396 (the same text as for the account), 408, 643-646, 629
        assertEquals(List.of(ECHO, "ACCOUNT NOT FOUND: 00000002001", "ERROR READING XREF FILE",
                "FILE STATUS IS: NNNN0023", "ABENDING PROGRAM"), runLogLines);
    }

    @Test
    void intR08_defaultRateMissingStops() {
        // no rate for the account's group and none for DEFAULT
        rates.clear();

        InterestRunStoppedException stopped = stop();

        assertEquals(StopReason.DEFAULT_RATE_MISSING, stopped.reason());
        assertEquals("INT-R08", stopped.reason().ruleId());
        // CBACT04C:418-419 (the fallback is tried first), 455, 643-646, 629
        assertEquals(List.of(ECHO, "DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE",
                "ERROR READING DEFAULT DISCLOSURE GROUP", "FILE STATUS IS: NNNN0023", "ABENDING PROGRAM"), runLogLines);
    }

    @Test
    void intH03_twoCardsStops() {
        cards.put(2001L, List.of("3000000000009001", "4000000000002001"));

        InterestRunStoppedException stopped = stop();

        assertEquals(StopReason.MULTI_CARD, stopped.reason());
        assertEquals("INT-H03", stopped.reason().ruleId());
        assertTrue(stopped.getMessage().contains("INT-H03"));
        // no legacy text exists: a line naming INT-H03, then the abend line; no status line
        assertEquals(List.of(ECHO, "INT-H03: THE ACCOUNT HAS SEVERAL CARDS", "ABENDING PROGRAM"), runLogLines);
    }

    @Test
    void intH03_firstByCardNumberWhenSwitched() {
        // the port returns the cards in card-number order; the lower number is used
        cards.put(2001L, List.of("3000000000009001", "4000000000002001"));

        AccountInterest result = calculator(PolicySwitches.multiCard(MultiCard.FIRST_BY_CARD_NUM), new TransactionCounter())
                .calculate(balance("1000.00"));

        assertEquals("3000000000009001", result.transactions().get(0).cardNumber());
        assertEquals(List.of(ECHO), runLogLines);
    }

    @Test
    void intH03_defaultIsStop() {
        assertEquals(MultiCard.STOP, new InterestPolicy(null, null, null, false, null, null, null, null, null).multiCard());
        assertTrue(InterestPolicy.legacy().intH03StopsOnSeveralCards());
    }

    @Test
    void intH05_interestAboveFieldStops() {
        // 999999999.99 * 9999.99 / 1200 = 8333324999.90..., above the 999999999.99 of S9(09)V99
        rates.put("GOLD/01/1", new BigDecimal("9999.99"));

        InterestRunStoppedException stopped = stop(InterestPolicy.legacy(), new TransactionCounter(), "999999999.99");

        assertEquals(StopReason.OVERFLOW, stopped.reason());
        assertEquals("INT-H05", stopped.reason().ruleId());
        assertTrue(stopped.getMessage().contains("INT-H05"));
        assertEquals(List.of(ECHO, "INT-H05: A VALUE DOES NOT FIT ITS FIELD", "ABENDING PROGRAM"), runLogLines);
    }

    @Test
    void intH05_largestInterestThatFitsIsWritten() {
        // 999999999.99 * 1200.00 / 1200 = 999999999.99 exactly: fits TRAN-AMT, nothing is cut
        rates.put("GOLD/01/1", new BigDecimal("1200.00"));

        AccountInterest result = calculator(InterestPolicy.legacy(), new TransactionCounter()).calculate(balance("999999999.99"));

        assertEquals(new BigDecimal("999999999.99"), result.transactions().get(0).amount());
        // 1000.00 + 999999999.99 = 1000000999.99 fits S9(10)V99
        assertEquals(Optional.of(new BigDecimal("1000000999.99")), result.newBalance());
    }

    @Test
    void intH05_accountTotalAboveFieldStops() {
        // two balances of 600000000.00 at 1200.00 % give 600000000.00 each; the total 1200000000.00
        // does not fit WS-TOTAL-INT S9(09)V99 (CBACT04C:169, 467)
        rates.put("GOLD/01/1", new BigDecimal("1200.00"));
        rates.put("GOLD/01/2", new BigDecimal("1200.00"));
        AccountBalances item = new AccountBalances(2001L, List.of(
                new BalanceRead(new CategoryBalance(2001L, "01", 1, new BigDecimal("600000000.00")), ECHO),
                new BalanceRead(new CategoryBalance(2001L, "01", 2, new BigDecimal("600000000.00")), ECHO)), false);

        InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class,
                () -> calculator(InterestPolicy.legacy(), new TransactionCounter()).calculate(item));

        assertEquals(StopReason.OVERFLOW, stopped.reason());
    }

    @Test
    void intH05_newBalanceAboveFieldStops() {
        // 9999999999.99 + 10.00 = 10000000009.99 does not fit ACCT-CURR-BAL S9(10)V99 (CBACT04C:352)
        accounts.put(2001L, new Account(2001L, new BigDecimal("9999999999.99"), "GOLD", "Y"));

        assertEquals(StopReason.OVERFLOW, stop().reason());
    }

    @Test
    void intH05_millionthTransactionStops() {
        // 999999 ids are issued: the next would be 1000000, seven digits (CBACT04C:173, 474)
        InterestRunStoppedException stopped = stop(InterestPolicy.legacy(), new TransactionCounter(999_999), "1000.00");

        assertEquals(StopReason.OVERFLOW, stopped.reason());
        assertEquals("INT-H05", stopped.reason().ruleId());
        assertEquals(List.of(ECHO, "INT-H05: A VALUE DOES NOT FIT ITS FIELD", "ABENDING PROGRAM"), runLogLines);
    }

    @Test
    void intH05_transaction999999IsTheLastThatFits() {
        AccountInterest result = calculator(InterestPolicy.legacy(), new TransactionCounter(999_998)).calculate(balance("1000.00"));

        assertEquals("2022071800999999", result.transactions().get(0).id());
    }

    @Test
    void intH05_defaultIsStopAndWidenIsRefused() {
        assertEquals(Overflow.STOP, new InterestPolicy(null, null, null, false, null, null, null, null, null).overflow());
        assertTrue(InterestPolicy.legacy().intH05StopsOnOverflow());
        // WIDEN needs wider fields than the frozen layouts and schema have: refused, naming the decision
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> new InterestPolicy(null, null, null, false, null, null, null, null, Overflow.WIDEN));
        assertTrue(refused.getMessage().contains("INT-H05"));
    }

    @Test
    void intT08_statusLineFormat() {
        // CBACT04C:643-646: the literal 'FILE STATUS IS: NNNN', then 00 and the two-character status
        assertEquals("FILE STATUS IS: NNNN0023", RunStop.statusLine("23"));
    }

    @Test
    void intT08_noStatusLineWhereTheLegacyStatusIsNotDetermined() {
        // a database failure replaces a read error whose status the source does not fix: message and abend line only
        InterestRunStoppedException stopped = RunStop.of(runLog, StopReason.READ_RATE, "the rates cannot be read");

        assertEquals(List.of("ERROR READING DISCLOSURE GROUP FILE", "ABENDING PROGRAM"), runLogLines);
        assertEquals("INT-R08", stopped.reason().ruleId());
        assertNull(stopped.file());
    }

    @Test
    void intT10_openFailureOfTheRatesKeepsTheCopiedText() {
        // CBACT04C:281: the open of the disclosure-group file reports 'ERROR OPENING DALY REJECTS FILE'
        InterestRunStoppedException stopped = RunStop.openFailed(runLog, LegacyFile.RATES, "the rates cannot be reached", null);

        assertEquals(StopReason.OPEN_INPUT, stopped.reason());
        assertEquals(LegacyFile.RATES, stopped.file());
        assertEquals(List.of("ERROR OPENING DALY REJECTS FILE", "ABENDING PROGRAM"), runLogLines);
    }

    @Test
    void intR08_openFailureTextOfEachFile() {
        // CBACT04C:245, 263, 300, 318
        for (LegacyFile file : LegacyFile.values()) {
            RunStop.openFailed(runLog, file, "cannot be reached", null);
        }

        assertEquals(List.of("ERROR OPENING TRANSACTION CATEGORY BALANCE", "ABENDING PROGRAM",
                "ERROR OPENING CROSS REF FILE", "ABENDING PROGRAM", "ERROR OPENING DALY REJECTS FILE", "ABENDING PROGRAM",
                "ERROR OPENING ACCOUNT MASTER FILE", "ABENDING PROGRAM", "ERROR OPENING TRANSACTION FILE", "ABENDING PROGRAM"),
                runLogLines);
    }
}
