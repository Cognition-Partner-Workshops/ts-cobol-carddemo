package com.carddemo.interest.recon;

/**
 * The class of one side-by-side difference (strategy item SS4): a defect, an open decision (its
 * id) or a mapping gap (its id). {@code accepted} is true only for a difference the chosen
 * comparison rule allows; every other difference stays open against the exit of the period.
 */
public record DifferenceClass(Kind kind, String id, boolean accepted, String reason) {

    public enum Kind { DEFECT, DECISION, GAP }

    static DifferenceClass defect(String reason) {
        return new DifferenceClass(Kind.DEFECT, "defect", false, reason);
    }

    static DifferenceClass decision(String id, boolean accepted, String reason) {
        return new DifferenceClass(Kind.DECISION, id, accepted, reason);
    }

    static DifferenceClass gap(String id, String reason) {
        return new DifferenceClass(Kind.GAP, id, false, reason);
    }

    /** What stands on the difference line: "defect", or the id of the decision or gap. */
    public String label() {
        return kind == Kind.DEFECT ? "defect" : kind.name().toLowerCase(java.util.Locale.ROOT) + " " + id;
    }
}
