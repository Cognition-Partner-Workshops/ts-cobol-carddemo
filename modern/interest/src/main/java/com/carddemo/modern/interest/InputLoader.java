package com.carddemo.modern.interest;

import org.carddemo.modern.codec.Layouts;
import org.carddemo.modern.codec.RecordLayout;
import com.carddemo.modern.interest.Records.Account;
import com.carddemo.modern.interest.Records.CardXref;
import com.carddemo.modern.interest.Records.DisclosureGroup;
import com.carddemo.modern.interest.Records.TranCatBal;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The chain's setup jobs (DEFGDGB, ACCTFILE, XREFFILE, TCATBALF, DISCGRP): IDCAMS DELETE/DEFINE,
 * REPRO of the flat input into the cluster and, for the xref, the AIX/PATH/BLDINDEX.
 * Here that is truncate + load of the PostgreSQL tables from the fixture's input datasets.
 */
@Component
public class InputLoader {

    /** Setup job steps the JCL runner records into the first job's rc.json (see record.py). */
    static final Map<String, List<String>> SETUP_STEPS = new LinkedHashMap<>();

    static {
        SETUP_STEPS.put("DEFGDGB", List.of("STEP05"));
        SETUP_STEPS.put("ACCTFILE", List.of("STEP05", "STEP10", "STEP15"));
        SETUP_STEPS.put("XREFFILE", List.of("STEP05", "STEP10", "STEP15", "STEP20", "STEP25", "STEP30"));
        SETUP_STEPS.put("TCATBALF", List.of("STEP05", "STEP10", "STEP15"));
        SETUP_STEPS.put("DISCGRP", List.of("STEP05", "STEP10", "STEP15"));
    }

    private final Estate estate;
    private final InterestRepository repository;

    public InputLoader(Estate estate, InterestRepository repository) {
        this.estate = estate;
        this.repository = repository;
    }

    /** Loads the case and returns the setup step return codes (all 0, or the load fails loudly). */
    @Transactional
    public Map<String, Integer> load(Path caseDir) {
        repository.clear();
        load(caseDir, "AWS.M2.CARDDEMO.ACCTDATA.PS", Layouts.ACCOUNT, Account::of, repository::insert);
        load(caseDir, "AWS.M2.CARDDEMO.CARDXREF.PS", Layouts.CARD_XREF, CardXref::of, repository::insert);
        load(caseDir, "AWS.M2.CARDDEMO.TCATBALF.PS", Layouts.TRAN_CAT_BAL, TranCatBal::of, repository::insert);
        load(caseDir, "AWS.M2.CARDDEMO.DISCGRP.PS", Layouts.DIS_GROUP, DisclosureGroup::of, repository::insert);
        Map<String, Integer> rcs = new LinkedHashMap<>();
        SETUP_STEPS.values().forEach(steps -> steps.forEach(step -> rcs.put(step, 0)));
        return rcs;
    }

    private <T> void load(Path caseDir, String dsn, RecordLayout layout, Function<Map<String, Object>, T> mapper,
                          Consumer<T> sink) {
        int lrecl = estate.chain().get("inputs").get(dsn).asInt();
        if (layout.length() != lrecl) {
            throw new IllegalStateException(dsn + ": " + layout.copybook() + " is " + layout.length() + " bytes, LRECL " + lrecl);
        }
        Path src = caseDir.resolve("input").resolve(dsn + ".txt");
        if (!Files.exists(src)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(src, StandardCharsets.ISO_8859_1)) {
                sink.accept(mapper.apply(layout.decode(line, true)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
