package com.carddemo.posting.store;

import com.carddemo.posting.record.Records.Account;
import com.carddemo.posting.record.Records.CardXref;
import com.carddemo.posting.record.Records.TranCatBalance;
import com.carddemo.posting.record.Records.TranCategory;
import com.carddemo.posting.record.Records.TranType;
import com.carddemo.posting.record.Records.Transaction;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The posting chain's VSAM clusters, kept as PostgreSQL tables in the {@code posting} schema. */
@Repository
public class PostingStore {

    private static final RowMapper<Account> ACCOUNT = (rs, n) -> new Account(rs.getLong("acct_id"),
            rs.getString("active_status"), rs.getBigDecimal("curr_bal"), rs.getBigDecimal("credit_limit"),
            rs.getBigDecimal("cash_credit_limit"), rs.getString("open_date"), rs.getString("expiration_date"),
            rs.getString("reissue_date"), rs.getBigDecimal("curr_cyc_credit"), rs.getBigDecimal("curr_cyc_debit"),
            rs.getString("addr_zip"), rs.getString("group_id"));
    private static final RowMapper<CardXref> XREF = (rs, n) -> new CardXref(rs.getString("card_num"),
            rs.getLong("cust_id"), rs.getLong("acct_id"));
    private static final RowMapper<TranCatBalance> TCATBAL = (rs, n) -> new TranCatBalance(rs.getLong("acct_id"),
            rs.getString("type_cd"), rs.getInt("cat_cd"), rs.getBigDecimal("balance"));
    private static final RowMapper<TranType> TRAN_TYPE = (rs, n) -> new TranType(rs.getString("type_cd"),
            rs.getString("description"));
    private static final RowMapper<TranCategory> TRAN_CATEGORY = (rs, n) -> new TranCategory(
            rs.getString("type_cd"), rs.getInt("cat_cd"), rs.getString("description"));
    private static final RowMapper<Transaction> TRANSACTION = PostingStore::transaction;

    private final JdbcTemplate jdbc;

    public PostingStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Transaction transaction(ResultSet rs, int n) throws SQLException {
        return new Transaction(rs.getString("tran_id"), rs.getString("type_cd"), rs.getInt("cat_cd"),
                rs.getString("source"), rs.getString("description"), rs.getBigDecimal("amount"),
                rs.getString("merchant_id"), rs.getString("merchant_name"), rs.getString("merchant_city"),
                rs.getString("merchant_zip"), rs.getString("card_num"), rs.getString("orig_ts"),
                rs.getString("proc_ts"));
    }

    /** DELETE/DEFINE CLUSTER of every cluster the chain owns. */
    public void clear() {
        jdbc.execute("truncate account, card_xref, tran_cat_balance, tran_type, tran_category, transaction");
    }

    public void insert(Account a) {
        jdbc.update("insert into account values (?,?,?,?,?,?,?,?,?,?,?,?)", a.acctId(), a.activeStatus(),
                a.currBal(), a.creditLimit(), a.cashCreditLimit(), a.openDate(), a.expirationDate(),
                a.reissueDate(), a.currCycCredit(), a.currCycDebit(), a.addrZip(), a.groupId());
    }

    public void insert(CardXref x) {
        jdbc.update("insert into card_xref values (?,?,?)", x.cardNum(), x.custId(), x.acctId());
    }

    public void insert(TranCatBalance b) {
        jdbc.update("insert into tran_cat_balance values (?,?,?,?)", b.acctId(), b.typeCd(), b.catCd(),
                b.balance());
    }

    public void insert(TranType t) {
        jdbc.update("insert into tran_type values (?,?)", t.typeCd(), t.description());
    }

    public void insert(TranCategory c) {
        jdbc.update("insert into tran_category values (?,?,?)", c.typeCd(), c.catCd(), c.description());
    }

    public void insert(Transaction t) {
        jdbc.update("insert into transaction values (?,?,?,?,?,?,?,?,?,?,?,?,?)", t.tranId(), t.typeCd(),
                t.catCd(), t.source(), t.description(), t.amount(), t.merchantId(), t.merchantName(),
                t.merchantCity(), t.merchantZip(), t.cardNum(), t.origTs(), t.procTs());
    }

    public Optional<CardXref> findXref(String cardNum) {
        return jdbc.query("select * from card_xref where card_num = ?", XREF, cardNum).stream().findFirst();
    }

    public Optional<Account> findAccount(long acctId) {
        return jdbc.query("select * from account where acct_id = ?", ACCOUNT, acctId).stream().findFirst();
    }

    public Optional<TranCatBalance> findTranCatBalance(long acctId, String typeCd, int catCd) {
        return jdbc.query("select * from tran_cat_balance where acct_id = ? and type_cd = ? and cat_cd = ?",
                TCATBAL, acctId, typeCd, catCd).stream().findFirst();
    }

    public Optional<TranType> findTranType(String typeCd) {
        return jdbc.query("select * from tran_type where type_cd = ?", TRAN_TYPE, typeCd).stream().findFirst();
    }

    public Optional<TranCategory> findTranCategory(String typeCd, int catCd) {
        return jdbc.query("select * from tran_category where type_cd = ? and cat_cd = ?", TRAN_CATEGORY,
                typeCd, catCd).stream().findFirst();
    }

    public void updateBalance(TranCatBalance b) {
        jdbc.update("update tran_cat_balance set balance = ? where acct_id = ? and type_cd = ? and cat_cd = ?",
                b.balance(), b.acctId(), b.typeCd(), b.catCd());
    }

    public void updateCurrentBalances(Account a) {
        jdbc.update("update account set curr_bal = ?, curr_cyc_credit = ?, curr_cyc_debit = ? where acct_id = ?",
                a.currBal(), a.currCycCredit(), a.currCycDebit(), a.acctId());
    }

    public List<Account> accounts() {
        return jdbc.query("select * from account order by acct_id", ACCOUNT);
    }

    public List<TranCatBalance> tranCatBalances() {
        return jdbc.query("select * from tran_cat_balance order by acct_id, type_cd, cat_cd", TCATBAL);
    }

    public List<Transaction> transactions() {
        return jdbc.query("select * from transaction order by tran_id", TRANSACTION);
    }
}
