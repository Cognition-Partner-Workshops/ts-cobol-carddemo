package org.carddemo.modern.interest;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.carddemo.modern.codec.Layouts;
import org.carddemo.modern.codec.RecordLayout;
import org.springframework.jdbc.core.JdbcTemplate;

final class InterestRun {
    private static final String PREFIX = "AWS.M2.CARDDEMO.";
    private static final String STAMP = "2022-07-05-10.00.00.000000";
    private final JdbcTemplate jdbc;

    InterestRun(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    static BigDecimal monthlyInterest(BigDecimal balance, BigDecimal rate) {
        // Legacy truncation defect: COMPUTE into WS-MONTHLY-INT PIC S9(09)V99 has no ROUNDED.
        return balance.multiply(rate).divide(BigDecimal.valueOf(1200), 2, RoundingMode.DOWN);
    }

    void run(Path input, Path output) throws IOException {
        jdbc.execute("CREATE TABLE IF NOT EXISTS records (dataset text NOT NULL, record_key text NOT NULL, record text NOT NULL, PRIMARY KEY(dataset, record_key))");
        jdbc.update("DELETE FROM records");
        load(input, "ACCTDATA", Layouts.ACCOUNT, 11);
        load(input, "CARDXREF", Layouts.XREF, 16);
        load(input, "DISCGRP", Layouts.DISCLOSURE, 16);
        load(input, "TCATBALF", Layouts.CATEGORY_BALANCE, 17);

        var accounts = new LinkedHashMap<BigDecimal, Map<String, Object>>();
        for (var record : read("ACCTDATA", Layouts.ACCOUNT)) accounts.put(number(record, "ACCT-ID"), record);
        var cards = new LinkedHashMap<BigDecimal, String>();
        for (var record : read("CARDXREF", Layouts.XREF)) cards.putIfAbsent(number(record, "XREF-ACCT-ID"), record.get("XREF-CARD-NUM").toString());
        var rates = new LinkedHashMap<String, BigDecimal>();
        for (var record : read("DISCGRP", Layouts.DISCLOSURE)) rates.put(rateKey(record.get("DIS-ACCT-GROUP-ID").toString(), record.get("DIS-TRAN-TYPE-CD"), record.get("DIS-TRAN-CAT-CD")), number(record, "DIS-INT-RATE"));

        var sysout = new ArrayList<String>();
        sysout.add("START OF EXECUTION OF PROGRAM CBACT04C");
        var transactions = new ArrayList<Map<String, Object>>();
        for (var balance : read("TCATBALF", Layouts.CATEGORY_BALANCE)) {
            sysout.add(Layouts.CATEGORY_BALANCE.encode(balance));
            var accountId = number(balance, "TRANCAT-ACCT-ID");
            var account = accounts.get(accountId);
            if (account == null) throw new IllegalStateException("ACCOUNT NOT FOUND: " + accountId);
            if (!cards.containsKey(accountId)) throw new IllegalStateException("XREF NOT FOUND: " + accountId);
            var type = balance.get("TRANCAT-TYPE-CD");
            var category = balance.get("TRANCAT-CD");
            var rate = rates.get(rateKey(account.get("ACCT-GROUP-ID").toString(), type, category));
            if (rate == null) {
                sysout.add("DISCLOSURE GROUP RECORD MISSING");
                sysout.add("TRY WITH DEFAULT GROUP CODE");
                rate = rates.get(rateKey("DEFAULT", type, category));
            }
            if (rate == null) throw new IllegalStateException("ERROR READING DEFAULT DISCLOSURE GROUP");
            account.put("ACCT-CURR-CYC-CREDIT", BigDecimal.ZERO);
            account.put("ACCT-CURR-CYC-DEBIT", BigDecimal.ZERO);
            if (rate.signum() == 0) continue;
            var amount = monthlyInterest(number(balance, "TRAN-CAT-BAL"), rate);
            account.put("ACCT-CURR-BAL", number(account, "ACCT-CURR-BAL").add(amount));
            var transaction = new LinkedHashMap<String, Object>();
            transaction.put("TRAN-ID", "2022071800" + String.format("%06d", transactions.size() + 1));
            transaction.put("TRAN-TYPE-CD", "01");
            transaction.put("TRAN-CAT-CD", 5);
            transaction.put("TRAN-SOURCE", "System");
            transaction.put("TRAN-DESC", "Int. for a/c " + String.format("%011d", accountId.longValueExact()));
            transaction.put("TRAN-AMT", amount);
            transaction.put("TRAN-CARD-NUM", cards.get(accountId));
            transaction.put("TRAN-ORIG-TS", STAMP);
            transaction.put("TRAN-PROC-TS", STAMP);
            transactions.add(transaction);
        }
        for (var account : accounts.values()) store("ACCTDATA", Layouts.ACCOUNT.encode(account).substring(0, 11), Layouts.ACCOUNT.encode(account));
        for (var transaction : transactions) store("SYSTRAN", transaction.get("TRAN-ID").toString(), Layouts.TRAN.encode(transaction));
        Files.createDirectories(output.resolve("datasets"));
        Files.createDirectories(output.resolve("sysout"));
        write(output.resolve("datasets/" + PREFIX + "ACCTDATA.VSAM.KSDS.txt"), Layouts.ACCOUNT, read("ACCTDATA", Layouts.ACCOUNT));
        write(output.resolve("datasets/" + PREFIX + "SYSTRAN.G0001V00.txt"), Layouts.TRAN, read("SYSTRAN", Layouts.TRAN));
        sysout.add("END OF EXECUTION OF PROGRAM CBACT04C");
        Files.write(output.resolve("sysout/INTCALC.STEP15.txt"), sysout, StandardCharsets.ISO_8859_1);
        Files.writeString(output.resolve("rc.json"), "{\n \"INTCALC\": {\n  \"maxrc\": 0,\n  \"steps\": {\n   \"STEP05\": 0,\n   \"STEP10\": 0,\n   \"STEP15\": 0,\n   \"STEP20\": 0,\n   \"STEP25\": 0,\n   \"STEP30\": 0\n  }\n }\n}");
    }

    private void load(Path input, String dataset, RecordLayout layout, int keyLength) throws IOException {
        for (var line : Files.readAllLines(input.resolve(PREFIX + dataset + ".PS.txt"), StandardCharsets.ISO_8859_1)) {
            var encoded = layout.encode(layout.decode(line));
            store(dataset, encoded.substring(0, keyLength), encoded);
        }
    }

    private void store(String dataset, String key, String record) {
        jdbc.update("INSERT INTO records VALUES (?, ?, ?) ON CONFLICT (dataset, record_key) DO UPDATE SET record = EXCLUDED.record", dataset, key, record);
    }

    private List<Map<String, Object>> read(String dataset, RecordLayout layout) {
        return jdbc.query("SELECT record FROM records WHERE dataset = ? ORDER BY record_key COLLATE \"C\"", (rs, row) -> layout.decode(rs.getString(1)), dataset);
    }

    private static void write(Path path, RecordLayout layout, List<Map<String, Object>> records) throws IOException {
        Files.write(path, records.stream().map(layout::encodeLine).toList(), StandardCharsets.ISO_8859_1);
    }

    private static BigDecimal number(Map<String, Object> record, String field) { return (BigDecimal) record.get(field); }
    private static String rateKey(String group, Object type, Object category) { return group.stripTrailing() + "/" + type + "/" + category; }
}
