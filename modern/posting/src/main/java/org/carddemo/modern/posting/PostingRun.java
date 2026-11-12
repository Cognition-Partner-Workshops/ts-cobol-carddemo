package org.carddemo.modern.posting;

import java.io.IOException;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.carddemo.modern.codec.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.carddemo.modern.posting.PostingTables.*;

/** POSTTRAN followed by TRANREPT, including its stable card sort and recorder exports. */
public final class PostingRun {
    static final String PREFIX = "AWS.M2.CARDDEMO.";
    private final JdbcTemplate jdbc;
    public PostingRun(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    static BigDecimal number(Map<String, Object> r, String f) { return (BigDecimal) r.get(f); }
    static String text(Map<String, Object> r, String f) { return (String) r.get(f); }
    static String id(BigDecimal n) { return String.format(Locale.ROOT, "%011d", n.longValueExact()); }
    static List<String> lines(Path p) throws IOException { return Files.exists(p) ? Files.readAllLines(p, StandardCharsets.ISO_8859_1) : List.of(); }
    private void load(Path input, String dsn, PostingTables table) throws IOException {
        for (var line : lines(input.resolve(PREFIX + dsn + ".txt"))) table.write(jdbc, table.layout().decode(line), false);
    }
    private void export(Path output, String dsn, PostingTables table) throws IOException {
        Files.write(output.resolve("datasets/" + PREFIX + dsn + ".txt"), table.read(jdbc).stream().map(table.layout()::encodeLine).toList(), StandardCharsets.ISO_8859_1);
    }
    public int run(Path input, Path output, String clock, String sortStart, String sortEnd) throws IOException {
        var stamp = timestamp(clock);
        for (var table : ALL) jdbc.update("DELETE FROM " + table.name());
        load(input, "ACCTDATA.PS", ACCOUNT); load(input, "CARDXREF.PS", XREF); load(input, "TCATBALF.PS", BALANCE);
        load(input, "TRANTYPE.PS", TYPE); load(input, "TRANCATG.PS", CATEGORY);
        var accounts = new TreeMap<BigDecimal, Map<String, Object>>();
        for (var row : ACCOUNT.read(jdbc)) accounts.put(number(row, "ACCT-ID"), row);
        var cards = new TreeMap<String, Map<String, Object>>();
        for (var row : XREF.read(jdbc)) cards.put(text(row, "XREF-CARD-NUM"), row);
        var balances = new TreeMap<String, Map<String, Object>>();
        for (var row : BALANCE.read(jdbc)) balances.put(balanceKey(number(row, "TRANCAT-ACCT-ID"), text(row, "TRANCAT-TYPE-CD"), number(row, "TRANCAT-CD")), row);
        var sysout = new ArrayList<String>(); sysout.add("START OF EXECUTION OF PROGRAM CBTRN02C");
        var rejects = new ArrayList<String>(); int count = 0, postRc = 0;
        for (var line : lines(input.resolve(PREFIX + "DALYTRAN.PS.txt"))) {
            count++;
            var daily = Layouts.DALYTRAN.decode(line);
            var xref = cards.get(text(daily, "DALYTRAN-CARD-NUM"));
            var account = xref == null ? null : accounts.get(number(xref, "XREF-ACCT-ID"));
            int reason = validate(daily, account, xref != null);
            if (reason != 0) { rejects.add(new RejectRecord(line, reason, reasonText(reason)).toLine()); continue; }
            var accountId = number(xref, "XREF-ACCT-ID");
            var key = balanceKey(accountId, text(daily, "DALYTRAN-TYPE-CD"), number(daily, "DALYTRAN-CAT-CD"));
            var balance = balances.get(key);
            if (balance == null) {
                sysout.add("TCATBAL record not found for key : " + key + ".. Creating.");
                balance = new LinkedHashMap<>(); balance.put("TRANCAT-ACCT-ID", accountId);
                balance.put("TRANCAT-TYPE-CD", text(daily, "DALYTRAN-TYPE-CD")); balance.put("TRANCAT-CD", number(daily, "DALYTRAN-CAT-CD"));
                balance.put("TRAN-CAT-BAL", BigDecimal.ZERO); balances.put(key, balance);
            }
            var amount = number(daily, "DALYTRAN-AMT");
            balance.put("TRAN-CAT-BAL", number(balance, "TRAN-CAT-BAL").add(amount).setScale(2, RoundingMode.HALF_EVEN));
            account.put("ACCT-CURR-BAL", number(account, "ACCT-CURR-BAL").add(amount));
            var cycle = amount.signum() >= 0 ? "ACCT-CURR-CYC-CREDIT" : "ACCT-CURR-CYC-DEBIT";
            account.put(cycle, number(account, cycle).add(amount));
            BALANCE.write(jdbc, balance, true); ACCOUNT.write(jdbc, account, true);
            var transaction = new LinkedHashMap<String, Object>();
            daily.forEach((field, value) -> transaction.put(field.replace("DALYTRAN-", "TRAN-"), value));
            transaction.put("TRAN-PROC-TS", stamp);
            if (TRAN.write(jdbc, transaction, false) == 0) {
                sysout.addAll(List.of("ERROR WRITING TO TRANSACTION FILE", "FILE STATUS IS: NNNN0022", "ABENDING PROGRAM", "CEE3ABD: USER ABEND U+0000000999")); postRc = 12; break;
            }
        }
        if (postRc == 0) {
            postRc = rejects.isEmpty() ? 0 : 4;
            sysout.add(String.format(Locale.ROOT, "TRANSACTIONS PROCESSED :%09d", count));
            sysout.add(String.format(Locale.ROOT, "TRANSACTIONS REJECTED  :%09d", rejects.size()));
            sysout.add("END OF EXECUTION OF PROGRAM CBTRN02C");
        }
        Files.createDirectories(output.resolve("datasets")); Files.createDirectories(output.resolve("sysout"));
        export(output, "TRANSACT.VSAM.KSDS", TRAN); export(output, "ACCTDATA.VSAM.KSDS", ACCOUNT); export(output, "TCATBALF.VSAM.KSDS", BALANCE);
        Files.write(output.resolve("datasets/" + PREFIX + "DALYREJS.G0001V00.txt"), rejects, StandardCharsets.ISO_8859_1);
        Files.write(output.resolve("sysout/POSTTRAN.STEP15.txt"), sysout, StandardCharsets.ISO_8859_1);
        var report = new PostingReport(jdbc, cards);
        int reportRc = report.run(input, output, sortStart, sortEnd);
        Files.writeString(output.resolve("rc.json"), "{\n \"POSTTRAN\": {\n  \"maxrc\": " + postRc
                + ",\n  \"steps\": {\n   \"CLCIFIL\": 0,\n   \"OPCIFIL\": 0,\n   \"STEP05\": 0,\n   \"STEP10\": 0,\n   \"STEP15\": " + postRc
                + ",\n   \"STEP20\": 0,\n   \"STEP25\": 0,\n   \"STEP30\": 0\n  }\n },\n \"TRANREPT\": {\n  \"maxrc\": " + reportRc
                + ",\n  \"steps\": {\n   \"PRC001\": 0,\n   \"STEP05R\": 0,\n   \"STEP10R\": " + reportRc + "\n  }\n }\n}");
        return Math.max(postRc, reportRc);
    }
    static String balanceKey(BigDecimal account, String type, BigDecimal category) { return id(account) + type + String.format(Locale.ROOT, "%04d", category.intValueExact()); }
    static String timestamp(String clock) {
        return LocalDateTime.parse(clock, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SS")) + "0000";
    }
    static int validate(Map<String, Object> daily, Map<String, Object> account, boolean hasCard) {
        if (!hasCard) return 100;
        if (account == null) return 101;
        // CBTRN02C 1500-VALIDATE-ACCOUNT uses cycle credit minus signed cycle debit,
        // rather than current balance. The golden cases do not distinguish these formulas.
        int reason = number(account, "ACCT-CURR-CYC-CREDIT").subtract(number(account, "ACCT-CURR-CYC-DEBIT"))
                .add(number(daily, "DALYTRAN-AMT")).compareTo(number(account, "ACCT-CREDIT-LIMIT")) > 0 ? 102 : 0;
        if (text(account, "ACCT-EXPIRAION-DATE").compareTo(text(daily, "DALYTRAN-ORIG-TS").substring(0, 10)) < 0) reason = 103;
        return reason;
    }
    static String reasonText(int reason) { return switch (reason) {
        case 100 -> "INVALID CARD NUMBER FOUND"; case 101 -> "ACCOUNT RECORD NOT FOUND";
        case 102 -> "OVERLIMIT TRANSACTION"; case 103 -> "TRANSACTION RECEIVED AFTER ACCT EXPIRATION";
        default -> throw new IllegalArgumentException("unknown validation reason " + reason);
    }; }
}
