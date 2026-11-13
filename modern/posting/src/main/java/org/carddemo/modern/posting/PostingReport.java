package org.carddemo.modern.posting;

import static org.carddemo.modern.posting.PostingRun.add;
import static org.carddemo.modern.posting.PostingRun.number;
import static org.carddemo.modern.posting.PostingRun.same;
import static org.carddemo.modern.posting.PostingRun.text;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.carddemo.modern.codec.Layouts;
import org.springframework.jdbc.core.JdbcTemplate;

final class PostingReport {
    private static final String RULE = "-".repeat(133);
    private final JdbcTemplate jdbc;
    private final List<String> report = new ArrayList<>();
    private final List<String> log = new ArrayList<>();
    private int lineCount;
    private BigDecimal page = BigDecimal.ZERO;
    private BigDecimal account = BigDecimal.ZERO;
    private BigDecimal grand = BigDecimal.ZERO;

    PostingReport(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    int run(Path input, Path output, List<Map<String, Object>> transactions) throws IOException {
        log.add("START OF EXECUTION OF PROGRAM CBTRN03C");
        var dates = PostingRun.lines(input, "DATEPARM");
        int rc = 0;
        if (!dates.isEmpty()) {
            var date = pad(dates.getFirst(), 21);
            var start = date.substring(0, 10);
            var end = date.substring(11, 21);
            log.add("Reporting from " + start + " to " + end);
            boolean first = true;
            String currentCard = " ".repeat(16);
            Map<String, Object> xref = null;
            for (var row : transactions) {
                if (!inRange(row, start, end)) continue;
                log.add(Layouts.TRAN.encode(row));
                var card = text(row, "TRAN-CARD-NUM");
                if (!currentCard.equals(card)) {
                    if (!first) {
                        total("Account Total", 13, account);
                        account = BigDecimal.ZERO;
                        line(RULE);
                    }
                    currentCard = card;
                    xref = PostingTables.XREF.read(jdbc).stream().filter(r -> same(r.get("XREF-CARD-NUM"), card)).findFirst().orElse(null);
                }
                var type = PostingTables.TYPE.read(jdbc).stream().filter(r -> same(r.get("TRAN-TYPE"), row.get("TRAN-TYPE-CD"))).findFirst().orElse(null);
                var category = PostingTables.CATEGORY.read(jdbc).stream().filter(r -> same(r.get("TRAN-TYPE-CD"), row.get("TRAN-TYPE-CD"))
                        && same(r.get("TRAN-CAT-CD"), row.get("TRAN-CAT-CD"))).findFirst().orElse(null);
                if (xref == null || type == null || category == null) {
                    if (xref == null) log.add("INVALID CARD NUMBER : " + card);
                    else if (type == null) log.add("INVALID TRANSACTION TYPE : " + text(row, "TRAN-TYPE-CD"));
                    else log.add("INVALID TRAN CATG KEY : " + text(row, "TRAN-TYPE-CD") + String.format("%04d", number(row, "TRAN-CAT-CD").intValue()));
                    log.addAll(List.of("FILE STATUS IS: NNNN0023", "ABENDING PROGRAM", "CEE3ABD: USER ABEND U+0000000999"));
                    rc = 12;
                    break;
                }
                if (first) {
                    first = false;
                    headers(start, end);
                }
                if (lineCount % 20 == 0) {
                    pageTotal();
                    headers(start, end);
                }
                page = add(page, number(row, "TRAN-AMT"), 9);
                account = add(account, number(row, "TRAN-AMT"), 9);
                line(pad(text(row, "TRAN-ID"), 16) + " " + String.format("%011d", number(xref, "XREF-ACCT-ID").longValue())
                        + " " + text(row, "TRAN-TYPE-CD") + "-" + pad(text(type, "TRAN-TYPE-DESC"), 15)
                        + " " + String.format("%04d", number(row, "TRAN-CAT-CD").intValue()) + "-"
                        + pad(text(category, "TRAN-CAT-TYPE-DESC"), 29) + " " + pad(text(row, "TRAN-SOURCE"), 10)
                        + "    " + edited(number(row, "TRAN-AMT"), false));
            }
            if (rc == 0 && !transactions.isEmpty() && inRange(transactions.getLast(), start, end)) {
                var lastAmount = number(transactions.getLast(), "TRAN-AMT");
                log.add("TRAN-AMT " + display(lastAmount));
                log.add("WS-PAGE-TOTAL" + display(page));
                page = add(page, lastAmount, 9);
                account = add(account, lastAmount, 9);
                pageTotal();
                total("Grand Total", 11, grand);
            }
        }
        if (rc == 0) log.add("END OF EXECUTION OF PROGRAM CBTRN03C");
        PostingRun.write(output.resolve("sysout/TRANREPT.STEP10R.txt"), log);
        PostingRun.write(output.resolve("datasets/" + PostingRun.PREFIX + "TRANREPT.G0001V00.txt"), report);
        return rc;
    }

    private static boolean inRange(Map<String, Object> row, String start, String end) {
        var date = text(row, "TRAN-PROC-TS").substring(0, 10);
        return date.compareTo(start) >= 0 && date.compareTo(end) <= 0;
    }

    private void headers(String start, String end) {
        line(pad("DALYREPT", 38) + pad("Daily Transaction Report", 41) + "Date Range: " + start + " to " + end);
        line("");
        line(pad("Transaction ID", 17) + pad("Account ID", 12) + pad("Transaction Type", 19)
                + pad("Tran Category", 35) + pad("Tran Source", 14) + " " + "        Amount");
        line(RULE);
    }

    private void pageTotal() {
        total("Page Total", 11, page);
        grand = add(grand, page, 9);
        page = BigDecimal.ZERO;
        line(RULE);
    }

    private void total(String label, int width, BigDecimal amount) {
        line(pad(label, width) + ".".repeat(97 - width) + edited(amount, true));
    }

    private void line(String value) {
        report.add(pad(value, 133).stripTrailing());
        lineCount++;
    }

    static String edited(BigDecimal amount, boolean plus) {
        String digits = amount.signum() == 0 ? "" : String.format(Locale.ROOT, "%,.2f", amount.abs());
        if (amount.abs().compareTo(BigDecimal.ONE) < 0 && amount.signum() != 0) digits = digits.substring(1);
        return (amount.signum() < 0 ? "-" : plus ? "+" : " ") + String.format(Locale.ROOT, "%14s", digits);
    }

    private static String display(BigDecimal amount) {
        return String.format(Locale.ROOT, "%011d", amount.abs().movePointRight(2).longValue()) + (amount.signum() < 0 ? "-" : "+");
    }

    private static String pad(String value, int length) {
        return (value + " ".repeat(length)).substring(0, length);
    }
}
