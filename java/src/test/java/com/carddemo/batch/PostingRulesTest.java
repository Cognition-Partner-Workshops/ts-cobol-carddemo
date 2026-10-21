package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.DailyTransaction;
import com.carddemo.batch.domain.PostingRules;
import com.carddemo.batch.domain.RejectReason;
import org.junit.jupiter.api.Test;

class PostingRulesTest {

    private static final CardXref XREF = new CardXref("4000002610000053", 953, 53);

    private static Account account(String limit, String credit, String expires) {
        return new Account(53, new BigDecimal(credit), new BigDecimal(limit), expires, new BigDecimal(credit),
                new BigDecimal("0.00"), "A000000000");
    }

    private static DailyTransaction tran(String amount, String day) {
        return new DailyTransaction(1, "2610000000000001", "01", 1, "POS TERM", "Purchase", new BigDecimal(amount),
                1L, "m", "c", "z", XREF.cardNumber(), day + " 10:00:00.000000", "");
    }

    @Test
    void unknownCardIsRejected100() {
        assertThat(PostingRules.validate(tran("1.00", "2022-07-17"), Optional.empty(), Optional.empty()))
                .contains(RejectReason.INVALID_CARD);
    }

    @Test
    void cardWithoutAccountIsRejected101() {
        assertThat(PostingRules.validate(tran("1.00", "2022-07-17"), Optional.of(XREF), Optional.empty()))
                .contains(RejectReason.ACCOUNT_NOT_FOUND);
    }

    @Test
    void limitEqualToProjectedCycleTotalStillPosts() {
        Account a = account("500.00", "451.37", "2026-09-30");
        assertThat(PostingRules.validate(tran("48.63", "2022-07-17"), Optional.of(XREF), Optional.of(a))).isEmpty();
        assertThat(PostingRules.validate(tran("48.64", "2022-07-17"), Optional.of(XREF), Optional.of(a)))
                .contains(RejectReason.OVER_LIMIT);
    }

    @Test
    void expirationWinsWhenBothChecksFail() {
        Account a = account("100.00", "90.00", "2022-05-31");
        assertThat(PostingRules.validate(tran("50.00", "2022-07-17"), Optional.of(XREF), Optional.of(a)))
                .contains(RejectReason.AFTER_EXPIRATION);
    }

    @Test
    void transactionOnTheExpirationDayPosts() {
        Account a = account("3000.00", "0.00", "2022-06-30");
        assertThat(PostingRules.validate(tran("12.50", "2022-06-30"), Optional.of(XREF), Optional.of(a))).isEmpty();
    }

    @Test
    void negativeAmountsGoToCycleDebit() {
        Account posted = PostingRules.post(account("500.00", "10.00", "2026-09-30"), new BigDecimal("-4.00"));
        assertThat(posted.cycleDebit()).isEqualByComparingTo("-4.00");
        assertThat(posted.currentBalance()).isEqualByComparingTo("6.00");
    }
}
