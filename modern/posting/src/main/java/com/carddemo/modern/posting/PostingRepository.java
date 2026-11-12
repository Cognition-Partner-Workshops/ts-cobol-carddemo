package com.carddemo.modern.posting;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.carddemo.modern.codec.Layouts;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** KSDS keys are relational primary keys; record templates retain untouched fields and padding. */
@Repository
public class PostingRepository {
    private final JdbcTemplate jdbc;

    public PostingRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    void clear() {
        jdbc.execute("TRUNCATE posting.account, posting.card_xref, posting.tran_cat_bal, "
                + "posting.transaction_type, posting.transaction_category, posting.disclosure_group, "
                + "posting.transaction_master, posting.daily_transaction, posting.reject, posting.report_input");
    }

    void account(Map<String, Object> r) {
        jdbc.update("INSERT INTO posting.account VALUES (?,?,?,?,?,?,?) ON CONFLICT(acct_id) DO UPDATE SET "
                        + "record=excluded.record,curr_bal=excluded.curr_bal,credit_limit=excluded.credit_limit,"
                        + "cyc_credit=excluded.cyc_credit,cyc_debit=excluded.cyc_debit,expiration=excluded.expiration",
                r.get("ACCT-ID"), Layouts.ACCOUNT.encode(r), r.get("ACCT-CURR-BAL"), r.get("ACCT-CREDIT-LIMIT"),
                r.get("ACCT-CURR-CYC-CREDIT"), r.get("ACCT-CURR-CYC-DEBIT"), r.get("ACCT-EXPIRAION-DATE"));
    }

    Map<String, Object> account(long id) {
        List<String> rows = jdbc.queryForList("SELECT record FROM posting.account WHERE acct_id=?", String.class, id);
        return rows.isEmpty() ? null : Layouts.ACCOUNT.decode(rows.getFirst(), true);
    }

    void rewriteAccount(Map<String,Object> r) {
        jdbc.update("UPDATE posting.account SET record=?,curr_bal=?,credit_limit=?,cyc_credit=?,cyc_debit=?,expiration=? WHERE acct_id=?",
                Layouts.ACCOUNT.encode(r), r.get("ACCT-CURR-BAL"), r.get("ACCT-CREDIT-LIMIT"),
                r.get("ACCT-CURR-CYC-CREDIT"), r.get("ACCT-CURR-CYC-DEBIT"), r.get("ACCT-EXPIRAION-DATE"), r.get("ACCT-ID"));
    }

    Long accountForCard(String card) {
        List<Long> rows = jdbc.queryForList("SELECT acct_id FROM posting.card_xref WHERE card_num=?", Long.class, card);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    Map<String, Object> category(long id, String type, int cat) {
        List<String> rows = jdbc.queryForList("SELECT record FROM posting.tran_cat_bal WHERE acct_id=? AND type_cd=? AND cat_cd=?",
                String.class, id, type, cat);
        return rows.isEmpty() ? null : Layouts.TRAN_CAT_BAL.decode(rows.getFirst(), true);
    }

    void category(Map<String, Object> r) {
        jdbc.update("INSERT INTO posting.tran_cat_bal VALUES (?,?,?,?,?) ON CONFLICT(acct_id,type_cd,cat_cd) "
                        + "DO UPDATE SET balance=excluded.balance,record=excluded.record",
                r.get("TRANCAT-ACCT-ID"), r.get("TRANCAT-TYPE-CD"), r.get("TRANCAT-CD"),
                r.get("TRAN-CAT-BAL"), Layouts.TRAN_CAT_BAL.encode(r));
    }

    void postCategory(Map<String,Object> r, boolean create) {
        if (create) {
            jdbc.update("INSERT INTO posting.tran_cat_bal VALUES (?,?,?,?,?)", r.get("TRANCAT-ACCT-ID"),
                    r.get("TRANCAT-TYPE-CD"), r.get("TRANCAT-CD"), r.get("TRAN-CAT-BAL"), Layouts.TRAN_CAT_BAL.encode(r));
        } else {
            int updated = jdbc.update("UPDATE posting.tran_cat_bal SET balance=?,record=? WHERE acct_id=? AND type_cd=? AND cat_cd=?",
                    r.get("TRAN-CAT-BAL"), Layouts.TRAN_CAT_BAL.encode(r), r.get("TRANCAT-ACCT-ID"),
                    r.get("TRANCAT-TYPE-CD"), r.get("TRANCAT-CD"));
            if (updated == 0) throw new org.springframework.dao.EmptyResultDataAccessException(1);
        }
    }

    void transaction(Map<String, Object> r) {
        jdbc.update("INSERT INTO posting.transaction_master VALUES (?,?,?,?)", r.get("TRAN-ID"),
                r.get("TRAN-CARD-NUM"), r.get("TRAN-PROC-TS").toString().substring(0,10), Layouts.TRAN.encode(r));
    }

    String typeDescription(String type) {
        return jdbc.queryForObject("SELECT description FROM posting.transaction_type WHERE type_cd=?", String.class, type);
    }

    String categoryDescription(String type, int cat) {
        return jdbc.queryForObject("SELECT description FROM posting.transaction_category WHERE type_cd=? AND cat_cd=?",
                String.class, type, cat);
    }

    List<String> records(String table, String order) {
        return jdbc.queryForList("SELECT record FROM posting." + table + " ORDER BY " + order, String.class);
    }

    JdbcTemplate jdbc() { return jdbc; }

    static BigDecimal number(Map<String, Object> r, String key) { return (BigDecimal) r.get(key); }
}
