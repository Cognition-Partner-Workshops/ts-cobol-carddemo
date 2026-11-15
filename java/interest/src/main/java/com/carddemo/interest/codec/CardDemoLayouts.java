package com.carddemo.interest.codec;

/**
 * The five record layouts of the interest stream (FI-1), read by {@link Copybook} from the data
 * description lines of the copybooks under app/cpy. The lines are carried here because the job
 * runs from its jar, without the estate's source tree; CardDemoLayoutsTest reads app/cpy and fails
 * when a copybook and its copy here describe different fields.
 */
public final class CardDemoLayouts {

    /** app/cpy/CVACT01Y.cpy:4-17, RECLN 300. */
    public static final RecordLayout ACCOUNT = layout("CVACT01Y", 300, """
                   01  ACCOUNT-RECORD.
                       05  ACCT-ID                           PIC 9(11).
                       05  ACCT-ACTIVE-STATUS                PIC X(01).
                       05  ACCT-CURR-BAL                     PIC S9(10)V99.
                       05  ACCT-CREDIT-LIMIT                 PIC S9(10)V99.
                       05  ACCT-CASH-CREDIT-LIMIT            PIC S9(10)V99.
                       05  ACCT-OPEN-DATE                    PIC X(10).
                       05  ACCT-EXPIRAION-DATE               PIC X(10).
                       05  ACCT-REISSUE-DATE                 PIC X(10).
                       05  ACCT-CURR-CYC-CREDIT              PIC S9(10)V99.
                       05  ACCT-CURR-CYC-DEBIT               PIC S9(10)V99.
                       05  ACCT-ADDR-ZIP                     PIC X(10).
                       05  ACCT-GROUP-ID                     PIC X(10).
                       05  FILLER                            PIC X(178).
            """);

    /** app/cpy/CVACT03Y.cpy:4-8, RECLN 50. */
    public static final RecordLayout CARD_XREF = layout("CVACT03Y", 50, """
                   01 CARD-XREF-RECORD.
                       05  XREF-CARD-NUM                     PIC X(16).
                       05  XREF-CUST-ID                      PIC 9(09).
                       05  XREF-ACCT-ID                      PIC 9(11).
                       05  FILLER                            PIC X(14).
            """);

    /** app/cpy/CVTRA01Y.cpy:4-10, RECLN 50. */
    public static final RecordLayout CATEGORY_BALANCE = layout("CVTRA01Y", 50, """
                   01  TRAN-CAT-BAL-RECORD.
                       05  TRAN-CAT-KEY.
                          10 TRANCAT-ACCT-ID                       PIC 9(11).
                          10 TRANCAT-TYPE-CD                       PIC X(02).
                          10 TRANCAT-CD                            PIC 9(04).
                       05  TRAN-CAT-BAL                            PIC S9(09)V99.
                       05  FILLER                                  PIC X(22).
            """);

    /** app/cpy/CVTRA02Y.cpy:4-10, RECLN 50. */
    public static final RecordLayout DISCLOSURE_GROUP = layout("CVTRA02Y", 50, """
                   01  DIS-GROUP-RECORD.
                       05  DIS-GROUP-KEY.
                          10 DIS-ACCT-GROUP-ID                     PIC X(10).
                          10 DIS-TRAN-TYPE-CD                      PIC X(02).
                          10 DIS-TRAN-CAT-CD                       PIC 9(04).
                       05  DIS-INT-RATE                            PIC S9(04)V99.
                       05  FILLER                                  PIC X(28).
            """);

    /** app/cpy/CVTRA05Y.cpy:4-18, RECLN 350. */
    public static final RecordLayout TRANSACTION = layout("CVTRA05Y", 350, """
                   01  TRAN-RECORD.
                       05  TRAN-ID                                 PIC X(16).
                       05  TRAN-TYPE-CD                            PIC X(02).
                       05  TRAN-CAT-CD                             PIC 9(04).
                       05  TRAN-SOURCE                             PIC X(10).
                       05  TRAN-DESC                               PIC X(100).
                       05  TRAN-AMT                                PIC S9(09)V99.
                       05  TRAN-MERCHANT-ID                        PIC 9(09).
                       05  TRAN-MERCHANT-NAME                      PIC X(50).
                       05  TRAN-MERCHANT-CITY                      PIC X(50).
                       05  TRAN-MERCHANT-ZIP                       PIC X(10).
                       05  TRAN-CARD-NUM                           PIC X(16).
                       05  TRAN-ORIG-TS                            PIC X(26).
                       05  TRAN-PROC-TS                            PIC X(26).
                       05  FILLER                                  PIC X(20).
            """);

    private CardDemoLayouts() {
    }

    private static RecordLayout layout(String copybook, int recordLength, String dataDescription) {
        RecordLayout layout = Copybook.parse(copybook, dataDescription);
        for (LayoutField field : layout.fields()) {
            if (field.kind() == FieldKind.REFUSED) {
                throw new IllegalStateException("copybook " + copybook + ", field " + field.name() + ": " + field.refusal());
            }
        }
        if (layout.length() != recordLength) {
            throw new IllegalStateException("copybook " + copybook + " describes " + layout.length() + " bytes, the file has " + recordLength);
        }
        return layout;
    }
}
