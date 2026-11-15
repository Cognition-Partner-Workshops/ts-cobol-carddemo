package com.carddemo.interest.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.BooleanSupplier;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * The switches of the open decisions (architecture.md section 8, FI-5). The default of each is the
 * legacy behaviour. This is the only class that reads interest.policy.*. It carries the decisions
 * of the calculation: INT-Q01 to INT-Q06 and INT-H04, and the switches of the two holes the source
 * does not determine, multiCard (INT-H03) and overflow (INT-H05), whose decisions are made in
 * domain.HoleDecisions. INT-H01 has no switch (the fee hook is empty); rerunGuard (INT-H02) belongs
 * to ticket 06.
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

    /** INT-H03. STOP is the proposed default: the source does not determine the behaviour. */
    public enum MultiCard { STOP, FIRST_BY_CARD_NUM }

    /**
     * INT-H05. STOP is the proposed default: the source does not determine the behaviour. WIDEN
     * (architecture.md section 8) needs wider columns and record layouts (FI-1, FI-4) and is not built.
     */
    public enum Overflow { STOP }

    private static final DateTimeFormatter RUN_DATE_PREFIX = DateTimeFormatter.ofPattern("yyyyMMdd'00'");

    /** The constructor the configuration binds: every switch by its name. */
    @ConstructorBinding
    public InterestPolicy {
        rounding = rounding == null ? Rounding.TRUNCATE : rounding;
        idPrefix = idPrefix == null ? IdPrefix.PARM : idPrefix;
        timestampSource = timestampSource == null ? TimestampSource.RUN_CLOCK : timestampSource;
        negativeBalance = negativeBalance == null ? NegativeBalance.CHARGE : negativeBalance;
        zeroInterest = zeroInterest == null ? ZeroInterest.WRITE : zeroInterest;
        inactiveAccounts = inactiveAccounts == null ? InactiveAccounts.CHARGE : inactiveAccounts;
        multiCard = multiCard == null ? MultiCard.STOP : multiCard;
        overflow = overflow == null ? Overflow.STOP : overflow;
    }

    /** The switches of the calculation, with multiCard and overflow at their default STOP. */
    public InterestPolicy(Rounding rounding, IdPrefix idPrefix, TimestampSource timestampSource,
            boolean updateLastAccount, NegativeBalance negativeBalance, ZeroInterest zeroInterest,
            InactiveAccounts inactiveAccounts) {
        this(rounding, idPrefix, timestampSource, updateLastAccount, negativeBalance, zeroInterest,
                inactiveAccounts, MultiCard.STOP, Overflow.STOP);
    }

    /** The legacy behaviour of every switch. */
    public static InterestPolicy legacy() {
        return new InterestPolicy(Rounding.TRUNCATE, IdPrefix.PARM, TimestampSource.RUN_CLOCK, false,
                NegativeBalance.CHARGE, ZeroInterest.WRITE, InactiveAccounts.CHARGE);
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
     * INT-Q03 (CBACT04C:496-498, 613-624): both timestamps of a transaction are the clock at the
     * moment the record is written. CYCLE_DATE is option B of the rule: the cycle date of the PARM
     * (its first eight characters, YYYYMMDD) at 00.00.00.
     */
    public LocalDateTime intQ03TransactionMoment(LocalDateTime runClock, String parmDate) {
        return switch (timestampSource) {
            case RUN_CLOCK -> runClock;
            case CYCLE_DATE -> intQ03CycleDate(parmDate).atStartOfDay();
        };
    }

    private static LocalDate intQ03CycleDate(String parmDate) {
        try {
            return LocalDate.parse(parmDate.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("interest.policy.timestampSource=CYCLE_DATE (INT-Q03) needs a PARM "
                    + "that starts with a date YYYYMMDD", e);
        }
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
    public boolean intQ05ChargesBalance(BigDecimal balance) {
        return negativeBalance == NegativeBalance.CHARGE || balance.signum() >= 0;
    }

    /**
     * INT-Q06 (CBACT04C:214-217, 464-468): as coded the computed amount is not tested, so an
     * interest of 0.00 is still written. SKIP writes nothing for it.
     */
    public boolean intQ06WritesInterest(BigDecimal interest) {
        return zeroInterest == ZeroInterest.WRITE || interest.signum() != 0;
    }

    /**
     * INT-H04 (CBACT04C:202-203, 372-376): as coded the active flag of the account is never read,
     * so every account with a balance is charged. Under SKIP an account is charged only when its
     * flag is Y; {@code active} is asked for only then.
     */
    public boolean intH04ChargesAccount(BooleanSupplier active) {
        return inactiveAccounts == InactiveAccounts.CHARGE || active.getAsBoolean();
    }
}
