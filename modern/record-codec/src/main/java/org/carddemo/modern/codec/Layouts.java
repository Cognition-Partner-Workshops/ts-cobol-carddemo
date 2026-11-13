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

    public static final RecordLayout ACCOUNT = new RecordLayout("CVACT01Y", List.of(
            zoned("ACCT-ID", 0, 11, 0, false), text("ACCT-ACTIVE-STATUS", 11, 1),
            zoned("ACCT-CURR-BAL", 12, 12, 2, true), zoned("ACCT-CREDIT-LIMIT", 24, 12, 2, true),
            zoned("ACCT-CASH-CREDIT-LIMIT", 36, 12, 2, true), text("ACCT-OPEN-DATE", 48, 10),
            text("ACCT-EXPIRAION-DATE", 58, 10), text("ACCT-REISSUE-DATE", 68, 10),
            zoned("ACCT-CURR-CYC-CREDIT", 78, 12, 2, true), zoned("ACCT-CURR-CYC-DEBIT", 90, 12, 2, true),
            text("ACCT-ADDR-ZIP", 102, 10), text("ACCT-GROUP-ID", 112, 10), filler(122, 178)));

    public static final RecordLayout XREF = new RecordLayout("CVACT03Y", List.of(
            text("XREF-CARD-NUM", 0, 16), zoned("XREF-CUST-ID", 16, 9, 0, false),
            zoned("XREF-ACCT-ID", 25, 11, 0, false), filler(36, 14)));

    public static final RecordLayout CATEGORY_BALANCE = new RecordLayout("CVTRA01Y", List.of(
            zoned("TRANCAT-ACCT-ID", 0, 11, 0, false), text("TRANCAT-TYPE-CD", 11, 2),
            zoned("TRANCAT-CD", 13, 4, 0, false), zoned("TRAN-CAT-BAL", 17, 11, 2, true), filler(28, 22)));

    public static final RecordLayout DISCLOSURE = new RecordLayout("CVTRA02Y", List.of(
            text("DIS-ACCT-GROUP-ID", 0, 10), text("DIS-TRAN-TYPE-CD", 10, 2),
            zoned("DIS-TRAN-CAT-CD", 12, 4, 0, false), zoned("DIS-INT-RATE", 16, 6, 2, true), filler(22, 28)));

    /** COBTUPDT FD TR-RECORD read INTO WS-INPUT-REC (INPFILE, LRECL 53); defined in the program, not a copybook. */
    public static final RecordLayout TRANTYPE_UPDATE = new RecordLayout("COBTUPDT", List.of(
            text("INPUT-REC-TYPE", 0, 1), text("INPUT-REC-NUMBER", 1, 2), text("INPUT-REC-DESC", 3, 50)));

    /** CBTRNDYN FD DYNSQL-FILE DYNSQL-REC PIC X(80) (DYNSQL, LRECL 80); defined in the program. */
    public static final RecordLayout DYNSQL = new RecordLayout("CBTRNDYN", List.of(text("DYNSQL-REC", 0, 80)));

    /** CVTRA06Y DALYTRAN-RECORD, RECLN 350. */
    public static final RecordLayout DALYTRAN = new RecordLayout("CVTRA06Y", transaction("DALYTRAN"));

    /** CVTRA05Y TRAN-RECORD, RECLN 350. */
    public static final RecordLayout TRAN = new RecordLayout("CVTRA05Y", transaction("TRAN"));

    /** REJTRAN REJECT-RECORD: the 350-byte daily transaction plus the 80-byte validation trailer. */
    public static final RecordLayout REJTRAN = new RecordLayout("REJTRAN", rejectRecord());

    public static final RecordLayout TRAN_TYPE = new RecordLayout("CVTRA03Y", List.of(
            text("TRAN-TYPE", 0, 2), text("TRAN-TYPE-DESC", 2, 50), filler(52, 8)));

    public static final RecordLayout TRAN_CATEGORY = new RecordLayout("CVTRA04Y", List.of(
            text("TRAN-TYPE-CD", 0, 2), zoned("TRAN-CAT-CD", 2, 4, 0, false),
            text("TRAN-CAT-TYPE-DESC", 6, 50), filler(56, 4)));

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
        fields.add(zoned("REJ-FAIL-REASON", 350, 4, 0, false));
        fields.add(text("REJ-FAIL-REASON-DESC", 354, 76));
        return fields;
    }
}
