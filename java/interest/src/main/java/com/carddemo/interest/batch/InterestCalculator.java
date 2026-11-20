package com.carddemo.interest.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.record.AccountRecord;
import com.carddemo.interest.record.Fixed;
import com.carddemo.interest.record.TranCatBalRecord;
import com.carddemo.interest.record.TransactionRecord;

/**
 * The body of CBACT04C's main loop for one TCATBALF record: account break (1050 rewrite of the
 * previous account, 1100/1110 lookups), 1200 rate lookup with the DEFAULT fallback, 1300 interest
 * and 1300-B transaction. It carries the program's working storage between records, so it is step
 * scoped and the step is not restartable.
 */
public class InterestCalculator implements ItemProcessor<TranCatBalRecord, StepOutput> {

    private static final BigDecimal MONTHS_X_100 = BigDecimal.valueOf(1200);
    private static final String DEFAULT_GROUP = Fixed.pad("DEFAULT", 10);
    private static final DateTimeFormatter DB2_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final JdbcTemplate jdbc;
    private final Sysout sysout;
    private final String parmDate;
    private final Supplier<LocalDateTime> clock;

    private Long lastAcctNum;
    private AccountRecord account;
    private String cardNum;
    private BigDecimal totalInt = ZERO;
    private long tranIdSuffix;
    private boolean abended;

    public InterestCalculator(JdbcTemplate jdbc, Sysout sysout, String parmDate, Supplier<LocalDateTime> clock) {
        this.jdbc = jdbc;
        this.sysout = sysout;
        this.parmDate = Fixed.pad(parmDate, 10);
        this.clock = clock;
    }

    @Override
    public StepOutput process(TranCatBalRecord balance) {
        if (abended) {
            return null;
        }
        sysout.display(balance.format());
        AccountRecord rewritten = null;
        try {
            if (lastAcctNum == null || balance.acctId() != lastAcctNum) {
                if (lastAcctNum != null) {
                    // Legacy quirk: the rewrite happens only here, on the next account break, and
                    // the end-of-file branch that would rewrite the last account is unreachable.
                    rewritten = withInterest(account, totalInt);
                    account = rewritten;
                }
                totalInt = ZERO;
                lastAcctNum = balance.acctId();
                account = readAccount(balance.acctId());
                cardNum = readCardNum(balance.acctId());
            }
            BigDecimal rate = interestRate(account.groupId(), balance);
            if (rate.signum() == 0) {
                return rewritten == null ? null : new StepOutput(rewritten, 0, null, false);
            }
            BigDecimal monthlyInt = monthlyInterest(balance.tranCatBal(), rate);
            totalInt = Fixed.fit(totalInt.add(monthlyInt), 9, 2);
            return new StepOutput(rewritten, ++tranIdSuffix, interestTransaction(monthlyInt), false);
        } catch (Abend abend) {
            abended = true;
            return new StepOutput(rewritten, 0, null, true);
        }
    }

    public boolean abended() {
        return abended;
    }

    /** COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200 with no ROUNDED: truncates. */
    public static BigDecimal monthlyInterest(BigDecimal tranCatBal, BigDecimal intRate) {
        return Fixed.fit(tranCatBal.multiply(intRate).divide(MONTHS_X_100, 2, RoundingMode.DOWN), 9, 2);
    }

    private AccountRecord readAccount(long acctId) {
        List<AccountRecord> rows = jdbc.query("SELECT * FROM account WHERE acct_id = ?",
                (rs, n) -> new AccountRecord(rs.getLong("acct_id"), rs.getString("active_status"),
                        rs.getBigDecimal("curr_bal"), rs.getBigDecimal("credit_limit"),
                        rs.getBigDecimal("cash_credit_limit"), rs.getString("open_date"),
                        rs.getString("expiration_date"), rs.getString("reissue_date"),
                        rs.getBigDecimal("curr_cyc_credit"), rs.getBigDecimal("curr_cyc_debit"),
                        rs.getString("addr_zip"), rs.getString("group_id"), rs.getString("filler")),
                acctId);
        if (rows.isEmpty()) {
            sysout.display("ACCOUNT NOT FOUND: " + Fixed.unsigned(acctId, 11));
            throw Abend.afterIoError(sysout, "ERROR READING ACCOUNT FILE", "23");
        }
        return rows.get(0);
    }

    /** READ XREF-FILE KEY IS FD-XREF-ACCT-ID: the first card on the account alternate index. */
    private String readCardNum(long acctId) {
        List<String> rows = jdbc.queryForList(
                "SELECT card_num FROM card_xref WHERE acct_id = ? ORDER BY load_seq LIMIT 1", String.class, acctId);
        if (rows.isEmpty()) {
            sysout.display("ACCOUNT NOT FOUND: " + Fixed.unsigned(acctId, 11));
            throw Abend.afterIoError(sysout, "ERROR READING XREF FILE", "23");
        }
        return rows.get(0);
    }

    private BigDecimal interestRate(String groupId, TranCatBalRecord balance) {
        BigDecimal rate = findRate(groupId, balance);
        if (rate != null) {
            return rate;
        }
        sysout.display("DISCLOSURE GROUP RECORD MISSING");
        sysout.display("TRY WITH DEFAULT GROUP CODE");
        rate = findRate(DEFAULT_GROUP, balance);
        if (rate == null) {
            throw Abend.afterIoError(sysout, "ERROR READING DEFAULT DISCLOSURE GROUP", "23");
        }
        return rate;
    }

    private BigDecimal findRate(String groupId, TranCatBalRecord balance) {
        List<BigDecimal> rows = jdbc.queryForList("SELECT int_rate FROM disclosure_group"
                        + " WHERE acct_group_id = ? AND tran_type_cd = ? AND tran_cat_cd = ?", BigDecimal.class,
                groupId, balance.tranTypeCd(), balance.tranCatCd());
        return rows.isEmpty() ? null : rows.get(0);
    }

    private TransactionRecord interestTransaction(BigDecimal monthlyInt) {
        // WS-TRANID-SUFFIX is PIC 9(06): ADD 1 wraps after 999999
        String tranId = parmDate + Fixed.unsigned(tranIdSuffix % 1_000_000, 6);
        String timestamp = db2Timestamp(clock.get());
        return new TransactionRecord(tranId, "01", 5, "System",
                "Int. for a/c " + Fixed.unsigned(account.acctId(), 11), monthlyInt, 0, "", "", "", cardNum,
                timestamp, timestamp);
    }

    /** Z-GET-DB2-FORMAT-TIMESTAMP: yyyy-mm-dd-hh.mm.ss.hh followed by '0000'. */
    static String db2Timestamp(LocalDateTime now) {
        return DB2_TS.format(now) + String.format("%02d", now.getNano() / 10_000_000) + "0000";
    }

    /** 1050-UPDATE-ACCOUNT. */
    private static AccountRecord withInterest(AccountRecord a, BigDecimal totalInt) {
        return new AccountRecord(a.acctId(), a.activeStatus(), Fixed.fit(a.currBal().add(totalInt), 10, 2),
                a.creditLimit(), a.cashCreditLimit(), a.openDate(), a.expirationDate(), a.reissueDate(), ZERO, ZERO,
                a.addrZip(), a.groupId(), a.filler());
    }
}
