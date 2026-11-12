package org.carddemo.modern.posting;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.carddemo.modern.codec.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.carddemo.modern.posting.PostingRun.*;
import static org.carddemo.modern.posting.PostingTables.*;

/** CBTRN03C and CVTRA07Y report formatting, following TRANREPT's stable card sort. */
final class PostingReport {
    private final JdbcTemplate jdbc;
    private final Map<String, Map<String, Object>> cards;
    private final List<String> report = new ArrayList<>(), sysout = new ArrayList<>();
    private BigDecimal page = BigDecimal.ZERO, account = BigDecimal.ZERO, grand = BigDecimal.ZERO;
    private int lineCount;
    PostingReport(JdbcTemplate jdbc, Map<String, Map<String, Object>> cards) { this.jdbc = jdbc; this.cards = cards; }
    static String padded(String value, int length) { return (value + " ".repeat(length)).substring(0, length); }
    static String edited(BigDecimal value, boolean plus) {
        String digits = String.format(Locale.US, "%,.2f", value.abs());
        if (value.signum() == 0) digits = ".00";
        String sign = value.signum() < 0 ? "-" : plus ? "+" : "";
        // CVTRA07Y: -ZZZ,ZZZ,ZZZ.ZZ / +ZZZ,ZZZ,ZZZ.ZZ has a fixed sign, width 15.
        return padded(sign, 1) + " ".repeat(Math.max(0, 14 - digits.length())) + digits;
    }
    private void write(String line) { report.add(PostingLayouts.REPORT.encodeLine(Map.of("FD-REPTFILE-REC", line))); lineCount++; }
    private void headers(String start, String end) {
        write(padded("DALYREPT", 38) + padded("Daily Transaction Report", 41) + "Date Range: " + start + " to " + end);
        write("");
        write(padded("Transaction ID", 17) + padded("Account ID", 12) + padded("Transaction Type", 19)
                + padded("Tran Category", 35) + padded("Tran Source", 14) + " " + "        Amount  ");
        write("-".repeat(133));
    }
    private void pageTotal() { write(padded("Page Total", 11) + ".".repeat(86) + edited(page, true)); grand = grand.add(page); page = BigDecimal.ZERO; write("-".repeat(133)); }
    private void accountTotal() { write("Account Total" + ".".repeat(84) + edited(account, true)); account = BigDecimal.ZERO; write("-".repeat(133)); }
    int run(Path input, Path output, String sortStart, String sortEnd) throws IOException {
        sysout.add("START OF EXECUTION OF PROGRAM CBTRN03C");
        var dates = lines(input.resolve(PREFIX + "DATEPARM.txt"));
        int rc = 0;
        if (!dates.isEmpty()) {
            var date = PostingLayouts.DATE.decode(dates.getFirst());
            var start = text(date, "WS-START-DATE"); var end = text(date, "WS-END-DATE");
            sysout.add("Reporting from " + start + " to " + end);
            var types = new HashMap<String, String>();
            for (var row : TYPE.read(jdbc)) types.put(text(row, "TRAN-TYPE"), text(row, "TRAN-TYPE-DESC"));
            var categories = new HashMap<String, String>();
            for (var row : CATEGORY.read(jdbc)) categories.put(text(row, "TRAN-TYPE-CD") + String.format(Locale.ROOT, "%04d", number(row, "TRAN-CAT-CD").intValueExact()), text(row, "TRAN-CAT-TYPE-DESC"));
            var rows = TRAN.read(jdbc).stream().filter(r -> text(r, "TRAN-PROC-TS").substring(0, 10).compareTo(sortStart) >= 0
                    && text(r, "TRAN-PROC-TS").substring(0, 10).compareTo(sortEnd) <= 0)
                    .sorted(Comparator.comparing(r -> text(r, "TRAN-CARD-NUM"))).toList();
            String currentCard = null;
            for (var row : rows) {
                var procDate = text(row, "TRAN-PROC-TS").substring(0, 10);
                if (procDate.compareTo(start) < 0 || procDate.compareTo(end) > 0) continue;
                sysout.add(TRAN.layout().encode(row));
                String card = text(row, "TRAN-CARD-NUM"), type = text(row, "TRAN-TYPE-CD");
                String category = type + String.format(Locale.ROOT, "%04d", number(row, "TRAN-CAT-CD").intValueExact());
                if (!card.equals(currentCard)) { if (currentCard != null) accountTotal(); currentCard = card; }
                String error = !cards.containsKey(card) ? "INVALID CARD NUMBER : " + card
                        : !types.containsKey(type) ? "INVALID TRANSACTION TYPE : " + type
                        : !categories.containsKey(category) ? "INVALID TRAN CATG KEY : " + category : null;
                if (error != null) { sysout.addAll(List.of(error, "FILE STATUS IS: NNNN0023", "ABENDING PROGRAM", "CEE3ABD: USER ABEND U+0000000999")); rc = 12; break; }
                if (lineCount == 0) headers(start, end);
                if (lineCount % 20 == 0) { pageTotal(); headers(start, end); }
                var amount = number(row, "TRAN-AMT"); page = page.add(amount); account = account.add(amount);
                var detail = new LinkedHashMap<String, Object>();
                detail.put("TRAN-REPORT-TRANS-ID", text(row, "TRAN-ID")); detail.put("TRAN-REPORT-ACCOUNT-ID", id(number(cards.get(card), "XREF-ACCT-ID")));
                detail.put("TRAN-REPORT-TYPE-CD", type); detail.put("TYPE-SEPARATOR", "-"); detail.put("TRAN-REPORT-TYPE-DESC", types.get(type));
                detail.put("TRAN-REPORT-CAT-CD", number(row, "TRAN-CAT-CD")); detail.put("CATEGORY-SEPARATOR", "-"); detail.put("TRAN-REPORT-CAT-DESC", categories.get(category));
                detail.put("TRAN-REPORT-SOURCE", text(row, "TRAN-SOURCE")); detail.put("TRAN-REPORT-AMT", edited(amount, false));
                write(PostingLayouts.DETAIL.encode(detail));
            }
            if (rc == 0 && lineCount != 0) { pageTotal(); write(padded("Grand Total", 11) + ".".repeat(86) + edited(grand, true)); }
        }
        if (rc == 0) sysout.add("END OF EXECUTION OF PROGRAM CBTRN03C");
        Files.write(output.resolve("datasets/" + PREFIX + "TRANREPT.G0001V00.txt"), report, StandardCharsets.ISO_8859_1);
        Files.write(output.resolve("sysout/TRANREPT.STEP10R.txt"), sysout, StandardCharsets.ISO_8859_1);
        return rc;
    }
}
