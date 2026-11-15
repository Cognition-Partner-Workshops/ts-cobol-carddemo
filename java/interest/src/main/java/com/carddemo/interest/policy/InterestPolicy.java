package com.carddemo.interest.policy;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The switches of the open decisions (architecture.md section 8, FI-5). The default of each is the
 * legacy behaviour. This is the only class that reads interest.policy.*; the pilot (ticket 01)
 * carries the four decisions it implements: INT-Q01, INT-Q02, INT-Q03, INT-Q04.
 */
@ConfigurationProperties("interest.policy")
public record InterestPolicy(
        Rounding rounding,
        IdPrefix idPrefix,
        TimestampSource timestampSource,
        boolean updateLastAccount) {

    /** INT-Q01. */
    public enum Rounding { TRUNCATE, HALF_UP, HALF_EVEN }

    /** INT-Q02. */
    public enum IdPrefix { PARM, RUN_DATE }

    /** INT-Q03. */
    public enum TimestampSource { RUN_CLOCK, CYCLE_DATE }

    private static final DateTimeFormatter RUN_DATE_PREFIX = DateTimeFormatter.ofPattern("yyyyMMdd'00'");

    public InterestPolicy {
        rounding = rounding == null ? Rounding.TRUNCATE : rounding;
        idPrefix = idPrefix == null ? IdPrefix.PARM : idPrefix;
        timestampSource = timestampSource == null ? TimestampSource.RUN_CLOCK : timestampSource;
        if (timestampSource == TimestampSource.CYCLE_DATE) {
            throw new IllegalArgumentException("interest.policy.timestampSource=CYCLE_DATE (INT-Q03) is not built: "
                    + "it needs a cycle-date job parameter that architecture.md section 4 does not list");
        }
    }

    /** The legacy behaviour of every switch. */
    public static InterestPolicy legacy() {
        return new InterestPolicy(Rounding.TRUNCATE, IdPrefix.PARM, TimestampSource.RUN_CLOCK, false);
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
     * moment the record is written.
     */
    public LocalDateTime intQ03TransactionMoment(LocalDateTime runClock) {
        return runClock;
    }

    /**
     * INT-Q04 (CBACT04C:188-222, 339-340): as coded, the account that ends the input gets its
     * transactions but no update.
     */
    public boolean intQ04UpdatesAccountEndingInput() {
        return updateLastAccount;
    }
}
