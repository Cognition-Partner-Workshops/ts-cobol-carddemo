package com.carddemo.batch.domain;

import java.math.BigDecimal;

/**
 * Promotional purchase rate defined by CBACT04C R19 for change request #144.
 */
public final class PurchasePromotion {

    public static final String GROUP = "GOLDPROMO1";
    public static final String TYPE_CODE = "01";
    public static final int CATEGORY_CODE = 1;
    public static final BigDecimal ANNUAL_RATE = new BigDecimal("4.99");
    public static final int CYCLES = 3;

    private PurchasePromotion() {
    }

    public static boolean isMember(String groupId) {
        return groupId != null && GROUP.equals(groupId.stripTrailing());
    }

    public static boolean covers(String groupId, CategoryBalance cat) {
        return isMember(groupId) && TYPE_CODE.equals(cat.typeCode()) && CATEGORY_CODE == cat.categoryCode();
    }

    public static BigDecimal rate(String groupId, CategoryBalance cat, int cycle, BigDecimal ordinaryRate) {
        return covers(groupId, cat) && cycle >= 1 && cycle <= CYCLES ? ANNUAL_RATE : ordinaryRate;
    }
}
