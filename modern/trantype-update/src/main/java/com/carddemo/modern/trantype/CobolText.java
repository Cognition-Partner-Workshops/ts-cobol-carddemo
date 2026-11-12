package com.carddemo.modern.trantype;

/** The two COBOL text operations COBTUPDT relies on. */
final class CobolText {

    private CobolText() {
    }

    /**
     * {@code STRING ... DELIMITED BY SIZE INTO target}: the parts are copied from the first
     * character and stop when the target is full; the rest of the target keeps its old content
     * (STRING does not space-fill the receiving field).
     */
    static void stringInto(char[] target, String... parts) {
        String source = String.join("", parts);
        source.getChars(0, Math.min(source.length(), target.length), target, 0);
    }

    /**
     * MOVE to a {@code PIC ----9} numeric-edited item: four high-order digits kept (higher ones
     * dropped), leading zeros suppressed, a floating minus sign for negative values, right-justified
     * in five characters.
     */
    static String minusMinus9(long value) {
        String digits = Long.toString(Math.abs(value) % 10_000);
        String edited = value < 0 ? "-" + digits : digits;
        return " ".repeat(5 - edited.length()) + edited;
    }
}
