package com.carddemo.interest.codec;

import java.math.BigDecimal;

/** Layout CVTRA02Y (app/cpy/CVTRA02Y.cpy:4-10, RECLN 50). Data-map rows CVTRA02Y/*. */
public final class DisclosureGroupRecord {

    public static final int LENGTH = 50;

    private static final Field GROUP_ID = new Field("DIS-ACCT-GROUP-ID", 0, 10);
    private static final Field TYPE_CODE = new Field("DIS-TRAN-TYPE-CD", 10, 2);
    private static final Field CATEGORY_CODE = new Field("DIS-TRAN-CAT-CD", 12, 4);
    private static final Field YEARLY_RATE = new Field("DIS-INT-RATE", 16, 6);

    private DisclosureGroupRecord() {
    }

    /** One rate of a disclosure group: yearly rate in percent, (6,2). */
    public record Rate(String groupId, String typeCode, int categoryCode, BigDecimal yearlyRate) {
    }

    public static Rate decode(String image) {
        return new Rate(
                GROUP_ID.text(image),
                TYPE_CODE.text(image),
                (int) CATEGORY_CODE.unsigned(image),
                YEARLY_RATE.signed(image, 2));
    }
}
