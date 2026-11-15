package com.carddemo.interest.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * ports (FI-3) and the policy only. Rules INT-R01 to R07, the open decisions INT-Q01 to Q06 and
 * INT-H04, each as coded behind its switch. No fee is charged (INT-H01: the legacy fee paragraph,
 * CBACT04C:518-520, is empty and is not ported).
 */
public final class InterestCalculator {

    /** INT-R03 (CBACT04C:437): the group whose rate applies when the account's group has none. */
    private static final String DEFAULT_GROUP = "DEFAULT";
    /** INT-R06 (CBACT04C:482-491). */
    private static final String INTEREST_TYPE_CODE = "01";
    private static final int INTEREST_CATEGORY_CODE = 5;
    private static final String INTEREST_SOURCE = "System";
    private static final String DESCRIPTION_PREFIX = "Int. for a/c ";
    private static final long NO_MERCHANT = 0L;

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
            if (!isCharged(account)) {
                continue;
            }
            Optional<BigDecimal> interest = interestOn(account, read.balance());
            if (interest.isPresent()) {
                totalInterest = stored(totalInterest.add(interest.get()).setScale(2, RoundingMode.UNNECESSARY),
                        FieldRange.INTEREST_INTEGER_DIGITS, "the interest total of an account");
                transactions.add(interestTransaction(account, cardNumber, interest.get()));
            }
        }

        return new AccountInterest(item.accountId(), transactions, newBalanceAtBreak(item, account, totalInterest));
    }

    /**
     * INT-H05 (standards S3.4): a value is checked against its field before it is stored. As
     * proposed the run stops; the value is never cut to fit.
     */
    private BigDecimal stored(BigDecimal amount, int integerDigits, String what) {
        if (!FieldRange.fits(amount, integerDigits) && policy.intH05StopsOnOverflow()) {
            throw RunStop.of(runLog, StopReason.OVERFLOW, what + " does not fit its field");
        }
        return amount;
    }

    /** INT-H04: as coded every account with a balance is charged, whatever its active flag says. */
    private boolean isCharged(Account account) {
        return account.isActive() || policy.intH04ChargesInactiveAccounts();
    }

    /**
     * The interest decision of one balance (INT-R01): empty when no interest transaction is due.
     * INT-R04 (CBACT04C:214-217): a zero rate means no interest and no transaction. INT-Q05 and
     * INT-Q06: as coded nothing else is tested, so a credit balance and an interest of 0.00 are posted.
     */
    private Optional<BigDecimal> interestOn(Account account, CategoryBalance balance) {
        BigDecimal yearlyRate = rateFor(account.groupId(), balance);
        if (yearlyRate.signum() == 0) {
            return Optional.empty();
        }
        if (balance.balance().signum() < 0 && !policy.intQ05ChargesCreditBalance()) {
            return Optional.empty();
        }
        BigDecimal interest = stored(InterestFormula.monthlyInterest(
                balance.balance(), yearlyRate, policy.intQ01InterestRoundingMode()),
                FieldRange.INTEREST_INTEGER_DIGITS, "the interest on a balance");
        if (interest.signum() == 0 && !policy.intQ06WritesZeroInterest()) {
            return Optional.empty();
        }
        return Optional.of(interest);
    }

    /**
     * INT-R02: the rate of the account's group for the balance's type and category. INT-R03
     * (CBACT04C:416-420, 436-444): when that group has none, the run log says so and the rate of
     * the group DEFAULT for the same type and category applies.
     */
    private BigDecimal rateFor(String groupId, CategoryBalance balance) {
        Optional<BigDecimal> groupRate = rates.findRate(groupId, balance.typeCode(), balance.categoryCode());
        if (groupRate.isPresent()) {
            return groupRate.get();
        }
        runLog.line(RunLogLines.GROUP_RATE_MISSING);
        runLog.line(RunLogLines.TRY_DEFAULT_GROUP);
        return rates.findRate(DEFAULT_GROUP, balance.typeCode(), balance.categoryCode())
                .orElseThrow(() -> RunStop.of(runLog, StopReason.DEFAULT_RATE_MISSING,
                        "neither the account's group nor the group DEFAULT has a rate for a balance"));
    }

    /**
     * INT-R07 at the account break; INT-Q04: as coded, the account that ends the input is not updated.
     * An account left out by INT-H04 is not updated either.
     */
    private Optional<BigDecimal> newBalanceAtBreak(AccountBalances item, Account account, BigDecimal totalInterest) {
        if (account == null || !isCharged(account)
                || (item.endsInput() && !policy.intQ04UpdatesAccountEndingInput())) {
            return Optional.empty();
        }
        return Optional.of(stored(account.currentBalance().add(totalInterest).setScale(2, RoundingMode.UNNECESSARY),
                FieldRange.ACCOUNT_BALANCE_INTEGER_DIGITS, "the new balance of an account"));
    }

    private Account accountOf(long accountId) {
        return accounts.find(accountId).orElseThrow(() -> RunStop.recordMissing(runLog, StopReason.ACCOUNT_MISSING,
                accountId, "a balance names an account that is not on the account file"));
    }

    /**
     * INT-R06: the card number of the account. INT-H03: with several cards the run stops (proposed
     * default) or, when switched, the first in card-number order is taken - the port returns them
     * in that order.
     */
    private String cardNumberOf(long accountId) {
        List<String> cardNumbers = cards.cardNumbersOf(accountId);
        if (cardNumbers.isEmpty()) {
            throw RunStop.recordMissing(runLog, StopReason.CARD_MISSING, accountId,
                    "an account has no card cross-reference");
        }
        if (cardNumbers.size() > 1 && policy.intH03StopsOnSeveralCards()) {
            throw RunStop.of(runLog, StopReason.MULTI_CARD, "an account has several cards");
        }
        return cardNumbers.get(0);
    }

    /** INT-R06, with the id of INT-Q02 and the timestamps of INT-Q03; the clock is read once per transaction. */
    private InterestTransaction interestTransaction(Account account, String cardNumber, BigDecimal interest) {
        if (counter.isExhausted() && policy.intH05StopsOnOverflow()) {
            throw RunStop.of(runLog, StopReason.OVERFLOW, "the transaction counter does not fit the six digits of the id");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String timestamp = policy.intQ03StampsCycleDate()
                ? RunTimestamp.ofCycleDate(parmDate)
                : RunTimestamp.format(now);
        return new InterestTransaction(
                policy.intQ02IdPrefix(parmDate, now) + counter.next(),
                INTEREST_TYPE_CODE,
                INTEREST_CATEGORY_CODE,
                INTEREST_SOURCE,
                DESCRIPTION_PREFIX + AccountIdText.elevenDigits(account.accountId()),
                interest,
                NO_MERCHANT,
                cardNumber,
                timestamp,
                timestamp);
    }
}
