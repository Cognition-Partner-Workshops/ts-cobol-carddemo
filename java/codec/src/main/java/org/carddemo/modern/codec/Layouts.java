package org.carddemo.modern.codec;

import static org.carddemo.modern.codec.Field.filler;
import static org.carddemo.modern.codec.Field.text;
import static org.carddemo.modern.codec.Field.zoned;

import java.util.ArrayList;
import java.util.List;

/**
 * Record layouts converted from the estate copybooks (first pass, one entry per
 * copybook). Offsets follow the copybook order; FILLER is kept so record lengths
 * match the LRECL of the datasets.
 */
public final class Layouts {

    private Layouts() {
    }

    /** CVTRA06Y DALYTRAN-RECORD, RECLN 350. */
    public static final RecordLayout DALYTRAN = new RecordLayout("CVTRA06Y", transaction("DALYTRAN"));

    /** CVTRA05Y TRAN-RECORD, RECLN 350. */
    public static final RecordLayout TRAN = new RecordLayout("CVTRA05Y", transaction("TRAN"));

    /** REJTRAN REJECT-RECORD: the 350-byte daily transaction plus the 80-byte validation trailer. */
    public static final RecordLayout REJTRAN = new RecordLayout("REJTRAN", rejectRecord());

    private static List<Field> transaction(String prefix) {
        return List.of(
                text(prefix + "-ID", 0, 16),
                text(prefix + "-TYPE-CD", 16, 2),
                zoned(prefix + "-CAT-CD", 18, 4, 0, false),
                text(prefix + "-SOURCE", 22, 10),
                text(prefix + "-DESC", 32, 100),
                zoned(prefix + "-AMT", 132, 11, 2, true),
                zoned(prefix + "-MERCHANT-ID", 143, 9, 0, false),
                text(prefix + "-MERCHANT-NAME", 152, 50),
                text(prefix + "-MERCHANT-CITY", 202, 50),
                text(prefix + "-MERCHANT-ZIP", 252, 10),
                text(prefix + "-CARD-NUM", 262, 16),
                text(prefix + "-ORIG-TS", 278, 26),
                text(prefix + "-PROC-TS", 304, 26),
                filler(330, 20));
    }

    private static List<Field> rejectRecord() {
        List<Field> fields = new ArrayList<>(transaction("DALYTRAN"));
        // 05 REJ-VALIDATION-TRAILER
        fields.add(filler(350, 4));
        fields.add(text("REJ-FAIL-REASON-DESC", 354, 76));
        return fields;
    }
}
