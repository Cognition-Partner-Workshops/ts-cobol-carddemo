package com.carddemo.interest.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.carddemo.interest.policy.InterestPolicy.InactiveAccounts;
import com.carddemo.interest.policy.InterestPolicy.NegativeBalance;
import com.carddemo.interest.policy.InterestPolicy.TimestampSource;
import com.carddemo.interest.policy.InterestPolicy.ZeroInterest;
import com.carddemo.interest.port.AccountStore;
import com.carddemo.interest.port.RunLog;

/**
 * The open decisions ticket 04 adds (INT-Q03 option B, INT-Q05, INT-Q06, INT-H01, INT-H04) against
 * in-memory ports: the default of each switch and each other value. Synthetic data; every expected
 * value is worked by hand in a comment.
 */
class CalculationSwitchesTest {

    private static final String PARM_DATE = "2022071800";

    private final Map<Long, Account> accounts = new HashMap<>(Map.of(
            2008L, new Account(2008L, new BigDecimal("-300.00"), "GOLD", "Y"),
            2010L, new Account(2010L, new BigDecimal("500.00"), "GOLD", "N")));
    private final List<String> runLogLines = new ArrayList<>();

    private InterestCalculator calculator(InterestPolicy policy, String parmDate) {
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
        // GOLD 01/0001 is 12.00 % as in the fixtures; no other rate exists
        return new InterestCalculator(accountStore, accountId -> List.of("400000000000" + accountId),
                (groupId, typeCode, categoryCode) -> "GOLD".equals(groupId) && "01".equals(typeCode) && categoryCode == 1
                        ? Optional.of(new BigDecimal("12.00")) : Optional.empty(),
                runLog, policy, FileCaseRun.FROZEN, new TransactionCounter(), parmDate);
    }

    private InterestCalculator calculator(InterestPolicy policy) {
        return calculator(policy, PARM_DATE);
    }

    private static AccountBalances one(long accountId, String amount, boolean endsInput) {
        return new AccountBalances(accountId, List.of(new BalanceRead(
                new CategoryBalance(accountId, "01", 1, new BigDecimal(amount)), "image of " + accountId)), endsInput);
    }

    @Test
    void intQ05_negativeBalanceGivesNegativeInterest() {
        // i06-negative-balance data: -300.00 * 12.00 / 1200 = -3.00; -300.00 + -3.00 = -303.00
        AccountInterest result = calculator(InterestPolicy.legacy()).calculate(one(2008L, "-300.00", false));

        assertEquals(new BigDecimal("-3.00"), result.transactions().get(0).amount());
        assertEquals(Optional.of(new BigDecimal("-303.00")), result.newBalance());
    }

    @Test
    void intQ05_truncatesTowardsZeroOnACreditBalance() {
        // -300.50 * 12.00 / 1200 = -3.005: cut towards zero is -3.00, not -3.01
        assertEquals(new BigDecimal("-3.00"),
                calculator(InterestPolicy.legacy()).calculate(one(2008L, "-300.50", false)).transactions().get(0).amount());
    }

    @Test
    void intQ05_creditBalanceSkippedWhenSwitched() {
        // option B: no interest and no transaction; the break still posts the total of 0.00 (-300.00 stays)
        AccountInterest result = calculator(PolicySwitches.negativeBalance(NegativeBalance.SKIP))
                .calculate(one(2008L, "-300.00", false));

        assertEquals(List.of(), result.transactions());
        assertEquals(Optional.of(new BigDecimal("-300.00")), result.newBalance());
    }

    @Test
    void intQ05_skipLeavesDebitBalancesAlone() {
        // 300.00 * 12.00 / 1200 = 3.00
        assertEquals(new BigDecimal("3.00"), calculator(PolicySwitches.negativeBalance(NegativeBalance.SKIP))
                .calculate(one(2008L, "300.00", false)).transactions().get(0).amount());
    }

    @Test
    void intQ05_defaultIsCharge() {
        assertEquals(NegativeBalance.CHARGE, new InterestPolicy(null, null, null, false, null, null, null, null, null).negativeBalance());
        assertTrue(InterestPolicy.legacy().intQ05ChargesCreditBalance());
    }

    @Test
    void intQ06_zeroInterestIsStillPosted() {
        // no fixture: 0.50 * 12.00 / 1200 = 0.005, cut to 0.00; the rate is not zero, so the transaction is written
        AccountInterest result = calculator(InterestPolicy.legacy()).calculate(one(2008L, "0.50", false));

        assertEquals(1, result.transactions().size());
        assertEquals(new BigDecimal("0.00"), result.transactions().get(0).amount());
        assertEquals("2022071800000001", result.transactions().get(0).id());
    }

