package com.carddemo.modern.interest;

import com.carddemo.modern.interest.Records.Account;
import com.carddemo.modern.interest.Records.CardXref;
import com.carddemo.modern.interest.Records.DisclosureGroup;
import com.carddemo.modern.interest.Records.TranCatBal;
import com.carddemo.modern.interest.Records.Transaction;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.carddemo.modern.codec.Layouts;
import org.springframework.batch.item.ItemProcessor;

/**
 * The CBACT04C main loop, one TCATBALF record at a time, with the program's control break on
 * TRANCAT-ACCT-ID.
 *
 * <p>Legacy quirks reproduced on purpose (COBOL batch parity standard):
 * <ul>
 *   <li>Interest is truncated, not rounded: {@code COMPUTE WS-MONTHLY-INT} has no ROUNDED phrase.</li>
 *   <li>C2C-06: the last account on TCATBALF is never rewritten. 1050-UPDATE-ACCOUNT runs only when the
 *       next account starts; the EOF branch that would update the last one is unreachable because the loop
 *       re-tests END-OF-FILE before it. So nothing is flushed at end of input here either.</li>
 * </ul>
 */
public class Cbact04cProcessor implements ItemProcessor<TranCatBal, Step15Output> {

    private static final DateTimeFormatter DB2_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SS");

    private final InterestRepository repository;
    private final Sysout sysout;
    private final String parmDate;
    private final String db2FormatTs;

    private Long lastAcctNum;
    private boolean firstTime = true;
    private BigDecimal totalInt = BigDecimal.ZERO.setScale(2);
    private Account account;
    private CardXref xref;
    private int tranIdSuffix;
    private int written;
    private boolean abended;

    public Cbact04cProcessor(InterestRepository repository, Sysout sysout, String parmDate,
                             Clock clock) {
        this.repository = repository;
        this.sysout = sysout;
        this.parmDate = String.format("%-10.10s", parmDate);
        this.db2FormatTs = LocalDateTime.now(clock).format(DB2_TS) + "0000";
    }

    @Override
    public Step15Output process(TranCatBal tcat) {
        if (abended) {
            return null;
        }
        sysout.display(Layouts.TRAN_CAT_BAL.encode(tcat.fields()));
        Account rewrite = null;
        try {
            if (lastAcctNum == null || tcat.acctId() != lastAcctNum) {
                if (!firstTime) {
                    rewrite = updateAccount();
                } else {
                    firstTime = false;
                }
                totalInt = BigDecimal.ZERO.setScale(2);
                lastAcctNum = tcat.acctId();
                account = readAccount(tcat.acctId());
                xref = readXref(tcat.acctId());
            }
            DisclosureGroup group = interestRate(account.groupId(), tcat.typeCd(), tcat.catCd());
            Transaction transaction = null;
            int seq = 0;
            if (group.intRate().signum() != 0) {
                BigDecimal monthlyInt = CobolNumeric.store(
                        InterestCalculator.monthlyInterest(tcat.balance(), group.intRate()), 9, 2);
                totalInt = CobolNumeric.store(totalInt.add(monthlyInt), 9, 2);
                transaction = interestTransaction(monthlyInt);
                seq = ++written;
            }
            return new Step15Output(rewrite, transaction, seq, false);
        } catch (CobolAbend e) {
            // the REWRITE of the previous account already happened when CBACT04C abends on this record
            abended = true;
            return new Step15Output(rewrite, null, 0, true);
        }
    }

    /** 1050-UPDATE-ACCOUNT. */
    private Account updateAccount() {
        Account a = account;
        return new Account(a.acctId(), a.activeStatus(), CobolNumeric.store(a.currBal().add(totalInt), 10, 2),
                a.creditLimit(), a.cashCreditLimit(), a.openDate(), a.expirationDate(), a.reissueDate(),
                BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), a.addrZip(), a.groupId(), a.filler());
    }

    /** 1100-GET-ACCT-DATA. */
    private Account readAccount(long acctId) {
        Optional<Account> found = repository.findAccount(acctId);
        if (found.isEmpty()) {
            sysout.display("ACCOUNT NOT FOUND: " + acctNum(acctId));
            throw abend("ERROR READING ACCOUNT FILE");
        }
        return found.get();
    }

    /** 1110-GET-XREF-DATA (alternate index on XREF-ACCT-ID). */
    private CardXref readXref(long acctId) {
        Optional<CardXref> found = repository.findXrefByAccount(acctId);
        if (found.isEmpty()) {
            sysout.display("ACCOUNT NOT FOUND: " + acctNum(acctId));
            throw abend("ERROR READING XREF FILE");
        }
        return found.get();
    }

    /** 1200-GET-INTEREST-RATE with the 1200-A-GET-DEFAULT-INT-RATE fallback. */
    private DisclosureGroup interestRate(String groupId, String typeCd, int catCd) {
        Optional<DisclosureGroup> found = repository.findDisclosureGroup(groupId, typeCd, catCd);
        if (found.isPresent()) {
            return found.get();
        }
        sysout.display("DISCLOSURE GROUP RECORD MISSING");
        sysout.display("TRY WITH DEFAULT GROUP CODE");
        // MOVE 'DEFAULT' TO FD-DIS-ACCT-GROUP-ID: PIC X(10), space-padded like every key read from the file
        return repository.findDisclosureGroup(String.format("%-10s", "DEFAULT"), typeCd, catCd)
                .orElseThrow(() -> abend("ERROR READING DEFAULT DISCLOSURE GROUP"));
    }

    /** 1300-B-WRITE-TX. */
    private Transaction interestTransaction(BigDecimal monthlyInt) {
        tranIdSuffix = (tranIdSuffix + 1) % 1_000_000;
        return new Transaction(parmDate + String.format("%06d", tranIdSuffix), "01", 5, "System",
                "Int. for a/c " + acctNum(account.acctId()), monthlyInt, 0, "", "", "", xref.cardNum(),
                db2FormatTs, db2FormatTs);
    }

    /** 9910-DISPLAY-IO-STATUS for status 23 (record not found), then 9999-ABEND-PROGRAM. */
    private CobolAbend abend(String message) {
        sysout.display(message);
        sysout.display("FILE STATUS IS: NNNN0023");
        sysout.display("ABENDING PROGRAM");
        sysout.display("CEE3ABD: USER ABEND U+0000000999");
        // STOP RUN in the CEE3ABD stand-in: libcob closes the open files and warns on stderr, which the
        // JCL runner appends to SYSOUT after a blank line (reverse order of the OPENs in 0000-0400)
        sysout.display("");
        for (String fd : new String[] {"TRANSACT-FILE ('TRANSACT')", "ACCOUNT-FILE ('ACCTFILE')",
                "DISCGRP-FILE ('DISCGRP')", "XREF-FILE ('XREFFILE')", "TCATBAL-FILE ('TCATBALF')"}) {
            sysout.display("libcob: warning: implicit CLOSE of " + fd);
        }
        return new CobolAbend(message);
    }

    private static String acctNum(long acctId) {
        return String.format("%011d", acctId);
    }
}
