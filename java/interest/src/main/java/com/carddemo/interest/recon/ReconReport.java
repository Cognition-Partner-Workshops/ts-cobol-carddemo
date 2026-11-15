package com.carddemo.interest.recon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * The reconciliation report of one load and run: the lines of RC1-RC6, the figures RC5 is made of,
 * and the profile of TL4. It decides nothing (REL-04 and REL-05 are open); it states figures.
 */
public record ReconReport(String title, Optional<Long> runId, Optional<String> runStatus, String inputDir, String format,
        List<ReconLine> lines, Rc5Figures rc5, List<ProfileLine> profile, List<String> notes) {

    /** RC6 needs the mainframe; its line is in the report but is not part of the result of an off-host run. */
    static final String OFF_HOST_CHECK = "RC6";

    /** The figures of RC5 in words: what was written, how the balances moved, what was not added (INT-Q04). */
    public record Rc5Figures(Optional<Long> transactions, Optional<BigDecimal> transactionAmountSum,
            Optional<BigDecimal> balanceChange, Optional<BigDecimal> interestNotAdded, Optional<String> lastAccount) {
    }

    public List<ReconLine> linesOf(String check) {
        return lines.stream().filter(line -> line.check().equals(check)).toList();
    }

    public long count(ReconStatus status) {
        return lines.stream().filter(line -> !line.check().equals(OFF_HOST_CHECK)).filter(line -> line.status() == status).count();
    }

    /** RC1-RC5 all EQUAL. A line that is NOT AVAILABLE is not equal. */
    public boolean reconciled() {
        return lines.stream().filter(line -> !line.check().equals(OFF_HOST_CHECK))
                .allMatch(line -> line.status() == ReconStatus.EQUAL);
    }

    public String toMarkdown() {
        StringBuilder out = new StringBuilder();
        out.append("# Reconciliation report - ").append(title).append("\n\n");
        out.append("- interest_run: ").append(runId.map(String::valueOf).orElse(ReconLine.MISSING))
                .append(", status ").append(runStatus.orElse(ReconLine.MISSING)).append('\n');
        out.append("- unloaded files: ").append(inputDir).append(" (format ").append(format).append(")\n");
        out.append("- result RC1-RC5: **").append(reconciled() ? "RECONCILED" : "NOT RECONCILED").append("** - ")
                .append(count(ReconStatus.EQUAL)).append(" EQUAL, ").append(count(ReconStatus.DIFFERENT)).append(" DIFFERENT, ")
                .append(count(ReconStatus.NOT_AVAILABLE)).append(" NOT AVAILABLE\n\n");
        out.append("## Checks\n\n| Check | Item | Figure | From | Figure | From | Result |\n|---|---|---|---|---|---|---|\n");
        for (ReconLine line : lines) {
            out.append("| ").append(line.check()).append(" | ").append(line.item()).append(" | ").append(line.leftText())
                    .append(" | ").append(line.leftSource()).append(" | ").append(line.rightText()).append(" | ")
                    .append(line.rightSource()).append(" | ").append(line.status().label()).append(" |\n");
        }
        out.append("\n## After the calculation (RC5)\n\n");
        out.append("- transactions written: ").append(text(rc5.transactions())).append(", summing ")
                .append(amount(rc5.transactionAmountSum())).append('\n');
        out.append("- balance change (sum of ACCT-CURR-BAL now minus as loaded): ").append(amount(rc5.balanceChange())).append('\n');
        out.append("- interest not added to the last account (INT-Q04): ").append(amount(rc5.interestNotAdded()))
                .append(" (account ").append(rc5.lastAccount().orElse(ReconLine.MISSING)).append(")\n");
        out.append("\n## Profile of the load (TL4)\n\n| Finding | Asked for by | Count |\n|---|---|---|\n");
        for (ProfileLine line : profile) {
            out.append("| ").append(line.finding()).append(" | ").append(line.asksFor()).append(" | ").append(line.countText())
                    .append(" |\n");
        }
        if (!notes.isEmpty()) {
            out.append("\n## Notes\n\n");
            notes.forEach(note -> out.append("- ").append(note).append('\n'));
        }
        return out.toString();
    }

    private static String text(Optional<Long> figure) {
        return figure.map(String::valueOf).orElse(ReconLine.MISSING);
    }

    private static String amount(Optional<BigDecimal> figure) {
        return figure.map(BigDecimal::toPlainString).orElse(ReconLine.MISSING);
    }
}