    @Test
    void intQ06_zeroInterestSkippedWhenSwitched() {
        InterestCalculator calculator = calculator(PolicySwitches.zeroInterest(ZeroInterest.SKIP));

        assertEquals(List.of(), calculator.calculate(one(2008L, "0.50", false)).transactions());
        // 300.00 * 12.00 / 1200 = 3.00 is written, with the first id: the skipped amount used none
        InterestTransaction next = calculator.calculate(one(2008L, "300.00", false)).transactions().get(0);
        assertEquals(new BigDecimal("3.00"), next.amount());
        assertEquals("2022071800000001", next.id());
    }

    @Test
    void intQ06_defaultIsWrite() {
        assertEquals(ZeroInterest.WRITE, new InterestPolicy(null, null, null, false, null, null, null, null, null).zeroInterest());
        assertTrue(InterestPolicy.legacy().intQ06WritesZeroInterest());
    }

    @Test
    void intH04_inactiveAccountIsChargedLikeAnyOther() {
        // no fixture: account 2010 has active flag N; 500.00 * 12.00 / 1200 = 5.00; 500.00 + 5.00 = 505.00
        AccountInterest result = calculator(InterestPolicy.legacy()).calculate(one(2010L, "500.00", false));

        assertEquals(new BigDecimal("5.00"), result.transactions().get(0).amount());
        assertEquals(Optional.of(new BigDecimal("505.00")), result.newBalance());
    }

    @Test
    void intH04_inactiveAccountSkippedWhenSwitched() {
        // option B: no transaction and no update; the record is still displayed as read (CBACT04C:193)
        AccountInterest result = calculator(PolicySwitches.inactiveAccounts(InactiveAccounts.SKIP))
                .calculate(one(2010L, "500.00", false));

        assertEquals(List.of(), result.transactions());
        assertEquals(Optional.empty(), result.newBalance());
        assertEquals(List.of("image of 2010"), runLogLines);
    }

    @Test
    void intH04_skipLeavesActiveAccountsAlone() {
        // account 2008 is active: -300.00 * 12.00 / 1200 = -3.00
        assertEquals(new BigDecimal("-3.00"), calculator(PolicySwitches.inactiveAccounts(InactiveAccounts.SKIP))
                .calculate(one(2008L, "-300.00", false)).transactions().get(0).amount());
    }

    @Test
    void intH04_defaultIsCharge() {
        assertEquals(InactiveAccounts.CHARGE, new InterestPolicy(null, null, null, false, null, null, null, null, null).inactiveAccounts());
        assertTrue(InterestPolicy.legacy().intH04ChargesInactiveAccounts());
        assertTrue(new Account(1L, BigDecimal.ZERO, "GOLD", "Y").isActive());
        assertFalse(new Account(1L, BigDecimal.ZERO, "GOLD", "N").isActive());
        assertFalse(new Account(1L, BigDecimal.ZERO, "GOLD", "").isActive());
    }

    @Test
    void intH01_noFeeTransaction() {
        // CBACT04C:518-520 is empty: one balance gives exactly one transaction, the interest, and the
        // account total is the interest alone (500.00 + 5.00)
        AccountInterest result = calculator(InterestPolicy.legacy()).calculate(one(2010L, "500.00", false));

        assertEquals(1, result.transactions().size());
        assertEquals("Int. for a/c 00000002010", result.transactions().get(0).description());
        assertEquals(Optional.of(new BigDecimal("505.00")), result.newBalance());
    }

    @Test
    void intQ03_cycleDateOfTheParmWhenSwitched() {
        // option B: the cycle date of the PARM 2022071800 at 00.00.00, on both timestamps; the id is unchanged
        InterestTransaction transaction = calculator(PolicySwitches.timestampSource(TimestampSource.CYCLE_DATE))
                .calculate(one(2008L, "300.00", false)).transactions().get(0);

        assertEquals("2022-07-18-00.00.00.000000", transaction.originTimestamp());
        assertEquals("2022-07-18-00.00.00.000000", transaction.processedTimestamp());
        assertEquals("2022071800000001", transaction.id());
    }

    @Test
    void intQ03_cycleDateNeedsEightDigitsInTheParm() {
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> calculator(PolicySwitches.timestampSource(TimestampSource.CYCLE_DATE), "JULY 2022 ")
                        .calculate(one(2008L, "300.00", false)));

        assertEquals(StopReason.CYCLE_DATE_UNREADABLE, stop.reason());
        assertEquals("INT-Q03", stop.reason().ruleId());
    }

    @Test
    void intQ03_runClockIgnoresWhatThePARMHolds() {
        // as coded the PARM is text for the id only (CBACT04C:476-480): a PARM that is no date still runs
        InterestTransaction transaction = calculator(InterestPolicy.legacy(), "JULY 2022 ")
                .calculate(one(2008L, "300.00", false)).transactions().get(0);

        assertEquals("JULY 2022 000001", transaction.id());
        assertEquals("2022-07-05-10.00.00.000000", transaction.originTimestamp());
    }
}
