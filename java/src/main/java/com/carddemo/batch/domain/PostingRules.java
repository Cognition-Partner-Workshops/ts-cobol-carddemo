package com.carddemo.batch.domain;

import java.math.BigDecimal;
import java.util.Optional;

/** Validation and balance rules of CBTRN02C (POSTTRAN). */
public final class PostingRules {

    private PostingRules() {
    }

    /**
     * Returns the reject reason, or empty when the transaction posts. The card and account
     * lookups stop at the first miss; the limit and expiration checks both run and the
     * expiration reason, checked last, wins when both fail.
     */
    public static Optional<RejectReason> validate(DailyTransaction tran, Optional<CardXref> xref,
                                                  Optional<Account> account) {
        if (xref.isEmpty()) {
            return Optional.of(RejectReason.INVALID_CARD);
        }
        if (account.isEmpty()) {
            return Optional.of(RejectReason.ACCOUNT_NOT_FOUND);
        }
        Account acct = account.get();
        RejectReason reason = null;
        BigDecimal projected = acct.cycleCredit().subtract(acct.cycleDebit()).add(tran.amount());
        if (acct.creditLimit().compareTo(projected) < 0) {
            reason = RejectReason.OVER_LIMIT;
        }
        if (acct.expirationDate().compareTo(tran.originDate()) < 0) {
            reason = RejectReason.AFTER_EXPIRATION;
        }
        return Optional.ofNullable(reason);
    }

    /** Positive amounts go to cycle credit, negative ones to cycle debit; both move the balance. */
    public static Account post(Account account, BigDecimal amount) {
        BigDecimal credit = account.cycleCredit();
        BigDecimal debit = account.cycleDebit();
        if (amount.signum() >= 0) {
            credit = credit.add(amount);
        } else {
            debit = debit.add(amount);
        }
        return account.withBalances(account.currentBalance().add(amount), credit, debit);
    }
}
