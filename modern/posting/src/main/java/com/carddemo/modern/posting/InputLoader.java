package com.carddemo.modern.posting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.carddemo.modern.codec.Layouts;
import org.carddemo.modern.codec.RecordLayout;
import org.carddemo.modern.codec.Field;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** IDCAMS DELETE/DEFINE/REPRO setup jobs, including initial transaction-master contents. */
@Component
public class InputLoader {
    static final String PREFIX = "AWS.M2.CARDDEMO.";
    private final PostingRepository repo;
    private final Estate estate;
    private String startDate;
    private String endDate;

    public InputLoader(PostingRepository repo, Estate estate) { this.repo = repo; this.estate = estate; }

    @Transactional
    public void load(Path dir) throws IOException {
        repo.clear();
        for (String line : lines(dir, "ACCTDATA.PS")) {
            var r = Layouts.ACCOUNT.decode(line,true);
            if (repo.account(PostingRepository.number(r,"ACCT-ID").longValueExact()) == null) repo.account(r);
        }
        for (String line : lines(dir, "TCATBALF.PS")) {
            var r = Layouts.TRAN_CAT_BAL.decode(line,true);
            if (repo.category(PostingRepository.number(r,"TRANCAT-ACCT-ID").longValueExact(),r.get("TRANCAT-TYPE-CD").toString(),
                    PostingRepository.number(r,"TRANCAT-CD").intValueExact()) == null) repo.category(r);
        }
        for (String line : lines(dir, "CARDXREF.PS")) {
            var r = Layouts.CARD_XREF.decode(line);
            repo.jdbc().update("INSERT INTO posting.card_xref VALUES (?,?,?) ON CONFLICT DO NOTHING", r.get("XREF-CARD-NUM"), r.get("XREF-ACCT-ID"), line);
        }
        for (String line : lines(dir, "DISCGRP.PS")) {
            var r = Layouts.DIS_GROUP.decode(line);
            repo.jdbc().update("INSERT INTO posting.disclosure_group VALUES (?,?,?,?) ON CONFLICT DO NOTHING", r.get("DIS-ACCT-GROUP-ID"),
                    r.get("DIS-TRAN-TYPE-CD"), r.get("DIS-TRAN-CAT-CD"), line);
        }
        for (String line : lines(dir, "TRANTYPE.PS")) {
            var r = Layouts.TRAN_TYPE.decode(line);
            repo.jdbc().update("INSERT INTO posting.transaction_type VALUES (?,?) ON CONFLICT DO NOTHING", r.get("TRAN-TYPE"), r.get("TRAN-TYPE-DESC"));
        }
        for (String line : lines(dir, "TRANCATG.PS")) {
            var r = Layouts.TRAN_CATEGORY.decode(line);
            repo.jdbc().update("INSERT INTO posting.transaction_category VALUES (?,?,?) ON CONFLICT DO NOTHING", r.get("TRAN-TYPE-CD"),
                    r.get("TRAN-CAT-CD"), r.get("TRAN-CAT-TYPE-DESC"));
        }
        for (String line : lines(dir, "DALYTRAN.PS.INIT")) {
            var r = Layouts.TRAN.decode(line,true);
            repo.jdbc().update("INSERT INTO posting.transaction_master VALUES (?,?,?,?) ON CONFLICT DO NOTHING",
                    r.get("TRAN-ID"), r.get("TRAN-CARD-NUM"),r.get("TRAN-PROC-TS").toString().substring(0,10),line);
        }
        long seq = 0;
        for (String line : lines(dir, "DALYTRAN.PS"))
            repo.jdbc().update("INSERT INTO posting.daily_transaction VALUES (?,?)", ++seq, line);
        var dateLines = lines(dir,"DATEPARM");
        startDate = dateLines.isEmpty() ? null : dateLines.getFirst().substring(0,10);
        endDate = dateLines.isEmpty() ? null : dateLines.getFirst().substring(11,21);
    }

    private List<String> lines(Path dir, String suffix) throws IOException {
        String dsn = PREFIX + suffix;
        if (!estate.chain().get("inputs").has(dsn)) throw new IllegalArgumentException("Undeclared input " + dsn);
        Path p = dir.resolve("input").resolve(dsn + ".txt");
        if (!Files.exists(p)) return List.of();
        int length = estate.chain().get("inputs").get(dsn).asInt();
        var raw = new RecordLayout(dsn, List.of(Field.text("RECORD", 0, length)));
        return Files.readAllLines(p, StandardCharsets.ISO_8859_1).stream()
                .map(s -> raw.encode(raw.decode(s))).toList();
    }

    String startDate() { return startDate; }
    String endDate() { return endDate; }
}
