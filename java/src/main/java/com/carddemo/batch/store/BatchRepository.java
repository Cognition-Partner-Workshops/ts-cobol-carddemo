package com.carddemo.batch.store;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.CategoryBalance;
import com.carddemo.batch.domain.DailyTransaction;
import com.carddemo.batch.domain.PurchasePromotion;
import com.carddemo.batch.domain.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Keyed reads and rewrites, the way the programs use their VSAM files. */
@Repository
public class BatchRepository {

    private final JdbcTemplate jdbc;
    private final JsonlStore store;

    public BatchRepository(JdbcTemplate jdbc, JsonlStore store) {
        this.jdbc = jdbc;
        this.store = store;
    }

    public List<DailyTransaction> dailyTransactions() {
        return jdbc.query("SELECT * FROM daily_transaction ORDER BY seq", (rs, i) -> new DailyTransaction(
                rs.getLong("seq"), rs.getString("dalytran_id"), rs.getString("dalytran_type_cd"),
                rs.getInt("dalytran_cat_cd"), rs.getString("dalytran_source"), rs.getString("dalytran_desc"),
                rs.getBigDecimal("dalytran_amt"), rs.getLong("dalytran_merchant_id"),
                rs.getString("dalytran_merchant_name"), rs.getString("dalytran_merchant_city"),
                rs.getString("dalytran_merchant_zip"), rs.getString("dalytran_card_num"),
                rs.getString("dalytran_orig_ts"), rs.getString("dalytran_proc_ts")));
    }

    public Optional<CardXref> xrefByCard(String cardNumber) {
        return jdbc.query("SELECT * FROM card_xref WHERE xref_card_num = ?", (rs, i) -> xref(rs.getString(1),
                rs.getLong("xref_cust_id"), rs.getLong("xref_acct_id")), cardNumber).stream().findFirst();
    }

    public Optional<CardXref> xrefByAccount(long accountId) {
        return jdbc.query("SELECT * FROM card_xref WHERE xref_acct_id = ? ORDER BY xref_card_num",
                (rs, i) -> xref(rs.getString("xref_card_num"), rs.getLong("xref_cust_id"), rs.getLong("xref_acct_id")),
                accountId).stream().findFirst();
    }

    private static CardXref xref(String card, long cust, long acct) {
        return new CardXref(card, cust, acct);
    }

    public Optional<Account> account(long id) {
        return jdbc.query("SELECT * FROM account WHERE acct_id = ?", (rs, i) -> new Account(
                rs.getLong("acct_id"), rs.getBigDecimal("acct_curr_bal"), rs.getBigDecimal("acct_credit_limit"),
                rs.getString("acct_expiraion_date"), rs.getBigDecimal("acct_curr_cyc_credit"),
                rs.getBigDecimal("acct_curr_cyc_debit"), rs.getString("acct_group_id")), id).stream().findFirst();
    }

    public void rewrite(Account a) {
        jdbc.update("UPDATE account SET acct_curr_bal = ?, acct_curr_cyc_credit = ?, acct_curr_cyc_debit = ? WHERE acct_id = ?",
                a.currentBalance(), a.cycleCredit(), a.cycleDebit(), a.id());
    }

    public List<CategoryBalance> categoryBalancesInKeyOrder() {
        return jdbc.query("SELECT * FROM category_balance ORDER BY trancat_acct_id, trancat_type_cd, trancat_cd",
                (rs, i) -> new CategoryBalance(rs.getLong("trancat_acct_id"), rs.getString("trancat_type_cd"),
                        rs.getInt("trancat_cd"), rs.getBigDecimal("tran_cat_bal")));
    }

    /** Adds the amount to the category balance, creating the record at zero when it is missing. */
    public void addToCategoryBalance(long accountId, String typeCode, int categoryCode, BigDecimal amount) {
        int updated = jdbc.update("UPDATE category_balance SET tran_cat_bal = tran_cat_bal + ? "
                + "WHERE trancat_acct_id = ? AND trancat_type_cd = ? AND trancat_cd = ?", amount, accountId, typeCode, categoryCode);
        if (updated == 0) {
            jdbc.update("INSERT INTO category_balance VALUES (?, ?, ?, ?)", accountId, typeCode, categoryCode, amount);
        }
    }

    public int startPromotionCycle(long accountId) {
        int updated = jdbc.update("UPDATE promotion_cycle SET cycles_used = cycles_used + 1 "
                + "WHERE acct_id = ? AND promo_group = ?", accountId, PurchasePromotion.GROUP);
        if (updated == 0) {
            jdbc.update("INSERT INTO promotion_cycle (acct_id, promo_group, cycles_used) VALUES (?, ?, 1)",
                    accountId, PurchasePromotion.GROUP);
            return 1;
        }
        return promotionCyclesUsed(accountId);
    }

    public int promotionCyclesUsed(long accountId) {
        return jdbc.query("SELECT cycles_used FROM promotion_cycle WHERE acct_id = ? AND promo_group = ?",
                (rs, i) -> rs.getInt(1), accountId, PurchasePromotion.GROUP).stream().findFirst().orElse(0);
    }

    public Optional<BigDecimal> interestRate(String groupId, String typeCode, int categoryCode) {
        return jdbc.query("SELECT dis_int_rate FROM disclosure_group WHERE dis_acct_group_id = ? "
                        + "AND dis_tran_type_cd = ? AND dis_tran_cat_cd = ?", (rs, i) -> rs.getBigDecimal(1),
                groupId == null ? "" : groupId.stripTrailing(), typeCode, categoryCode).stream().findFirst();
    }

    public void write(Datasets table, Transaction t) {
        store.insert(table, Map.ofEntries(
                Map.entry("tran_id", t.id()), Map.entry("tran_type_cd", t.typeCode()),
                Map.entry("tran_cat_cd", t.categoryCode()), Map.entry("tran_source", t.source()),
                Map.entry("tran_desc", t.description()), Map.entry("tran_amt", t.amount()),
                Map.entry("tran_merchant_id", t.merchantId()), Map.entry("tran_merchant_name", t.merchantName()),
                Map.entry("tran_merchant_city", t.merchantCity()), Map.entry("tran_merchant_zip", t.merchantZip()),
                Map.entry("tran_card_num", t.cardNumber()), Map.entry("tran_orig_ts", t.originTimestamp()),
                Map.entry("tran_proc_ts", t.processTimestamp())));
    }

    public void reject(DailyTransaction d, int reason, String description) {
        jdbc.update("INSERT INTO rejected_transaction SELECT d.*, ?, ? FROM daily_transaction d WHERE d.seq = ?",
                reason, description, d.seq());
    }
}
