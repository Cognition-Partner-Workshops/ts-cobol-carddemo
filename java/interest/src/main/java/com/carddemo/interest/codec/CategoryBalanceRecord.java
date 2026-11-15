package com.carddemo.interest.codec;

import com.carddemo.interest.domain.CategoryBalance;

/** Layout CVTRA01Y (app/cpy/CVTRA01Y.cpy:4-10, RECLN 50). Data-map rows CVTRA01Y/*. */
public final class CategoryBalanceRecord {

    public static final int LENGTH = 50;

    private static final Field ACCOUNT_ID = new Field("TRANCAT-ACCT-ID", 0, 11);
    private static final Field TYPE_CODE = new Field("TRANCAT-TYPE-CD", 11, 2);
    private static final Field CATEGORY_CODE = new Field("TRANCAT-CD", 13, 4);
    private static final Field BALANCE = new Field("TRAN-CAT-BAL", 17, 11);
    /** TRAN-CAT-KEY: the three key fields, 17 bytes. */
    private static final int KEY_LENGTH = 17;

    private CategoryBalanceRecord() {
    }

    public static CategoryBalance decode(String image) {
        return new CategoryBalance(
                ACCOUNT_ID.unsigned(image),
                TYPE_CODE.text(image),
                (int) CATEGORY_CODE.unsigned(image),
                BALANCE.signed(image, 2));
    }

    public static String key(String image) {
        return image.substring(0, KEY_LENGTH);
    }
}
