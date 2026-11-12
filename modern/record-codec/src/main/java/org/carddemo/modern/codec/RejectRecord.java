package org.carddemo.modern.codec;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A CBTRN02C reject (DALYREJS): the daily transaction as it was read, followed
 * by the validation trailer (reason code 100-109 and its description).
 */
public record RejectRecord(String dailyTransaction, int failReason, String failReasonDesc) {

    public RejectRecord {
        if (failReason < 100 || failReason > 9999) {
            throw new IllegalArgumentException("validation reason out of range: " + failReason);
        }
    }

    public Map<String, Object> values() {
        Map<String, Object> values = new LinkedHashMap<>(Layouts.DALYTRAN.decode(dailyTransaction));
        values.put("REJ-FAIL-REASON", BigDecimal.valueOf(failReason));
        values.put("REJ-FAIL-REASON-DESC", failReasonDesc);
        return values;
    }

    /** One DALYREJS line in the recorded format. */
    public String toLine() {
        return Layouts.REJTRAN.encodeLine(values());
    }
}
