package org.carddemo.modern.posting;

import static org.carddemo.modern.posting.PostingRun.number;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class TransactionReport {
    private static final String RULE = "-".repeat(133);
    private final List<String> lines = new ArrayList<>();
    private int lineCounter;
    private int details;
    private BigDecimal page = BigDecimal.ZERO;
    private BigDecimal account = BigDecimal.ZERO;
    private BigDecimal grand = BigDecimal.ZERO;

    List<String> lines() { return List.copyOf(lines); }
    BigDecimal pageTotal() { return page; }

    void headers(String start, String end) {
        add(fit("DALYREPT", 38) + fit("Daily Transaction Report", 41) + "Date Range: " + start + " to " + end);
        add("");
        add(fit("Transaction ID", 17) + fit("Account ID", 12) + fit("Transaction Type", 19)
                + fit("Tran Category", 35) + fit("Tran Source", 14) + " " + fit("        Amount", 16));
        add(RULE);
    }

    void detail(Map<String, Object> transaction, Map<String, Object> xref, Map<String, Object> type,
            Map<String, Object> category, String start, String end) {
        if (lineCounter % 20 == 0) {
            pageTotalLine();
            headers(start, end);
        }
        var amount = number(transaction, "TRAN-AMT");
        page = page.add(amount);
        account = account.add(amount);
        add(fit(transaction.get("TRAN-ID").toString(), 16) + " "
                + String.format("%011d", number(xref, "XREF-ACCT-ID").longValueExact()) + " "
                + fit(transaction.get("TRAN-TYPE-CD").toString(), 2) + "-"
                + fit(type.get("TRAN-TYPE-DESC").toString(), 15) + " "
                + String.format("%04d", number(transaction, "TRAN-CAT-CD").intValueExact()) + "-"
                + fit(category.get("TRAN-CAT-TYPE-DESC").toString(), 29) + " "
                + fit(transaction.get("TRAN-SOURCE").toString(), 10) + "    " + edited(amount, false) + "  ");
        details++;
    }

    void accountTotal() {
        add("Account Total" + ".".repeat(84) + edited(account, true));
        account = BigDecimal.ZERO;
        add(RULE);
    }

    private void pageTotalLine() {
        add(fit("Page Total", 11) + ".".repeat(86) + edited(page, true));
        grand = grand.add(page);
        page = BigDecimal.ZERO;
        add(RULE);
    }

    void finish() {
        if (details > 0) accountTotal();
        pageTotalLine();
        add(fit("Grand Total", 11) + ".".repeat(86) + edited(grand, true));
    }

    static String edited(BigDecimal value, boolean signed) {
        var format = new DecimalFormat("#,###.00", DecimalFormatSymbols.getInstance(Locale.ROOT));
        format.setRoundingMode(java.math.RoundingMode.HALF_EVEN);
        var magnitude = format.format(value.abs());
        return (value.signum() < 0 ? "-" : signed ? "+" : " ") + " ".repeat(Math.max(0, 14 - magnitude.length())) + magnitude;
    }

    static String fit(String text, int length) {
        return text.length() >= length ? text.substring(0, length) : text + " ".repeat(length - text.length());
    }

    private void add(String line) { lines.add(line); lineCounter++; }
}
