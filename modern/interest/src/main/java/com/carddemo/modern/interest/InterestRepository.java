package com.carddemo.modern.interest;

import com.carddemo.modern.interest.Records.Account;
import com.carddemo.modern.interest.Records.CardXref;
import com.carddemo.modern.interest.Records.DisclosureGroup;
import com.carddemo.modern.interest.Records.TranCatBal;
import com.carddemo.modern.interest.Records.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** PostgreSQL tables standing in for the INTCALC VSAM clusters and the SYSTRAN GDG. */
@Repository
public class InterestRepository {

    static final String TRAN_CAT_BAL_IN_KEY_ORDER =
            "SELECT acct_id, type_cd, cat_cd, balance, filler FROM interest.tran_cat_bal ORDER BY acct_id, type_cd, cat_cd";

    static final RowMapper<TranCatBal> TRAN_CAT_BAL = (rs, i) -> new TranCatBal(rs.getLong("acct_id"),
            rs.getString("type_cd"), rs.getInt("cat_cd"), rs.getBigDecimal("balance"), rs.getString("filler"));

    private static final RowMapper<Account> ACCOUNT = (rs, i) -> new Account(rs.getLong("acct_id"),
            rs.getString("active_status"), rs.getBigDecimal("curr_bal"), rs.getBigDecimal("credit_limit"),
            rs.getBigDecimal("cash_credit_limit"), rs.getString("open_date"), rs.getString("expiration_date"),
            rs.getString("reissue_date"), rs.getBigDecimal("curr_cyc_credit"), rs.getBigDecimal("curr_cyc_debit"),
            rs.getString("addr_zip"), rs.getString("group_id"), rs.getString("filler"));

    private final JdbcTemplate jdbc;

    public InterestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** DELETE + DEFINE CLUSTER for every INTCALC cluster and a fresh SYSTRAN generation. */
    public void clear() {
        jdbc.execute("TRUNCATE interest.account, interest.card_xref, interest.tran_cat_bal, "
                + "interest.disclosure_group, interest.systran");
    }

    public void insert(Account a) {
        jdbc.update("INSERT INTO interest.account VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)", a.acctId(), a.activeStatus(),
                a.currBal(), a.creditLimit(), a.cashCreditLimit(), a.openDate(), a.expirationDate(),
                a.reissueDate(), a.currCycCredit(), a.currCycDebit(), a.addrZip(), a.groupId(), a.filler());
    }

    public void insert(CardXref x) {
        jdbc.update("INSERT INTO interest.card_xref VALUES (?,?,?,?)", x.cardNum(), x.custId(), x.acctId(), x.filler());
    }

    public void insert(TranCatBal t) {
        jdbc.update("INSERT INTO interest.tran_cat_bal VALUES (?,?,?,?,?)", t.acctId(), t.typeCd(), t.catCd(),
                t.balance(), t.filler());
    }

    public void insert(DisclosureGroup d) {
        jdbc.update("INSERT INTO interest.disclosure_group VALUES (?,?,?,?,?)", d.groupId(), d.typeCd(), d.catCd(),
                d.intRate(), d.filler());
    }

    /** READ ACCOUNT-FILE by ACCT-ID. */
    public Optional<Account> findAccount(long acctId) {
        return jdbc.query("SELECT * FROM interest.account WHERE acct_id = ?", ACCOUNT, acctId).stream().findFirst();
    }

    /** READ XREF-FILE KEY IS FD-XREF-ACCT-ID: the AIX path returns the first card for the account. */
    public Optional<CardXref> findXrefByAccount(long acctId) {
        return jdbc.query("SELECT * FROM interest.card_xref WHERE acct_id = ? ORDER BY card_num LIMIT 1",
                (rs, i) -> new CardXref(rs.getString("card_num"), rs.getLong("cust_id"), rs.getLong("acct_id"),
                        rs.getString("filler")), acctId).stream().findFirst();
    }

    /** READ DISCGRP-FILE by DIS-GROUP-KEY. */
    public Optional<DisclosureGroup> findDisclosureGroup(String groupId, String typeCd, int catCd) {
        return jdbc.query("SELECT * FROM interest.disclosure_group WHERE group_id = ? AND type_cd = ? AND cat_cd = ?",
                (rs, i) -> new DisclosureGroup(rs.getString("group_id"), rs.getString("type_cd"), rs.getInt("cat_cd"),
                        rs.getBigDecimal("int_rate"), rs.getString("filler")), groupId, typeCd, catCd)
                .stream().findFirst();
    }

    /** REWRITE FD-ACCTFILE-REC FROM ACCOUNT-RECORD. */
    public void rewrite(Account a) {
        jdbc.update("UPDATE interest.account SET curr_bal = ?, curr_cyc_credit = ?, curr_cyc_debit = ? WHERE acct_id = ?",
                a.currBal(), a.currCycCredit(), a.currCycDebit(), a.acctId());
    }

    /** WRITE FD-TRANFILE-REC FROM TRAN-RECORD. */
    public void write(int seq, Transaction t) {
        jdbc.update("INSERT INTO interest.systran VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)", seq, t.tranId(), t.typeCd(),
                t.catCd(), t.source(), t.description(), t.amount(), t.merchantId(), t.merchantName(),
                t.merchantCity(), t.merchantZip(), t.cardNum(), t.origTs(), t.procTs());
    }

    /** Unload in key order, as IDCAMS REPRO of the KSDS would. */
    public List<Account> accountsInKeyOrder() {
        return jdbc.query("SELECT * FROM interest.account ORDER BY acct_id", ACCOUNT);
    }

    public List<Transaction> transactionsInWriteOrder() {
        return jdbc.query("SELECT * FROM interest.systran ORDER BY seq", (rs, i) -> new Transaction(
                rs.getString("tran_id"), rs.getString("type_cd"), rs.getInt("cat_cd"), rs.getString("source"),
                rs.getString("description"), rs.getBigDecimal("amount"), rs.getLong("merchant_id"),
                rs.getString("merchant_name"), rs.getString("merchant_city"), rs.getString("merchant_zip"),
                rs.getString("card_num"), rs.getString("orig_ts"), rs.getString("proc_ts")));
    }
}
