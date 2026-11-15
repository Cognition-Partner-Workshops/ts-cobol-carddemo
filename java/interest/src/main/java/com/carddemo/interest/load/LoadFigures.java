package com.carddemo.interest.load;

import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.ToLongFunction;

import com.carddemo.interest.jdbc.ReplicaTable;

/**
 * The figures of reconciliation RC1 (record counts) and RC2 (sums and the account-id hash totals)
 * of one load. They are taken once from the decoded files and once from the loaded tables; the two
 * must be equal, and the table side is what interest_run keeps.
 */
public record LoadFigures(
        long accountCount, long cardXrefCount, long tranCatBalanceCount, long disclosureGroupCount,
        BigDecimal acctCurrBalSum, BigDecimal acctCreditLimitSum, BigDecimal acctCashCreditLimitSum,
        BigDecimal acctCurrCycCreditSum, BigDecimal acctCurrCycDebitSum,
        BigDecimal tranCatBalSum, BigDecimal disIntRateSum,
        BigDecimal acctIdSum, BigDecimal xrefAcctIdSum, BigDecimal trancatAcctIdSum) {

    /** Builds the figures from one side: a count per table and a sum per table and copybook field. */
    public static LoadFigures of(ToLongFunction<ReplicaTable> count, BiFunction<ReplicaTable, String, BigDecimal> sum) {
        return new LoadFigures(
                count.applyAsLong(ReplicaTable.ACCOUNT), count.applyAsLong(ReplicaTable.CARD_XREF),
                count.applyAsLong(ReplicaTable.TRAN_CAT_BALANCE), count.applyAsLong(ReplicaTable.DISCLOSURE_GROUP),
                sum.apply(ReplicaTable.ACCOUNT, "ACCT-CURR-BAL"), sum.apply(ReplicaTable.ACCOUNT, "ACCT-CREDIT-LIMIT"),
                sum.apply(ReplicaTable.ACCOUNT, "ACCT-CASH-CREDIT-LIMIT"), sum.apply(ReplicaTable.ACCOUNT, "ACCT-CURR-CYC-CREDIT"),
                sum.apply(ReplicaTable.ACCOUNT, "ACCT-CURR-CYC-DEBIT"),
                sum.apply(ReplicaTable.TRAN_CAT_BALANCE, "TRAN-CAT-BAL"), sum.apply(ReplicaTable.DISCLOSURE_GROUP, "DIS-INT-RATE"),
                sum.apply(ReplicaTable.ACCOUNT, "ACCT-ID"), sum.apply(ReplicaTable.CARD_XREF, "XREF-ACCT-ID"),
                sum.apply(ReplicaTable.TRAN_CAT_BALANCE, "TRANCAT-ACCT-ID"));
    }

    /** One line for a test or trial-load report: aggregates only, no value of a single record. */
    public String describe() {
        return "RC1 account=" + accountCount + " card_xref=" + cardXrefCount + " tran_cat_balance=" + tranCatBalanceCount
                + " disclosure_group=" + disclosureGroupCount
                + " | RC2 acct_curr_bal=" + acctCurrBalSum + " acct_credit_limit=" + acctCreditLimitSum
                + " acct_cash_credit_limit=" + acctCashCreditLimitSum + " acct_curr_cyc_credit=" + acctCurrCycCreditSum
                + " acct_curr_cyc_debit=" + acctCurrCycDebitSum + " tran_cat_bal=" + tranCatBalSum
                + " dis_int_rate=" + disIntRateSum + " acct_id=" + acctIdSum + " xref_acct_id=" + xrefAcctIdSum
                + " trancat_acct_id=" + trancatAcctIdSum;
    }
}
