package org.carddemo.modern.codec;

import static org.carddemo.modern.codec.Field.*;
import java.util.List;

/** Remaining posting layouts, audited against CVTRA03Y/04Y and CBTRN03C's FDs. */
public final class PostingLayouts {
    private PostingLayouts() { }
    public static final RecordLayout TYPE = new RecordLayout("CVTRA03Y", List.of(
            text("TRAN-TYPE", 0, 2), text("TRAN-TYPE-DESC", 2, 50), filler(52, 8)));
    public static final RecordLayout CATEGORY = new RecordLayout("CVTRA04Y", List.of(
            text("TRAN-TYPE-CD", 0, 2), zoned("TRAN-CAT-CD", 2, 4, 0, false),
            text("TRAN-CAT-TYPE-DESC", 6, 50), filler(56, 4)));
    public static final RecordLayout DATE = new RecordLayout("CBTRN03C WS-DATEPARM-RECORD", List.of(
            text("WS-START-DATE", 0, 10), filler(10, 1), text("WS-END-DATE", 11, 10), filler(21, 59)));
    // FD-REPTFILE-REC is PIC X(133); CVTRA07Y's edited numeric fields are formatted before this MOVE.
    public static final RecordLayout REPORT = new RecordLayout("CBTRN03C FD-REPTFILE-REC", List.of(text("FD-REPTFILE-REC", 0, 133)));
    public static final RecordLayout DETAIL = new RecordLayout("CVTRA07Y TRANSACTION-DETAIL-REPORT", List.of(
            text("TRAN-REPORT-TRANS-ID", 0, 16), filler(16, 1), text("TRAN-REPORT-ACCOUNT-ID", 17, 11),
            filler(28, 1), text("TRAN-REPORT-TYPE-CD", 29, 2), text("TYPE-SEPARATOR", 31, 1),
            text("TRAN-REPORT-TYPE-DESC", 32, 15), filler(47, 1), zoned("TRAN-REPORT-CAT-CD", 48, 4, 0, false),
            text("CATEGORY-SEPARATOR", 52, 1), text("TRAN-REPORT-CAT-DESC", 53, 29), filler(82, 1),
            text("TRAN-REPORT-SOURCE", 83, 10), filler(93, 4), text("TRAN-REPORT-AMT", 97, 14), filler(111, 2)));
}
