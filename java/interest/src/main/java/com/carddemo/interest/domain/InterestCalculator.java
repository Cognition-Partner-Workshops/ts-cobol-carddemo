package com.carddemo.interest.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.carddemo.interest.domain.AccountBalances.BalanceRead;
import com.carddemo.interest.policy.InterestPolicy;
import com.carddemo.interest.port.AccountStore;
import com.carddemo.interest.port.CardLookup;
import com.carddemo.interest.port.RateLookup;
import com.carddemo.interest.port.RunLog;

/**
 * The replacement of CBACT04C's main loop for one account (CBACT04C:188-222): it depends on the
 * ports (FI-3) and the policy only. It covers INT-R01 to R07 and the open decisions INT-Q01 to
 * Q06, INT-H01 and INT-H04, each as coded behind its switch, and the stop conditions: INT-R08 with
 * the lines CBACT04C displays before it abends, and the two holes INT-H03 and INT-H05 behind their
 * switches. The line ABENDING PROGRAM is written by the step, which ends the run.
 */
public final class InterestCalculator {

    /** INT-R06 (CBACT04C:482-491). */
    private static final String INTEREST_TYPE_CODE = "01";
    private static final int INTEREST_CATEGORY_CODE = 5;
    private static final String INTEREST_SOURCE = "System";
    private static final String DESCRIPTION_PREFIX = "Int. for a/c ";
    private static final long NO_MERCHANT = 0L;
    /** INT-R03 (CBACT04C:437): the group whose rate is used when the account's group has none. */
    private static final String DEFAULT_GROUP = "DEFAULT";
    /** INT-H05: the largest interest of a balance and of an account, nine digits and two decimals (CBACT04C:168-169). */
    private static final BigDecimal LARGEST_INTEREST = new BigDecimal("999999999.99");
    /** INT-H05: the largest account balance, ten digits and two decimals (CBACT04C:352). */
    private static final BigDecimal LARGEST_BALANCE = new BigDecimal("9999999999.99");

    private final AccountStore accounts;
    private final CardLookup cards;
    private final RateLookup rates;
    private final RunLog runLog;
    private final InterestPolicy policy;
    private final Clock clock;
    private final TransactionCounter counter;
    private final String parmDate;

    public InterestCalculator(AccountStore accounts, CardLookup cards, RateLookup rates, RunLog runLog,
            InterestPolicy policy, Clock clock, TransactionCounter counter, String parmDate) {
        this.accounts = accounts;
        this.cards = cards;
        this.rates = rates;
        this.runLog = runLog;
        this.policy = policy;
        this.clock = clock;
        this.counter = counter;
        this.parmDate = parmDate;
    }

    public AccountInterest calculate(AccountBalances item) {
        Account account = null;
        String cardNumber = null;
        BigDecimal totalInterest = BigDecimal.ZERO.setScale(2);
        List<InterestTransaction> transactions = new ArrayList<>();

        for (BalanceRead read : item.balances()) {
            // CBACT04C:193 - every balance record is displayed as read, before any lookup.
            runLog.echo(read.recordImage());
            if (account == null) {
                account = accountOf(item.accountId());
                cardNumber = cardNumberOf(item.accountId());
            }
            CategoryBalance balance = read.balance();
            BigDecimal yearlyRate = rateFor(account, balance);
            if (!policy.intH04ChargesAccount(account.activeStatus())) {
                continue;
            }
            // INT-R04 (CBACT04C:214): a zero rate means no interest and no transaction.
            if (yearlyRate.signum() == 0 || !policy.intQ05ChargesBalance(balance.balance())) {
                continue;
            }
            BigDecimal interest = InterestFormula.monthlyInterest(
                    balance.balance(), yearlyRate, policy.intQ01InterestRoundingMode());
            if (policy.intQ06WritesInterest(interest)) {
                intH05Fits(interest, LARGEST_INTEREST, "INTEREST OF A BALANCE");
                totalInterest = totalInterest.add(interest).setScale(2, RoundingMode.UNNECESSARY);
                intH05Fits(totalInterest, LARGEST_INTEREST, "INTEREST OF AN ACCOUNT");
                transactions.add(interestTransaction(account, cardNumber, interest));
            }
            transactions.addAll(FeeHook.intH01FeesAfterInterestOf(balance));
        }

        return new AccountInterest(item.accountId(), transactions, newBalanceAtBreak(item, account, totalInterest));
    }

    /**
     * INT-R07 at the account break; INT-Q04: as coded, the account that ends the input is not updated.
     * An account left out by INT-H04 is not touched.
     */
    private Optional<BigDecimal> newBalanceAtBreak(AccountBalances item, Account account, BigDecimal totalInterest) {
        if (account == null || !policy.intH04ChargesAccount(account.activeStatus())
                || (item.endsInput() && !policy.intQ04UpdatesAccountEndingInput())) {
            return Optional.empty();
        }
        BigDecimal newBalance = account.currentBalance().add(totalInterest).setScale(2, RoundingMode.UNNECESSARY);
        intH05Fits(newBalance, LARGEST_BALANCE, "ACCOUNT BALANCE");
        return Optional.of(newBalance);
    }

