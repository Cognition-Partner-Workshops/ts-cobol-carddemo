package org.carddemo.modern.posting;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.carddemo.modern.codec.Layouts;
import org.carddemo.modern.codec.RejectRecord;
import org.springframework.jdbc.core.JdbcTemplate;

final class PostingRun {
    static final String PREFIX = "AWS.M2.CARDDEMO.";
    static final String STAMP = "2022-07-05-10.00.00.000000";
    private final JdbcTemplate jdbc;

    PostingRun(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    int run(Path input, Path output) throws IOException {
        Files.createDirectories(output.resolve("datasets"));
        Files.createDirectories(output.resolve("sysout"));
        for (var table : PostingTables.ALL) jdbc.update("DELETE FROM " + table.name());
        load(input, output, "ACCTDATA.PS", "ACCTFILE", PostingTables.ACCOUNT);
        load(input, output, "CARDXREF.PS", "XREFFILE", PostingTables.XREF);
        load(input, output, "TCATBALF.PS", "TCATBALF", PostingTables.BALANCE);
        load(input, output, "TRANTYPE.PS", "TRANTYPE", PostingTables.TYPE);
        load(input, output, "TRANCATG.PS", "TRANCATG", PostingTables.CATEGORY);
        load(input, output, "DALYTRAN.PS.INIT", "TRANFILE", PostingTables.TRANSACTION);
        var log = new ArrayList<String>();
        log.add("START OF EXECUTION OF PROGRAM CBTRN02C");
        var rejects = new ArrayList<String>();
        int count = 0;
        int postingRc = 0;
        for (var raw : lines(input, "DALYTRAN.PS")) {
            count++;
            var daily = Layouts.DALYTRAN.decode(raw);
            var xref = find(PostingTables.XREF, "XREF-CARD-NUM", daily.get("DALYTRAN-CARD-NUM"));
            var account = xref == null ? null : find(PostingTables.ACCOUNT, "ACCT-ID", xref.get("XREF-ACCT-ID"));
            int reason = validation(daily, xref, account);
            if (reason != 0) {
                rejects.add(new RejectRecord(raw, reason, switch (reason) {
                    case 100 -> "INVALID CARD NUMBER FOUND";
                    case 101 -> "ACCOUNT RECORD NOT FOUND";
                    case 102 -> "OVERLIMIT TRANSACTION";
                    case 103 -> "TRANSACTION RECEIVED AFTER ACCT EXPIRATION";
                    default -> throw new IllegalStateException("unknown validation reason " + reason);
                }).toLine());
                continue;
            }
            var amount = number(daily, "DALYTRAN-AMT");
            var balance = PostingTables.BALANCE.read(jdbc).stream().filter(row ->
                    same(row.get("TRANCAT-ACCT-ID"), xref.get("XREF-ACCT-ID"))
                    && same(row.get("TRANCAT-TYPE-CD"), daily.get("DALYTRAN-TYPE-CD"))
                    && same(row.get("TRANCAT-CD"), daily.get("DALYTRAN-CAT-CD"))).findFirst().orElse(null);
            if (balance == null) {
                balance = new LinkedHashMap<>();
                balance.put("TRANCAT-ACCT-ID", xref.get("XREF-ACCT-ID"));
                balance.put("TRANCAT-TYPE-CD", daily.get("DALYTRAN-TYPE-CD"));
                balance.put("TRANCAT-CD", daily.get("DALYTRAN-CAT-CD"));
                balance.put("TRAN-CAT-BAL", BigDecimal.ZERO);
                log.add("TCATBAL record not found for key : " + String.format("%011d%s%04d",
                        number(xref, "XREF-ACCT-ID").longValue(), daily.get("DALYTRAN-TYPE-CD"),
                        number(daily, "DALYTRAN-CAT-CD").intValue()) + ".. Creating.");
            }
            balance.put("TRAN-CAT-BAL", add(number(balance, "TRAN-CAT-BAL"), amount, 9));
            PostingTables.BALANCE.store(jdbc, balance);
            account.put("ACCT-CURR-BAL", add(number(account, "ACCT-CURR-BAL"), amount, 10));
            var cycle = amount.signum() >= 0 ? "ACCT-CURR-CYC-CREDIT" : "ACCT-CURR-CYC-DEBIT";
            account.put(cycle, add(number(account, cycle), amount, 10));
            PostingTables.ACCOUNT.store(jdbc, account);
            var transaction = new LinkedHashMap<String, Object>();
            daily.forEach((key, value) -> transaction.put(key.replace("DALYTRAN-", "TRAN-"), value));
            transaction.put("TRAN-PROC-TS", STAMP);
            if (!PostingTables.TRANSACTION.insert(jdbc, transaction)) {
                log.addAll(List.of("ERROR WRITING TO TRANSACTION FILE", "FILE STATUS IS: NNNN0022",
                        "ABENDING PROGRAM", "CEE3ABD: USER ABEND U+0000000999"));
                postingRc = 12;
                break;
            }
        }
        if (postingRc == 0) {
            log.add(String.format("TRANSACTIONS PROCESSED :%09d", count));
            log.add(String.format("TRANSACTIONS REJECTED  :%09d", rejects.size()));
            log.add("END OF EXECUTION OF PROGRAM CBTRN02C");
            postingRc = rejects.isEmpty() ? 0 : 4;
        }
        write(output.resolve("sysout/POSTTRAN.STEP15.txt"), log);
        write(output.resolve("datasets/" + PREFIX + "DALYREJS.G0001V00.txt"), rejects);
        for (var entry : Map.of("ACCTDATA", PostingTables.ACCOUNT, "TCATBALF", PostingTables.BALANCE,
                "TRANSACT", PostingTables.TRANSACTION).entrySet()) {
            var table = entry.getValue();
            write(output.resolve("datasets/" + PREFIX + entry.getKey() + ".VSAM.KSDS.txt"),
                    table.read(jdbc).stream().map(table.layout()::encodeLine).toList());
        }
        var transactions = PostingTables.TRANSACTION.read(jdbc).stream().filter(row -> {
            var date = text(row, "TRAN-PROC-TS").substring(0, 10);
            return date.compareTo("2022-01-01") >= 0 && date.compareTo("2022-07-06") <= 0;
        }).sorted(Comparator.comparing(row -> new BigDecimal(text(row, "TRAN-CARD-NUM")))).toList();
        int reportRc = new PostingReport(jdbc).run(input, output, transactions);
        Files.writeString(output.resolve("rc.json"), "{\n \"POSTTRAN\": {\n  \"maxrc\": " + postingRc
                + ",\n  \"steps\": {\n   \"CLCIFIL\": 0,\n   \"OPCIFIL\": 0,\n   \"STEP05\": 0,\n   \"STEP10\": 0,\n   \"STEP15\": "
                + postingRc + ",\n   \"STEP20\": 0,\n   \"STEP25\": 0,\n   \"STEP30\": 0\n  }\n },\n \"TRANREPT\": {\n  \"maxrc\": "
                + reportRc + ",\n  \"steps\": {\n   \"PRC001\": 0,\n   \"STEP05R\": 0,\n   \"STEP10R\": " + reportRc + "\n  }\n }\n}");
        return Math.max(postingRc, reportRc);
    }

    static int validation(Map<String, Object> daily, Map<String, Object> xref, Map<String, Object> account) {
        if (xref == null) return 100;
        if (account == null) return 101;
        int reason = number(account, "ACCT-CREDIT-LIMIT").compareTo(add(
                number(account, "ACCT-CURR-CYC-CREDIT").subtract(number(account, "ACCT-CURR-CYC-DEBIT")),
                number(daily, "DALYTRAN-AMT"), 9)) < 0 ? 102 : 0;
        if (text(account, "ACCT-EXPIRAION-DATE").compareTo(text(daily, "DALYTRAN-ORIG-TS").substring(0, 10)) < 0) return 103;
        return reason;
    }

    private Map<String, Object> find(PostingTables table, String field, Object value) {
        return table.read(jdbc).stream().filter(row -> same(row.get(field), value)).findFirst().orElse(null);
    }

    private void load(Path input, Path output, String dataset, String job, PostingTables table) throws IOException {
        int processed = 0;
        int skipped = 0;
        for (var line : lines(input, dataset)) {
            if (table.insert(jdbc, table.layout().decode(line))) processed++; else skipped++;
        }
        var diagnostics = new ArrayList<String>();
        diagnostics.add(String.format("IDC0005I NUMBER OF RECORDS PROCESSED WAS %09d", processed));
        if (skipped > 0) diagnostics.add(String.format("IDC3302I DUPLICATE KEYS SKIPPED %09d", skipped));
        Files.createDirectories(output.resolve("setup"));
        write(output.resolve("setup/" + job + ".STEP15.txt"), diagnostics);
        Files.writeString(output.resolve("setup/" + job + ".rc.json"), "{\"STEP15\": " + (skipped == 0 ? 0 : 4) + "}");
    }

    static List<String> lines(Path input, String dataset) throws IOException {
        var path = input.resolve(PREFIX + dataset + ".txt");
        return Files.exists(path) ? Files.readAllLines(path, StandardCharsets.ISO_8859_1) : List.of();
    }

    static void write(Path path, List<String> lines) throws IOException {
        Files.write(path, lines, StandardCharsets.ISO_8859_1);
    }

    static boolean same(Object left, Object right) {
        if (left instanceof BigDecimal a && right instanceof BigDecimal b) return a.compareTo(b) == 0;
        return left.equals(right);
    }

    static BigDecimal add(BigDecimal left, BigDecimal right, int integerDigits) {
        return left.add(right).setScale(2, java.math.RoundingMode.DOWN).remainder(BigDecimal.TEN.pow(integerDigits));
    }

    static BigDecimal number(Map<String, Object> row, String field) { return (BigDecimal) row.get(field); }
    static String text(Map<String, Object> row, String field) { return row.get(field).toString(); }
}
