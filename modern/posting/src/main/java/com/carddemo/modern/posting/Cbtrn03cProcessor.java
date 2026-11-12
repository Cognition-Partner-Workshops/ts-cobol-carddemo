package com.carddemo.modern.posting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.carddemo.modern.codec.Layouts;
import org.springframework.batch.item.ItemProcessor;

/** CBTRN03C 1000-TRANFILE-GET-NEXT and 1100-WRITE-TRANSACTION-REPORT, including working storage. */
public class Cbtrn03cProcessor implements ItemProcessor<String, List<String>> {
    private static final String RULE = "-".repeat(133);
    private final PostingRepository repo;
    private final String start;
    private final String end;
    private final List<String> lines = new ArrayList<>();
    private final List<String> sysout = new ArrayList<>();
    private String card = "";
    private boolean first = true;
    private boolean lastEligible;
    private boolean abended;
    private long accountId;
    private int counter;
    private BigDecimal page = new BigDecimal("0.00");
    private BigDecimal account = new BigDecimal("0.00");
    private BigDecimal grand = new BigDecimal("0.00");
    private BigDecimal lastAmount = new BigDecimal("0.00");

    public Cbtrn03cProcessor(PostingRepository repo, String start, String end) {
        this.repo = repo; this.start = start; this.end = end;
        sysout.add("START OF EXECUTION OF PROGRAM CBTRN03C");
    }

    @Override
    public List<String> process(String raw) {
        if (start == null || abended) return List.of();
        var r = Layouts.TRAN.decode(raw);
        String date = r.get("TRAN-PROC-TS").toString().substring(0,10);
        lastEligible = date.compareTo(start) >= 0 && date.compareTo(end) <= 0;
        lastAmount = PostingRepository.number(r,"TRAN-AMT");
        if (!lastEligible) return List.of();
        sysout.add(pad(raw,350));
        String nextCard = r.get("TRAN-CARD-NUM").toString();
        if (!card.equals(nextCard)) {
            if (!first) { total("Account Total",account); line(RULE); account = new BigDecimal("0.00"); }
            card = nextCard;
            Long id = repo.accountForCard(card);
            if (id == null) return abend("INVALID CARD NUMBER : " + card);
            accountId = id;
        }
        String type = r.get("TRAN-TYPE-CD").toString();
        int category = PostingRepository.number(r,"TRAN-CAT-CD").intValueExact();
        String typeDesc;
        String catDesc;
        try { typeDesc = repo.typeDescription(type); }
        catch (org.springframework.dao.EmptyResultDataAccessException e) { return abend("INVALID TRANSACTION TYPE : " + type); }
        try { catDesc = repo.categoryDescription(type,category); }
        catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return abend("INVALID TRAN CATG KEY : " + type + String.format(Locale.ROOT,"%04d",category));
        }
        if (first) { first = false; headers(); }
        // 1100 checks the total line count, not a separate detail count or >= page size.
        if (counter % 20 == 0) { pageTotal(); headers(); }
        page = sum(page,lastAmount); account = sum(account,lastAmount);
        line(pad(r.get("TRAN-ID").toString(),16) + " " + String.format(Locale.ROOT,"%011d",accountId) + " "
                + type + "-" + pad(typeDesc,15) + " " + String.format(Locale.ROOT,"%04d",category) + "-"
                + pad(catDesc,29) + " " + pad(r.get("TRAN-SOURCE").toString(),10) + "    " + edited(lastAmount,false));
        return List.of();
    }

    /** EOF branch adds the retained last transaction again; there is no final account-total flush. */
    void finish() {
        if (abended) return;
        if (lastEligible) {
            sysout.add("TRAN-AMT " + display(lastAmount));
            sysout.add("WS-PAGE-TOTAL" + display(page));
            page = eofPageTotal(page,lastAmount); account = sum(account,lastAmount);
            pageTotal(); total("Grand Total",grand);
        }
        sysout.add("END OF EXECUTION OF PROGRAM CBTRN03C");
    }

    static BigDecimal eofPageTotal(BigDecimal page, BigDecimal retainedAmount) { return sum(page,retainedAmount); }

    private static BigDecimal sum(BigDecimal a, BigDecimal b) { return CobolNumeric.store(a.add(b),9,2); }
    private void headers() {
        line(pad("DALYREPT",38) + pad("Daily Transaction Report",41) + "Date Range: " + start + " to " + end);
        line("");
        line("Transaction ID   Account ID  Transaction Type   Tran Category                      Tran Source            Amount");
        line(RULE);
    }
    private void pageTotal() {
        total("Page Total ",page); grand = sum(grand,page); page = new BigDecimal("0.00"); line(RULE);
    }
    private void total(String label, BigDecimal value) { line(label + ".".repeat(97-label.length()) + edited(value,true)); }
    private void line(String value) { lines.add(pad(value,133).stripTrailing()); counter++; }
    static String edited(BigDecimal amount, boolean plus) {
        String digits = String.format(Locale.ROOT,"%,.2f",amount.abs());
        return (amount.signum() < 0 ? "-" : plus ? "+" : " ") + " ".repeat(14-digits.length()) + digits;
    }
    private static String display(BigDecimal amount) {
        return String.format(Locale.ROOT,"%011d",amount.abs().movePointRight(2).longValueExact()) + (amount.signum() < 0 ? "-" : "+");
    }
    static String pad(String value, int length) { return value.length() >= length ? value.substring(0,length) : value + " ".repeat(length-value.length()); }
    List<String> lines() { return List.copyOf(lines); }
    List<String> sysout() { return List.copyOf(sysout); }
    int returnCode() { return abended ? 12 : 0; }
    private List<String> abend(String message) {
        abended = true;
        Abend.append(sysout,message,23,Abend.REPORT_FILES);
        Abend.stopStep();
        return List.of();
    }

    /** 0000-0500 OPENs: only files opened successfully produce implicit CLOSE warnings on an abend. */
    void open() {
        var opened = new ArrayList<String>();
        String message = "ERROR OPENING TRANFILE";
        try {
            repo.jdbc().execute("SELECT 1 FROM posting.report_input LIMIT 0");
            opened.add("TRANSACT-FILE ('TRANFILE')");
            opened.add("REPORT-FILE ('TRANREPT')");
            message = "ERROR OPENING CROSS REF FILE";
            repo.jdbc().execute("SELECT 1 FROM posting.card_xref LIMIT 0");
            opened.add("XREF-FILE ('CARDXREF')");
            message = "ERROR OPENING TRANSACTION TYPE FILE";
            repo.jdbc().execute("SELECT 1 FROM posting.transaction_type LIMIT 0");
            opened.add("TRANTYPE-FILE ('TRANTYPE')");
            message = "ERROR OPENING TRANSACTION CATG FILE";
            repo.jdbc().execute("SELECT 1 FROM posting.transaction_category LIMIT 0");
        } catch (org.springframework.dao.DataAccessException e) {
            abended = true;
            Abend.append(sysout,message,30,Abend.reverse(opened));
            Abend.stopStep();
            return;
        }
        if (start != null) sysout.add("Reporting from " + start + " to " + end);
    }
}
