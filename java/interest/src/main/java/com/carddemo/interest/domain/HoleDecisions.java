package com.carddemo.interest.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.carddemo.interest.policy.InterestPolicy;
import com.carddemo.interest.policy.InterestPolicy.MultiCard;
import com.carddemo.interest.policy.InterestPolicy.Overflow;

/**
 * The two holes the source does not determine, each in one place (standards S5.1): INT-H03, an
 * account with several cards (switch multiCard), and INT-H05, a value that does not fit its field
 * (switch overflow). The switches are interest.policy.multiCard and interest.policy.overflow, read
 * through InterestPolicy. The proposed default of both is STOP; both decisions are OPEN, and what
 * CBACT04C does on the host in either case is unknown.
 */
public record HoleDecisions(MultiCard multiCard, Overflow overflow) {

    /** TRAN-ID holds six digits of the counter (CVTRA05Y/TRAN-ID, CBACT04C:173, 474). */
    private static final int LAST_TRANSACTION_OF_A_RUN = 999_999;

    public HoleDecisions {
        multiCard = multiCard == null ? MultiCard.STOP : multiCard;
        overflow = overflow == null ? Overflow.STOP : overflow;
    }

    /** The proposed default of both switches. */
    public static HoleDecisions stop() {
        return new HoleDecisions(MultiCard.STOP, Overflow.STOP);
    }

    /** The decisions as interest.policy.multiCard and interest.policy.overflow set them. */
    public static HoleDecisions of(InterestPolicy policy) {
        return new HoleDecisions(policy.multiCard(), policy.overflow());
    }

    /**
     * INT-H03 (CBACT04C:34-39, 393-404): the card number that goes on the interest transaction of
     * an account with these cards, given in card-number order. Empty when the run has to stop:
     * several cards under STOP. FIRST_BY_CARD_NUM takes the lowest card number.
     */
    public Optional<String> intH03CardNumber(List<String> cardNumbersInOrder) {
        if (cardNumbersInOrder.size() > 1 && multiCard == MultiCard.STOP) {
            return Optional.empty();
        }
        return Optional.of(cardNumbersInOrder.get(0));
    }

    /**
     * INT-H05 (CBACT04C:168-169, 352, 464-467): whether a signed amount with two decimals fits a
     * field with this many integer digits. Under STOP a value that does not fit stops the run;
     * its high-order digits are never cut off.
     */
    public boolean intH05Fits(BigDecimal amount, int integerDigits) {
        return amount.abs().compareTo(BigDecimal.TEN.pow(integerDigits)) < 0;
    }

    /** INT-H05 (CBACT04C:173, 474): whether the run can issue one more transaction id. */
    public boolean intH05HasNextTransaction(TransactionCounter counter) {
        return counter.issued() < LAST_TRANSACTION_OF_A_RUN;
    }
}
