package com.carddemo.interest.codec;

import java.util.Arrays;
import java.util.Comparator;

import com.carddemo.interest.domain.CategoryBalance;

/** Layout CVTRA01Y (app/cpy/CVTRA01Y.cpy:4-10, RECLN 50). Data-map rows CVTRA01Y/*. */
public final class CategoryBalanceRecord {

    private static final RecordLayout.Builder COPYBOOK = RecordLayout.builder("CVTRA01Y");
    private static final FieldSpec ACCOUNT_ID = COPYBOOK.field("TRANCAT-ACCT-ID", "9(11)");
    private static final FieldSpec TYPE_CODE = COPYBOOK.field("TRANCAT-TYPE-CD", "X(02)");
    private static final FieldSpec CATEGORY_CODE = COPYBOOK.field("TRANCAT-CD", "9(04)");
    private static final FieldSpec BALANCE = COPYBOOK.field("TRAN-CAT-BAL", "S9(09)V99");
    private static final FieldSpec FILLER = COPYBOOK.filler("X(22)");
    public static final RecordLayout LAYOUT = COPYBOOK.build();
    /** TRAN-CAT-KEY: the three key fields, 17 bytes. */
    private static final int KEY_LENGTH = BALANCE.offset();

    /** The byte order of TRAN-CAT-KEY, as VSAM returns the file (rule E6). */
    public static final Comparator<byte[]> KEY_ORDER = (a, b) ->
            Arrays.compareUnsigned(a, 0, Math.min(a.length, KEY_LENGTH), b, 0, Math.min(b.length, KEY_LENGTH));

    private final RecordCodec codec;

    public CategoryBalanceRecord(CodePage codePage) {
        this.codec = new RecordCodec(LAYOUT, codePage);
    }

    public RecordCodec codec() {
        return codec;
    }

    public CategoryBalance decode(byte[] record, RecordSource source) {
        RecordValues values = codec.decode(record, source);
        return new CategoryBalance(
                values.unsigned(ACCOUNT_ID),
                values.text(TYPE_CODE),
                (int) values.unsigned(CATEGORY_CODE),
                values.signed(BALANCE));
    }
}