    /**
     * INT-H05 (standards S3.4): a value is checked against its field before it is stored. As long
     * as the switch says STOP, a value that does not fit stops the run; no digit is ever cut off.
     */
    private void intH05Fits(BigDecimal value, BigDecimal largest, String what) {
        if (value.abs().compareTo(largest) > 0 && policy.intH05StopsWhenAValueDoesNotFit()) {
            throw overflow(what, "a value is larger than its field");
        }
    }

    private InterestRunStoppedException overflow(String what, String detail) {
        runLog.line(RunLogLines.valueDoesNotFit(what));
        return new InterestRunStoppedException(StopReason.OVERFLOW, detail);
    }

    /** INT-R08 (CBACT04C:372-391): the message with the account id, the failed read and status 23. */
    private Account accountOf(long accountId) {
        return accounts.find(accountId).orElseThrow(() -> {
            runLog.line(RunLogLines.accountNotFound(accountId));
            runLog.line(RunLogLines.ERROR_READING_ACCOUNT);
            runLog.line(RunLogLines.STATUS_RECORD_MISSING);
            return new InterestRunStoppedException(
                    StopReason.ACCOUNT_MISSING, "a balance names an account that is not on the account file");
        });
    }

    /**
     * INT-R06: the card number of the account. No card is INT-R08 (CBACT04C:393-413). Several
     * cards are INT-H03: the run stops (proposed default) or takes the lowest card number.
     */
    private String cardNumberOf(long accountId) {
        List<String> cardNumbers = cards.cardNumbersOf(accountId);
        if (cardNumbers.isEmpty()) {
            runLog.line(RunLogLines.accountNotFound(accountId));
            runLog.line(RunLogLines.ERROR_READING_CARD);
            runLog.line(RunLogLines.STATUS_RECORD_MISSING);
            throw new InterestRunStoppedException(StopReason.CARD_MISSING, "an account has no card cross-reference");
        }
        if (cardNumbers.size() == 1) {
            return cardNumbers.get(0);
        }
        if (policy.intH03TakesTheLowestCardNumber()) {
            return Collections.min(cardNumbers);
        }
        runLog.line(RunLogLines.severalCards(accountId));
        throw new InterestRunStoppedException(StopReason.MULTI_CARD, "an account has several cards");
    }

    /**
     * INT-R02: the rate of the account's group, the balance's type and category. INT-R03
     * (CBACT04C:416-439): when the group has none, the run log says so and the rate of the group
     * DEFAULT for the same type and category is used.
     */
    private BigDecimal rateFor(Account account, CategoryBalance balance) {
        return rates.findRate(account.groupId(), balance.typeCode(), balance.categoryCode())
                .orElseGet(() -> defaultRateFor(balance));
    }

    private BigDecimal defaultRateFor(CategoryBalance balance) {
        runLog.line(RunLogLines.GROUP_RATE_MISSING);
        runLog.line(RunLogLines.TRYING_DEFAULT_GROUP);
        // INT-R08 (CBACT04C:443-460): no DEFAULT rate either - the failed read and status 23.
        return rates.findRate(DEFAULT_GROUP, balance.typeCode(), balance.categoryCode()).orElseThrow(() -> {
            runLog.line(RunLogLines.ERROR_READING_DEFAULT_RATE);
            runLog.line(RunLogLines.STATUS_RECORD_MISSING);
            return new InterestRunStoppedException(StopReason.DEFAULT_RATE_MISSING,
                    "neither the account's group nor the group DEFAULT has a rate for a balance");
        });
    }

    /** INT-R06, with the id of INT-Q02 and the timestamps of INT-Q03; the clock is read once per transaction. */
    private InterestTransaction interestTransaction(Account account, String cardNumber, BigDecimal interest) {
        // INT-H05 (CBACT04C:474): transaction 1,000,000 of a run has no six-digit number.
        if (counter.nextNeedsSevenDigits() && policy.intH05StopsWhenAValueDoesNotFit()) {
            throw overflow("TRANSACTION NUMBER", "a run has more transactions than its six-digit counter holds");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String timestamp = policy.intQ03StampsTheCycleDate() ? RunTimestamp.ofCycleDate(parmDate) : RunTimestamp.format(now);
        return new InterestTransaction(
                policy.intQ02IdPrefix(parmDate, now) + counter.next(),
                INTEREST_TYPE_CODE,
                INTEREST_CATEGORY_CODE,
                INTEREST_SOURCE,
                DESCRIPTION_PREFIX + String.format("%011d", account.accountId()),
                interest,
                NO_MERCHANT,
                cardNumber,
                timestamp,
                timestamp);
    }
}
