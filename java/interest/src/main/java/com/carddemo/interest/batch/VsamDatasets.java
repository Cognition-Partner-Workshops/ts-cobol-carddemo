package com.carddemo.interest.batch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.carddemo.interest.record.AccountRecord;
import com.carddemo.interest.record.CardXrefRecord;
import com.carddemo.interest.record.DisclosureGroupRecord;
import com.carddemo.interest.record.TranCatBalRecord;
import com.carddemo.interest.record.TransactionRecord;

/**
 * Coexistence adapter between the step's DD files and the interest schema. The JCL runner hands
 * the step flat (fixed-length, unblocked) images of the KSDS clusters; they are loaded before the
 * job and the files the step writes (ACCTFILE, TRANSACT) are produced from the tables after it.
 * Once the chain's neighbours read PostgreSQL directly this adapter is the part that goes away.
 */
@Component
public class VsamDatasets {

    private final JdbcTemplate jdbc;

    public VsamDatasets(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void load(Path acctFile, Path xrefFile, Path discGrp, Path tcatBalF) throws IOException {
        List<Object[]> rows = new ArrayList<>();
        for (AccountRecord a : read(acctFile, AccountRecord.LENGTH, AccountRecord::parse)) {
            rows.add(new Object[] {a.acctId(), a.activeStatus(), a.currBal(), a.creditLimit(), a.cashCreditLimit(),
                    a.openDate(), a.expirationDate(), a.reissueDate(), a.currCycCredit(), a.currCycDebit(),
                    a.addrZip(), a.groupId(), a.filler()});
        }
        jdbc.batchUpdate("INSERT INTO account VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", rows);

        rows = new ArrayList<>();
        int seq = 0;
        for (CardXrefRecord x : read(xrefFile, CardXrefRecord.LENGTH, CardXrefRecord::parse)) {
            rows.add(new Object[] {x.cardNum(), ++seq, x.custId(), x.acctId(), x.filler()});
        }
        jdbc.batchUpdate("INSERT INTO card_xref VALUES (?, ?, ?, ?, ?)", rows);

        rows = new ArrayList<>();
        for (DisclosureGroupRecord d : read(discGrp, DisclosureGroupRecord.LENGTH, DisclosureGroupRecord::parse)) {
            rows.add(new Object[] {d.acctGroupId(), d.tranTypeCd(), d.tranCatCd(), d.intRate(), d.filler()});
        }
        jdbc.batchUpdate("INSERT INTO disclosure_group VALUES (?, ?, ?, ?, ?)", rows);

        rows = new ArrayList<>();
        for (TranCatBalRecord t : read(tcatBalF, TranCatBalRecord.LENGTH, TranCatBalRecord::parse)) {
            rows.add(new Object[] {t.acctId(), t.tranTypeCd(), t.tranCatCd(), t.tranCatBal(), t.filler()});
        }
        jdbc.batchUpdate("INSERT INTO tran_cat_balance VALUES (?, ?, ?, ?, ?)", rows);
    }

    /** ACCTFILE in key order, rewritten balances included. */
    public void unloadAccounts(Path acctFile) throws IOException {
        List<String> records = jdbc.query("SELECT * FROM account ORDER BY acct_id",
                (rs, n) -> new AccountRecord(rs.getLong("acct_id"), rs.getString("active_status"),
                        rs.getBigDecimal("curr_bal"), rs.getBigDecimal("credit_limit"),
                        rs.getBigDecimal("cash_credit_limit"), rs.getString("open_date"),
                        rs.getString("expiration_date"), rs.getString("reissue_date"),
                        rs.getBigDecimal("curr_cyc_credit"), rs.getBigDecimal("curr_cyc_debit"),
                        rs.getString("addr_zip"), rs.getString("group_id"), rs.getString("filler")).format());
        write(acctFile, records);
    }

    /** SYSTRAN(+1) in write order. */
    public void unloadTransactions(Path tranFile) throws IOException {
        List<String> records = jdbc.query("SELECT * FROM systran ORDER BY write_seq",
                (rs, n) -> new TransactionRecord(rs.getString("tran_id"), rs.getString("tran_type_cd"),
                        rs.getInt("tran_cat_cd"), rs.getString("tran_source"), rs.getString("tran_desc"),
                        rs.getBigDecimal("tran_amt"), rs.getLong("merchant_id"), rs.getString("merchant_name"),
                        rs.getString("merchant_city"), rs.getString("merchant_zip"), rs.getString("card_num"),
                        rs.getString("orig_ts"), rs.getString("proc_ts")).format());
        write(tranFile, records);
    }

    static <T> List<T> read(Path file, int lrecl, Function<String, T> parser) throws IOException {
        String data = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
        List<T> records = new ArrayList<>();
        for (int i = 0; i + lrecl <= data.length(); i += lrecl) {
            records.add(parser.apply(data.substring(i, i + lrecl)));
        }
        return records;
    }

    static void write(Path file, List<String> records) throws IOException {
        Files.write(file, String.join("", records).getBytes(StandardCharsets.ISO_8859_1));
    }
}
