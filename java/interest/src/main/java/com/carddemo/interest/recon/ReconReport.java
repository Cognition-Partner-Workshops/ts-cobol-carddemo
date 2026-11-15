package com.carddemo.interest.recon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.carddemo.interest.load.ExtractProfile;
import com.carddemo.interest.load.RejectedRecord;
import com.carddemo.interest.recon.ReconTables.RunRow;

/**
 * The reconciliation report of one load and one run: the lines of RC1 to RC6, the figures of the
 * reconciliation table of strategy-interest.md, the trial-load profile (TL4) and the reject list (TL3).
 */
public record ReconReport(String name, String inputDir, String outputDir, Optional<RunRow> run, String runWhyNot,
        List<CheckLine> lines, Map<String, Figure> figures, Optional<ExtractProfile> profile, List<RejectedRecord> rejects) {

    /** The checks that can be shown off-host; RC6 is listed and never part of the result. */
    public static final List<String> CHECKS = List.of("RC1", "RC2", "RC3", "RC4", "RC5");

    public ReconReport {
        lines = List.copyOf(lines);
        figures = Collections.unmodifiableMap(new LinkedHashMap<>(figures));
        rejects = List.copyOf(rejects);
    }

    public List<CheckLine> lines(String check) {
        return lines.stream().filter(line -> line.check().equals(check)).toList();
    }

    public Verdict verdict(String check) {
        return Verdict.of(lines(check).stream().map(CheckLine::verdict).toList());
    }

    /** EQUAL only when every line of RC1 to RC5 is EQUAL. */
    public Verdict verdict() {
        return Verdict.of(CHECKS.stream().map(this::verdict).toList());
    }

    public Figure figure(String column) {
        return figures.get(column);
    }

    public String summaryLine() {
        return "interest/" + name + " [recon]: " + verdict().label() + " ("
                + String.join(", ", CHECKS.stream().map(check -> check + " " + verdict(check).label()).toList())
                + "; RC6 " + verdict("RC6").label() + ": needs a mainframe unload)";
    }

    public String markdown() {
        List<String> out = new ArrayList<>();
        out.add("## Reconciliation report - " + name + ": **" + verdict().label() + "**");
        out.add("");
        out.add("Unloaded files: `" + inputDir + "`. Exported files: `" + outputDir + "`. Run: "
                + run.map(row -> "`interest_run.run_id` " + row.runId() + ", status " + row.status() + ", PARM " + row.parmDate())
                        .orElse(Verdict.NOT_AVAILABLE.label() + " (" + runWhyNot + ")")
                + ". The report reads the files, `carddemo.interest_run` and the tables in one read-only transaction; it writes nothing.");
        out.add("");
        out.add("| Check | Result | Lines EQUAL | Lines DIFFERENT | Lines NOT AVAILABLE |");
        out.add("|---|---|---|---|---|");
        for (String check : CHECKS) {
            out.add("| " + check + " | **" + verdict(check).label() + "** | " + count(check, Verdict.EQUAL) + " | "
                    + count(check, Verdict.DIFFERENT) + " | " + count(check, Verdict.NOT_AVAILABLE) + " |");
        }
        out.add("| RC6 | NOT AVAILABLE - needs ACCTDATA unloaded again on the mainframe after the write-back; not part of the result | 0 | 0 | "
                + count("RC6", Verdict.NOT_AVAILABLE) + " |");
        out.add("");
        out.add("### Figures (the columns of the reconciliation table of strategy-interest.md, from interest_run and the tables)");
        out.add("");
        out.add("| " + String.join(" | ", figures.keySet()) + " |");
        out.add("|" + "---|".repeat(figures.size()));
        out.add("| " + String.join(" | ", figures.values().stream().map(figure -> cell(figure.text())).toList()) + " |");
        out.add("");
        out.add("### Lines");
        out.add("");
        out.add("| Check | Figure | First side | Value | Second side | Value | Result | Note |");
        out.add("|---|---|---|---|---|---|---|---|");
        for (CheckLine line : lines) {
            out.add("| " + line.check() + " | " + cell(line.what()) + " | " + cell(line.firstSide()) + " | " + cell(line.first().text())
                    + " | " + cell(line.secondSide()) + " | " + cell(line.second().text()) + " | **" + line.verdict().label()
                    + "** | " + cell(line.note()) + " |");
        }
        out.add("");
        out.add("### Trial-load profile (TL4), from the unloaded files");
        out.add("");
        if (profile.isEmpty()) {
            out.add(Verdict.NOT_AVAILABLE.label() + ": one of the four files is missing or has a record on the reject list.");
        } else {
            ExtractProfile found = profile.get();
            out.add("| What the mapping gaps ask for | Id | Found |");
            out.add("|---|---|---|");
            out.add("| Accounts with several cards | G08, INT-H03 | " + found.accountsWithSeveralCards() + " |");
            out.add("| Type codes of the balances | G06 | " + cell(String.join(", ", found.balanceTypeCodes())) + " |");
            out.add("| Accounts with mixed type codes (digits and letters) | G06 | " + found.accountsWithMixedTypeCodes() + " |");
            out.add("| Minus zero amounts | G03 | " + found.minusZeroAmounts() + " |");
            out.add("| F-signed amounts | G02 | " + found.fSignedAmounts() + " |");
            out.add("| Fillers that are not blank | E4 | " + found.nonBlankFillers() + " |");
            out.add("| Accounts that are not active | INT-H04 | " + found.accountsNotActive() + " |");
            out.add("| Balances without a rate and without a DEFAULT rate | INT-R08 | " + found.balancesWithoutRate() + " |");
            out.add("");
            out.add(found.isClean() ? "The profile is clean: nothing a mapping gap or an open rule asks about was found."
                    : "The profile is not clean: the rows above that are not 0 need the owner named for their id.");
        }
        out.add("");
        out.add("### Reject list (TL3)");
        out.add("");
        if (rejects.isEmpty()) {
            out.add("No record rejected.");
        } else {
            out.add("| File | Record | Field | Reason |");
            out.add("|---|---|---|---|");
            for (RejectedRecord reject : rejects) {
                out.add("| " + cell(reject.file()) + " | " + reject.recordNumber() + " | " + cell(reject.field()) + " | "
                        + cell(reject.reason()) + " |");
            }
        }
        out.add("");
        return String.join("\n", out) + "\n";
    }

    private long count(String check, Verdict verdict) {
        return lines(check).stream().filter(line -> line.verdict() == verdict).count();
    }

    static String cell(String text) {
        return text == null ? "" : text.replace("|", "\\|").replace("\n", "<br>");
    }
}
