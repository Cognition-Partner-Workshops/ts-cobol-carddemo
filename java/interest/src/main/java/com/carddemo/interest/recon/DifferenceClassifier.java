package com.carddemo.interest.recon;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Gives every side-by-side difference its class (strategy item SS4). A difference is a decision or
 * a gap only when it has exactly the shape that decision or gap produces; anything else is a
 * defect, so nothing is left unclassified and nothing is excused by a loose rule.
 *
 * <ul>
 * <li>INT-Q03 - the timestamps are the moment each job ran. Under {@link TimestampRule#FORMAT} two
 * well-formed values that differ are accepted; under {@link TimestampRule#EXACT} they are a defect.
 * <li>INT-Q01 - an amount one cent apart: the target rounded where the legacy job cuts off.
 * <li>INT-Q02 - a transaction id with the same counter and another ten-character prefix.
 * <li>INT-Q04 - the three amounts of the account break on the last account record: the target
 * updated the account the legacy job leaves as it was.
 * <li>G05 - a description that agrees as far as the target wrote it: bytes the program never sets.
 * </ul>
 */
public final class DifferenceClassifier {

    /** YYYY-MM-DD-HH.MM.SS.hh0000 (data-map gap G10). */
    private static final Pattern TIMESTAMP = Pattern.compile("\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{2}0000");
    private static final Pattern AMOUNT = Pattern.compile("-?\\d+\\.\\d{2}");
    private static final Pattern RECORD_NUMBER = Pattern.compile(" rec (\\d+) ");
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");
    private static final int ID_PREFIX_LENGTH = 10;
    private static final int ID_LENGTH = 16;

    private static final Set<String> TIMESTAMP_FIELDS = Set.of("TRAN-ORIG-TS", "TRAN-PROC-TS");
    private static final Set<String> ACCOUNT_BREAK_FIELDS = Set.of("ACCT-CURR-BAL", "ACCT-CURR-CYC-CREDIT", "ACCT-CURR-CYC-DEBIT");
    private static final String AMOUNT_FIELD = "TRAN-AMT";
    private static final String ID_FIELD = "TRAN-ID";
    private static final String DESCRIPTION_FIELD = "TRAN-DESC";

    private final TimestampRule timestampRule;
    private final long lastAccountRecord;

    /**
     * @param lastAccountRecord the number of records of the legacy ACCTDATA image; 0 when unknown
     */
    public DifferenceClassifier(TimestampRule timestampRule, long lastAccountRecord) {
        this.timestampRule = timestampRule;
        this.lastAccountRecord = lastAccountRecord;
    }

    public DifferenceClass classify(ComparedDifference difference) {
        String field = difference.field();
        String legacy = difference.legacy();
        String target = difference.target();
        if (TIMESTAMP_FIELDS.contains(field)) {
            boolean wellFormed = TIMESTAMP.matcher(legacy).matches() && TIMESTAMP.matcher(target).matches();
            if (!wellFormed) {
                return DifferenceClass.defect("a timestamp is not of the form YYYY-MM-DD-HH.MM.SS.hh0000");
            }
            return timestampRule == TimestampRule.FORMAT
                    ? DifferenceClass.decision("INT-Q03", true, "both well-formed; compared for format only (SS3)")
                    : DifferenceClass.defect("timestamps compared exactly (SS3) and they differ");
        }
        if (AMOUNT_FIELD.equals(field) && AMOUNT.matcher(legacy).matches() && AMOUNT.matcher(target).matches()
                && new BigDecimal(legacy).subtract(new BigDecimal(target)).abs().compareTo(ONE_CENT) == 0) {
            return DifferenceClass.decision("INT-Q01", false, "one cent apart: rounding instead of cutting off");
        }
        if (ID_FIELD.equals(field) && legacy.length() == ID_LENGTH && target.length() == ID_LENGTH
                && legacy.substring(ID_PREFIX_LENGTH).equals(target.substring(ID_PREFIX_LENGTH))) {
            return DifferenceClass.decision("INT-Q02", false, "same counter, another prefix");
        }
        if (ACCOUNT_BREAK_FIELDS.contains(field) && lastAccountRecord > 0 && recordNumber(difference.where()) == lastAccountRecord) {
            return DifferenceClass.decision("INT-Q04", false,
                    "an amount of the account break on the last account record; confirm that this account ends TCATBALF");
        }
        if (DESCRIPTION_FIELD.equals(field) && !target.isEmpty() && legacy.length() > target.length() && legacy.startsWith(target)) {
            return DifferenceClass.gap("G05", "the legacy record holds bytes after the text the program sets");
        }
        return DifferenceClass.defect("no decision or gap produces this difference");
    }

    private static long recordNumber(String where) {
        var matcher = RECORD_NUMBER.matcher(where);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : -1;
    }
}
