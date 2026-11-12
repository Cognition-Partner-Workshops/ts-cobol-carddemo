package com.carddemo.modern.posting;

import static com.carddemo.modern.posting.PostingRepository.number;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.carddemo.modern.codec.Layouts;
import org.carddemo.modern.codec.RejectRecord;
import org.springframework.batch.item.ItemProcessor;

/** CBTRN02C 1000-DALYTRAN-GET-NEXT, 1500-VALIDATE-TRAN and 2000-POST-TRANSACTION. */
public class Cbtrn02cProcessor implements ItemProcessor<String, Cbtrn02cProcessor.Posted> {
    record Posted(long seq, Map<String, Object> account, Map<String, Object> category,
                  boolean createCategory, Map<String, Object> transaction, String reject) { }

    private final PostingRepository repo;
    private final String timestamp;
    private final Map<Long, Map<String, Object>> accounts = new HashMap<>();
    private final Map<String, Map<String, Object>> categories = new HashMap<>();
    private final List<String> sysout = new ArrayList<>();
    private long processed;
    private long rejected;
    private boolean abended;

    public Cbtrn02cProcessor(PostingRepository repo, Estate estate) {
        this.repo = repo;
        timestamp = LocalDateTime.now(estate.clock()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SS")) + "0000";
        sysout.add("START OF EXECUTION OF PROGRAM CBTRN02C");
    }

    /** Expiration is checked last and overwrites overlimit (1500-B-LOOKUP-ACCT). */
    static int validate(Long accountId, Map<String, Object> account, Map<String, Object> daily) {
        if (accountId == null) return 100;
        if (account == null) return 101;
        int reason = 0;
        // The legacy limit is cycle-based, never ACCT-CURR-BAL, including negative cycle debit.
        BigDecimal projected = CobolNumeric.store(number(account,"ACCT-CURR-CYC-CREDIT")
                .subtract(number(account,"ACCT-CURR-CYC-DEBIT")).add(number(daily,"DALYTRAN-AMT")), 9, 2);
        if (number(account,"ACCT-CREDIT-LIMIT").compareTo(projected) < 0) reason = 102;
        if (account.get("ACCT-EXPIRAION-DATE").toString().compareTo(daily.get("DALYTRAN-ORIG-TS").toString().substring(0,10)) < 0)
            reason = 103;
        return reason;
    }

    static String reasonDescription(int reason) {
        return switch (reason) {
            case 100 -> "INVALID CARD NUMBER FOUND";
            case 101, 109 -> "ACCOUNT RECORD NOT FOUND";
            case 102 -> "OVERLIMIT TRANSACTION";
            case 103 -> "TRANSACTION RECEIVED AFTER ACCT EXPIRATION";
            default -> throw new IllegalArgumentException("Unknown reason " + reason);
        };
    }

    @Override
    public Posted process(String line) {
        if (abended) return null;
        try {
            return post(line);
        } catch (Abend.Failure failure) {
            abend(failure.getMessage(),failure.status);
            Abend.stopStep();
            return null;
        }
    }

    private Posted post(String line) {
        long seq = ++processed;
        var daily = Layouts.DALYTRAN.decode(line);
        Long id = read("ERROR READING CROSS REF FILE", () -> repo.accountForCard(daily.get("DALYTRAN-CARD-NUM").toString()));
        Map<String, Object> account = id == null ? null : accounts.computeIfAbsent(id,
                key -> read("ERROR READING ACCOUNT MASTER FILE",() -> repo.account(key)));
        int reason = validate(id, account, daily);
        if (reason != 0) {
            rejected++;
            return new Posted(seq, null, null, false, null, new RejectRecord(line, reason, reasonDescription(reason)).toLine());
        }
        String type = daily.get("DALYTRAN-TYPE-CD").toString();
        int cat = number(daily,"DALYTRAN-CAT-CD").intValueExact();
        String key = String.format(java.util.Locale.ROOT,"%011d%s%04d",id,type,cat);
        Map<String, Object> category = categories.get(key);
        boolean createCategory = false;
        if (category == null) {
            category = read("ERROR READING TRANSACTION BALANCE FILE",() -> repo.category(id,type,cat));
            if (category == null) {
                createCategory = true;
                sysout.add("TCATBAL record not found for key : " + key + ".. Creating.");
                category = new LinkedHashMap<>();
                category.put("TRANCAT-ACCT-ID", BigDecimal.valueOf(id));
                category.put("TRANCAT-TYPE-CD", type);
                category.put("TRANCAT-CD", BigDecimal.valueOf(cat));
                category.put("TRAN-CAT-BAL", BigDecimal.ZERO);
            }
            categories.put(key,category);
        }
        BigDecimal amount = number(daily,"DALYTRAN-AMT");
        add(category,"TRAN-CAT-BAL",amount,9);
        add(account,"ACCT-CURR-BAL",amount,10);
        // 2800-UPDATE-ACCOUNT-REC adds the negative value itself to cycle debit.
        add(account,amount.signum() >= 0 ? "ACCT-CURR-CYC-CREDIT" : "ACCT-CURR-CYC-DEBIT",amount,10);
        var transaction = new LinkedHashMap<String, Object>();
        daily.forEach((k,v) -> transaction.put(k.replace("DALYTRAN-","TRAN-"),v));
        transaction.put("TRAN-PROC-TS",timestamp);
        return new Posted(seq, new LinkedHashMap<>(account), new LinkedHashMap<>(category), createCategory, transaction, null);
    }

    private static void add(Map<String,Object> r, String field, BigDecimal amount, int digits) {
        r.put(field,CobolNumeric.store(number(r,field).add(amount),digits,2));
    }

    List<String> sysout() {
        var result = new ArrayList<>(sysout);
        if (abended) return result;
        result.add(String.format(java.util.Locale.ROOT,"TRANSACTIONS PROCESSED :%09d",processed));
        result.add(String.format(java.util.Locale.ROOT,"TRANSACTIONS REJECTED  :%09d",rejected));
        result.add("END OF EXECUTION OF PROGRAM CBTRN02C");
        return result;
    }

    void abend(String message, int status) {
        abended = true;
        Abend.append(sysout,message,status,Abend.POST_FILES);
    }

    /** 0100-TRANFILE-OPEN uses OUTPUT, discarding even records just loaded by TRANFILE REPRO. */
    void open() {
        var opened = new ArrayList<String>();
        String message = "ERROR OPENING DALYTRAN";
        try {
            repo.jdbc().execute("SELECT 1 FROM posting.daily_transaction LIMIT 0");
            opened.add("DALYTRAN-FILE ('DALYTRAN')");
            message = "ERROR OPENING TRANSACTION FILE";
            repo.jdbc().execute("TRUNCATE posting.transaction_master");
            opened.add("TRANSACT-FILE ('TRANFILE')");
            message = "ERROR OPENING CROSS REF FILE";
            repo.jdbc().execute("SELECT 1 FROM posting.card_xref LIMIT 0");
            opened.add("XREF-FILE ('XREFFILE')");
            message = "ERROR OPENING DALY REJECTS FILE";
            repo.jdbc().execute("TRUNCATE posting.reject");
            opened.add("DALYREJS-FILE ('DALYREJS')");
            message = "ERROR OPENING ACCOUNT MASTER FILE";
            repo.jdbc().execute("SELECT 1 FROM posting.account LIMIT 0");
            opened.add("ACCOUNT-FILE ('ACCTFILE')");
            message = "ERROR OPENING TRANSACTION BALANCE FILE";
            repo.jdbc().execute("SELECT 1 FROM posting.tran_cat_bal LIMIT 0");
        } catch (org.springframework.dao.DataAccessException e) {
            abended = true;
            Abend.append(sysout,message,30,Abend.reverse(opened));
            Abend.stopStep();
        }
    }

    private <T> T read(String message, java.util.function.Supplier<T> lookup) {
        try { return lookup.get(); }
        catch (org.springframework.dao.DataAccessException e) { throw new Abend.Failure(message,30); }
    }

    int returnCode() { return abended ? 12 : rejected > 0 ? 4 : 0; }
}
