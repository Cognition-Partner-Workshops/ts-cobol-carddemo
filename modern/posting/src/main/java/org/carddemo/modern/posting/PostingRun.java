package org.carddemo.modern.posting;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.carddemo.modern.codec.Layouts;
import org.springframework.jdbc.core.JdbcTemplate;

final class PostingRun {
    static final String PREFIX = "AWS.M2.CARDDEMO.";
    private final JdbcTemplate jdbc;

    PostingRun(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    int run(Path input, Path output, String clock, String sortStart, String sortEnd) throws IOException {
        int postingRc = post(input, output, clock);
        int reportRc = report(output, sortStart, sortEnd);
        writeReturnCodes(output, postingRc, reportRc);
        return Math.max(postingRc, reportRc);
    }

    int post(Path input, Path output, String clock) throws IOException {
        var timestamp = LocalDateTime.parse(clock, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS"));
        Files.createDirectories(output.resolve("datasets"));
        Files.createDirectories(output.resolve("sysout"));
        for (var table : PostingTable.ALL) jdbc.update("DELETE FROM " + table.name());
        load(input, output, "ACCTDATA.PS", "ACCTFILE", PostingTable.ACCOUNT);
        load(input, output, "CARDXREF.PS", "XREFFILE", PostingTable.XREF);
        load(input, output, "TCATBALF.PS", "TCATBALF", PostingTable.BALANCE);
        load(input, output, "DISCGRP.PS", "DISCGRP", PostingTable.DISCLOSURE);
        load(input, output, "TRANTYPE.PS", "TRANTYPE", PostingTable.TYPE);
        load(input, output, "TRANCATG.PS", "TRANCATG", PostingTable.CATEGORY);
        load(input, output, "DALYTRAN.PS.INIT", "TRANFILE", PostingTable.INITIAL);
        load(input, output, "DALYTRAN.PS", null, PostingTable.DAILY);
        load(input, output, "DATEPARM", null, PostingTable.DATES);

        var sysout = new ArrayList<String>();
        sysout.add("START OF EXECUTION OF PROGRAM CBTRN02C");
        int processed = 0;
        int rejected = 0;
        int rc;
        try {
            for (var daily : PostingTable.DAILY.read(jdbc)) {
                processed++;
                var xref = PostingTable.XREF.find(jdbc, daily.get("DALYTRAN-CARD-NUM"));
                var account = xref == null ? null : PostingTable.ACCOUNT.find(jdbc, xref.get("XREF-ACCT-ID"));
                int reason = validationReason(daily, xref, account);
                if (reason != 0) {
                    rejected++;
                    var reject = new LinkedHashMap<>(daily);
                    reject.put("REJ-FAIL-REASON", reason);
                    reject.put("REJ-FAIL-REASON-DESC", reasonDescription(reason));
                    PostingTable.REJECT.insert(jdbc, reject);
                    continue;
                }
                var amount = number(daily, "DALYTRAN-AMT");
                var accountId = xref.get("XREF-ACCT-ID");
                var type = daily.get("DALYTRAN-TYPE-CD");
                var category = daily.get("DALYTRAN-CAT-CD");
                var balance = PostingTable.BALANCE.find(jdbc, accountId, type, category);
                if (balance == null) {
                    sysout.add("TCATBAL record not found for key : " + String.format("%011d%s%04d",
                            ((BigDecimal) accountId).longValueExact(), type, ((BigDecimal) category).intValueExact()) + ".. Creating.");
                    balance = new LinkedHashMap<>();
                    balance.put("TRANCAT-ACCT-ID", accountId);
                    balance.put("TRANCAT-TYPE-CD", type);
                    balance.put("TRANCAT-CD", category);
                    balance.put("TRAN-CAT-BAL", BigDecimal.ZERO);
                }
                balance.put("TRAN-CAT-BAL", number(balance, "TRAN-CAT-BAL").add(amount));
                PostingTable.BALANCE.store(jdbc, balance);
                account.put("ACCT-CURR-BAL", number(account, "ACCT-CURR-BAL").add(amount));
                var cycleField = amount.signum() >= 0 ? "ACCT-CURR-CYC-CREDIT" : "ACCT-CURR-CYC-DEBIT";
                account.put(cycleField, number(account, cycleField).add(amount));
                PostingTable.ACCOUNT.store(jdbc, account);

                var transaction = new LinkedHashMap<String, Object>();
                for (var entry : daily.entrySet()) transaction.put(entry.getKey().replace("DALYTRAN-", "TRAN-"), entry.getValue());
                transaction.put("TRAN-PROC-TS", timestamp);
                if (!PostingTable.TRANSACTION.insert(jdbc, transaction)) {
                    throw new BatchAbend("ERROR WRITING TO TRANSACTION FILE", "22");
                }
            }
            sysout.add(String.format("TRANSACTIONS PROCESSED :%09d", processed));
            sysout.add(String.format("TRANSACTIONS REJECTED  :%09d", rejected));
            sysout.add("END OF EXECUTION OF PROGRAM CBTRN02C");
            rc = rejected == 0 ? 0 : 4;
        } catch (BatchAbend failure) {
            failure.appendTo(sysout);
            rc = 12;
        }
        writeRecords(output, "TRANSACT.VSAM.KSDS", PostingTable.TRANSACTION);
        writeRecords(output, "ACCTDATA.VSAM.KSDS", PostingTable.ACCOUNT);
        writeRecords(output, "TCATBALF.VSAM.KSDS", PostingTable.BALANCE);
        writeRecords(output, "DALYREJS.G0001V00", PostingTable.REJECT);
        writeLines(output.resolve("sysout/POSTTRAN.STEP15.txt"), sysout);
        return rc;
    }

    static int validationReason(Map<String, Object> daily, Map<String, Object> xref, Map<String, Object> account) {
        if (xref == null) return 100;
        if (account == null) return 101;
        int reason = 0;
        var projected = number(account, "ACCT-CURR-BAL").add(number(daily, "DALYTRAN-AMT"));
        if (projected.compareTo(number(account, "ACCT-CREDIT-LIMIT")) > 0) reason = 102;
        if (account.get("ACCT-EXPIRAION-DATE").toString().compareTo(daily.get("DALYTRAN-ORIG-TS").toString().substring(0, 10)) < 0) reason = 103;
        return reason;
    }

    private static String reasonDescription(int reason) {
        return switch (reason) {
            case 100 -> "INVALID CARD NUMBER FOUND";
            case 101 -> "ACCOUNT RECORD NOT FOUND";
            case 102 -> "OVERLIMIT TRANSACTION";
            case 103 -> "TRANSACTION RECEIVED AFTER ACCT EXPIRATION";
            default -> throw new IllegalArgumentException("unknown rejection reason " + reason);
        };
    }

    int report(Path output, String sortStart, String sortEnd) throws IOException {
        var transactions = PostingTable.TRANSACTION.read(jdbc).stream()
                .filter(record -> within(record.get("TRAN-PROC-TS").toString(), sortStart, sortEnd))
                .sorted(java.util.Comparator.comparing(record -> record.get("TRAN-CARD-NUM").toString()))
                .toList();
        var sysout = new ArrayList<String>();
        sysout.add("START OF EXECUTION OF PROGRAM CBTRN03C");
        var report = new TransactionReport();
        int rc = 0;
        var dates = PostingTable.DATES.read(jdbc);
        try {
            if (!dates.isEmpty()) {
                var start = dates.getFirst().get("WS-START-DATE").toString();
                var end = dates.getFirst().get("WS-END-DATE").toString();
                sysout.add("Reporting from " + start + " to " + end);
                report.headers(start, end);
                String previousCard = null;
                Map<String, Object> last = null;
                for (var transaction : transactions) {
                    if (!within(transaction.get("TRAN-PROC-TS").toString(), start, end)) continue;
                    sysout.add(Layouts.TRAN.encode(transaction));
                    var card = transaction.get("TRAN-CARD-NUM").toString();
                    if (previousCard != null && !previousCard.equals(card)) report.accountTotal();
                    previousCard = card;
                    var xref = PostingTable.XREF.find(jdbc, card);
                    if (xref == null) throw new BatchAbend("INVALID CARD NUMBER : " + card, "23");
                    var type = PostingTable.TYPE.find(jdbc, transaction.get("TRAN-TYPE-CD"));
                    if (type == null) throw new BatchAbend("INVALID TRANSACTION TYPE : " + transaction.get("TRAN-TYPE-CD"), "23");
                    var category = PostingTable.CATEGORY.find(jdbc, transaction.get("TRAN-TYPE-CD"), transaction.get("TRAN-CAT-CD"));
                    if (category == null) throw new BatchAbend("INVALID TRAN CATG KEY : " + transaction.get("TRAN-TYPE-CD")
                            + String.format("%04d", number(transaction, "TRAN-CAT-CD").intValueExact()), "23");
                    report.detail(transaction, xref, type, category, start, end);
                    last = transaction;
                }
                if (last != null) {
                    sysout.add("TRAN-AMT " + org.carddemo.modern.codec.Zoned.encode(number(last, "TRAN-AMT"), 11, 2, true));
                    sysout.add("WS-PAGE-TOTAL" + org.carddemo.modern.codec.Zoned.encode(report.pageTotal(), 11, 2, true));
                }
                report.finish();
            }
            sysout.add("END OF EXECUTION OF PROGRAM CBTRN03C");
        } catch (BatchAbend failure) {
            failure.appendTo(sysout);
            rc = 12;
        }
        writeLines(output.resolve("datasets/" + PREFIX + "TRANREPT.G0001V00.txt"),
                report.lines().stream().map(line -> Layouts.REPORT.encodeLine(Map.of("FD-REPTFILE-REC", line))).toList());
        writeLines(output.resolve("sysout/TRANREPT.STEP10R.txt"), sysout);
        return rc;
    }

    private static boolean within(String timestamp, String start, String end) {
        String date = timestamp.substring(0, 10);
        return date.compareTo(start) >= 0 && date.compareTo(end) <= 0;
    }

    private void load(Path input, Path output, String dataset, String setupJob, PostingTable table) throws IOException {
        var path = input.resolve(PREFIX + dataset + ".txt");
        int processed = 0;
        int skipped = 0;
        for (var line : Files.exists(path) ? Files.readAllLines(path, StandardCharsets.ISO_8859_1) : List.<String>of()) {
            if (table.insert(jdbc, table.layout().decode(line))) processed++;
            else skipped++;
        }
        if (setupJob != null) {
            Files.createDirectories(output.resolve("setup"));
            var lines = new ArrayList<String>();
            lines.add(String.format("IDC0005I NUMBER OF RECORDS PROCESSED WAS %09d", processed));
            if (skipped > 0) lines.add(String.format("IDC3302I DUPLICATE KEYS SKIPPED %09d", skipped));
            writeLines(output.resolve("setup/" + setupJob + ".STEP15.txt"), lines);
            Files.writeString(output.resolve("setup/" + setupJob + ".rc.json"), "{\"STEP15\": " + (skipped == 0 ? 0 : 4) + "}");
        }
    }

    private void writeRecords(Path output, String dataset, PostingTable table) throws IOException {
        writeLines(output.resolve("datasets/" + PREFIX + dataset + ".txt"),
                table.read(jdbc).stream().map(table.layout()::encodeLine).toList());
    }

    static void writeReturnCodes(Path output, int postingRc, int reportRc) throws IOException {
        Files.writeString(output.resolve("rc.json"), "{\n \"POSTTRAN\": {\n  \"maxrc\": " + postingRc
                + ",\n  \"steps\": {\n   \"CLCIFIL\": 0,\n   \"OPCIFIL\": 0,\n   \"STEP05\": 0,\n   \"STEP10\": 0,\n   \"STEP15\": " + postingRc
                + ",\n   \"STEP20\": 0,\n   \"STEP25\": 0,\n   \"STEP30\": 0\n  }\n },\n \"TRANREPT\": {\n  \"maxrc\": " + reportRc
                + ",\n  \"steps\": {\n   \"PRC001\": 0,\n   \"STEP05R\": 0,\n   \"STEP10R\": " + reportRc + "\n  }\n }\n}");
    }

    private static void writeLines(Path path, List<String> lines) throws IOException {
        Files.write(path, lines, StandardCharsets.ISO_8859_1);
    }

    static BigDecimal number(Map<String, Object> record, String field) { return (BigDecimal) record.get(field); }

    private static final class BatchAbend extends RuntimeException {
        private final String diagnostic;
        private final String status;
        BatchAbend(String diagnostic, String status) { this.diagnostic = diagnostic; this.status = status; }
        void appendTo(List<String> sysout) {
            sysout.add(diagnostic);
            sysout.add("FILE STATUS IS: NNNN00" + status);
            sysout.add("ABENDING PROGRAM");
            sysout.add("CEE3ABD: USER ABEND U+0000000999");
        }
    }
}
