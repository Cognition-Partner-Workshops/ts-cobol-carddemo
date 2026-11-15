package com.carddemo.interest.policy;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The switches of the open decisions (architecture.md section 8, FI-5). The default of each is the
 * legacy behaviour. This is the only class that reads interest.policy.*. It carries the decisions
 * of the calculation: INT-Q01 to INT-Q06, INT-H03, INT-H04 and INT-H05 (tickets 01, 04 and 08).
 */
@ConfigurationProperties("interest.policy")
public record InterestPolicy(
        Rounding rounding,
        IdPrefix idPrefix,
        TimestampSource timestampSource,
        boolean updateLastAccount,
        NegativeBalance negativeBalance,
        ZeroInterest zeroInterest,
        InactiveAccounts inactiveAccounts,
        MultiCard multiCard,
        Overflow overflow) {

    /** INT-Q01. */
    public enum Rounding { TRUNCATE, HALF_UP, HALF_EVEN }

    /** INT-Q02. */
    public enum IdPrefix { PARM, RUN_DATE }

    /** INT-Q03. */
    public enum TimestampSource { RUN_CLOCK, CYCLE_DATE }

    /** INT-Q05. */
    public enum NegativeBalance { CHARGE, SKIP }

    /** INT-Q06. */
    public enum ZeroInterest { WRITE, SKIP }

    /** INT-H04. */
    public enum InactiveAccounts { CHARGE, SKIP }

    /** INT-H03. */
    public enum MultiCard { STOP, FIRST_BY_CARD_NUM }

    /** INT-H05. */
    public enum Overflow { STOP, WIDEN }

    private static final DateTimeFormatter RUN_DATE_PREFIX = DateTimeFormatter.ofPattern("yyyyMMdd'00'");

    public InterestPolicy {
        rounding = rounding == null ? Rounding.TRUNCATE : rounding;
        idPrefix = idPrefix == null ? IdPrefix.PARM : idPrefix;
        timestampSource = timestampSource == null ? TimestampSource.RUN_CLOCK : timestampSource;
        negativeBalance = negativeBalance == null ? NegativeBalance.CHARGE : negativeBalance;
        zeroInterest = zeroInterest == null ? ZeroInterest.WRITE : zeroInterest;
        inactiveAccounts = inactiveAccounts == null ? InactiveAccounts.CHARGE : inactiveAccounts;
        multiCard = multiCard == null ? MultiCard.STOP : multiCard;
        overflow = overflow == null ? Overflow.STOP : overflow;
        if (overflow == Overflow.WIDEN) {
            throw new IllegalArgumentException("interest.policy.overflow=WIDEN (INT-H05) is not built: it needs wider "
                    + "fields than the frozen record layouts (FI-1) and schema (FI-4) have");
        }
    }

    /** The legacy behaviour of every switch. */
    public static InterestPolicy legacy() {
        return new InterestPolicy(Rounding.TRUNCATE, IdPrefix.PARM, TimestampSource.RUN_CLOCK, false,
                NegativeBalance.CHARGE, ZeroInterest.WRITE, InactiveAccounts.CHARGE, MultiCard.STOP, Overflow.STOP);
    }

    /**
     * INT-Q01 (CBACT04C:464-465): COMPUTE without ROUNDED cuts the interest off at the cent,
     * towards zero. TRUNCATE is RoundingMode.DOWN.
     */
    public RoundingMode intQ01InterestRoundingMode() {
        return switch (rounding) {
            case TRUNCATE -> RoundingMode.DOWN;
            case HALF_UP -> RoundingMode.HALF_UP;
            case HALF_EVEN -> RoundingMode.HALF_EVEN;
        };
    }

    /**
     * INT-Q02 (CBACT04C:476-480): the first ten characters of the transaction id are the PARM text
     * of the job, used as text. RUN_DATE is the date of the run followed by 00.
     */
    public String intQ02IdPrefix(String parmDate, LocalDateTime runClock) {
        return switch (idPrefix) {
            case PARM -> parmDate;
            case RUN_DATE -> RUN_DATE_PREFIX.format(runClock);
        };
    }

    /**
     * INT-Q03 (CBACT04C:496-498, 613-624): as coded, both timestamps of a transaction are the clock
     * at the moment the record is written. CYCLE_DATE stamps the cycle date of the PARM at 00.00.00
     * instead.
     */
    public boolean intQ03StampsCycleDate() {
        return timestampSource == TimestampSource.CYCLE_DATE;
    }

    /**
     * INT-Q04 (CBACT04C:188-222, 339-340): as coded, the account that ends the input gets its
     * transactions but no update.
     */
    public boolean intQ04UpdatesAccountEndingInput() {
        return updateLastAccount;
    }

    /**
     * INT-Q05 (CBACT04C:214-217, 464-467): as coded only the rate is tested, never the sign of the
     * balance, so a credit balance earns negative interest. SKIP gives it no interest and no transaction.
     */
    public boolean intQ05ChargesCreditBalance() {
        return negativeBalance == NegativeBalance.CHARGE;
    }

    /**
     * INT-Q06 (CBACT04C:214-217, 464-468): as coded an interest of 0.00 at a non-zero rate is still
     * written. SKIP writes nothing for it.
     */
    public boolean intQ06WritesZeroInterest() {
        return zeroInterest == ZeroInterest.WRITE;
    }

    /**
     * INT-H04 (CBACT04C:202-203, 372-376): as coded no statement reads the active flag, so every
     * account with a balance is charged. SKIP leaves an account that is not active untouched.
     */
    public boolean intH04ChargesInactiveAccounts() {
        return inactiveAccounts == InactiveAccounts.CHARGE;
    }

    /**
     * INT-H03 (CBACT04C:34-39, 393-404; G08): the source does not determine which card an account
     * with several cards gets, or whether the run stops. STOP (proposed default) stops the run;
     * FIRST_BY_CARD_NUM takes the lowest card number.
     */
    public boolean intH03StopsOnSeveralCards() {
        return multiCard == MultiCard.STOP;
    }

    /**
     * INT-H05 (CBACT04C:464-467, 474): the source does not determine what an amount or counter
     * larger than its field becomes. STOP (proposed default) stops the run; nothing is cut.
     * WIDEN is refused when the policy is built.
     */
    public boolean intH05StopsOnOverflow() {
        return overflow == Overflow.STOP;
    }
}
