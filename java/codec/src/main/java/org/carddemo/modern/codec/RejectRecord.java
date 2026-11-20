package org.carddemo.modern.codec;

import java.math.BigDecimal;
import java.util.Map;

/**
 * A CBTRN02C reject (DALYREJS): the daily transaction exactly as it was read,
 * followed by the validation trailer (reason code 100-109 and its description).
 */
public record RejectRecord(String dailyTransaction, int failReason, String failReasonDesc) {

    public RejectRecord {
        if (failReason < 100 || failReason > 9999) {
            throw new IllegalArgumentException("validation reason out of range: " + failReason);
        }
    }

    /** One DALYREJS line in the recorded format. */
    public String toLine() {
        int tranLength = Layouts.DALYTRAN.length();
        String trailer = Layouts.REJTRAN.encode(Map.of(
                "REJ-FAIL-REASON", BigDecimal.valueOf(failReason),
                "REJ-FAIL-REASON-DESC", failReasonDesc)).substring(tranLength);
        return (RecordLayout.pad(dailyTransaction, tranLength) + trailer).stripTrailing();
    }
}
