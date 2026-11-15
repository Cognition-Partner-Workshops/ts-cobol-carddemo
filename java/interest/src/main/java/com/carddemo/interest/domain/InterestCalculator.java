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
 * ports (FI-3) and the policy only. It covers INT-R01 to R07 and the open decisions INT-Q01 to
 * Q06, INT-H01 and INT-H04, each as coded behind its switch. The legacy messages of the stop
 * conditions (INT-R08) and INT-H03, INT-H05 are ticket 08.
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
                totalInterest = totalInterest.add(interest).setScale(2, RoundingMode.UNNECESSARY);
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
        return Optional.of(account.currentBalance().add(totalInterest).setScale(2, RoundingMode.UNNECESSARY));
    }

    private Account accountOf(long accountId) {
        return accounts.find(accountId).orElseThrow(() -> new InterestRunStoppedException(
                StopReason.ACCOUNT_MISSING, "a balance names an account that is not on the account file"));
    }

    /** INT-R06: the card number of the account. Several cards are INT-H03: the pilot stops (proposed default). */
    private String cardNumberOf(long accountId) {
        List<String> cardNumbers = cards.cardNumbersOf(accountId);
        if (cardNumbers.isEmpty()) {
            throw new InterestRunStoppedException(StopReason.CARD_MISSING, "an account has no card cross-reference");
        }
        if (cardNumbers.size() > 1) {
            throw new InterestRunStoppedException(StopReason.MULTI_CARD, "an account has several cards");
        }
        return cardNumbers.get(0);
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
        return rates.findRate(DEFAULT_GROUP, balance.typeCode(), balance.categoryCode())
                .orElseThrow(() -> new InterestRunStoppedException(StopReason.DEFAULT_RATE_MISSING,
                        "neither the account's group nor the group DEFAULT has a rate for a balance"));
    }

    /** INT-R06, with the id of INT-Q02 and the timestamps of INT-Q03; the clock is read once per transaction. */
    private InterestTransaction interestTransaction(Account account, String cardNumber, BigDecimal interest) {
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
