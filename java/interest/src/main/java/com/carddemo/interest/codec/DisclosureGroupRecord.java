package com.carddemo.interest.codec;

import java.math.BigDecimal;

/** Layout CVTRA02Y (app/cpy/CVTRA02Y.cpy:4-10, RECLN 50). Data-map rows CVTRA02Y/*. */
public final class DisclosureGroupRecord {

    private static final RecordLayout.Builder COPYBOOK = RecordLayout.builder("CVTRA02Y");
    private static final FieldSpec GROUP_ID = COPYBOOK.field("DIS-ACCT-GROUP-ID", "X(10)");
    private static final FieldSpec TYPE_CODE = COPYBOOK.field("DIS-TRAN-TYPE-CD", "X(02)");
    private static final FieldSpec CATEGORY_CODE = COPYBOOK.field("DIS-TRAN-CAT-CD", "9(04)");
    private static final FieldSpec YEARLY_RATE = COPYBOOK.field("DIS-INT-RATE", "S9(04)V99");
    private static final FieldSpec FILLER = COPYBOOK.filler("X(28)");
    public static final RecordLayout LAYOUT = COPYBOOK.build();

    private final RecordCodec codec;

    public DisclosureGroupRecord(CodePage codePage) {
        this.codec = new RecordCodec(LAYOUT, codePage);
    }

    public RecordCodec codec() {
        return codec;
    }

    /** One rate of a disclosure group: yearly rate in percent, (6,2). */
    public record Rate(String groupId, String typeCode, int categoryCode, BigDecimal yearlyRate) {
    }

    public Rate decode(byte[] record, RecordSource source) {
        RecordValues values = codec.decode(record, source);
        return new Rate(
                values.text(GROUP_ID),
                values.text(TYPE_CODE),
                (int) values.unsigned(CATEGORY_CODE),
                values.signed(YEARLY_RATE));
    }
}
