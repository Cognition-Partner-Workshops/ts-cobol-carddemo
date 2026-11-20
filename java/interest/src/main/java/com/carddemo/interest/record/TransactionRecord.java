package com.carddemo.interest.record;

import static com.carddemo.interest.record.Fixed.pad;
import static com.carddemo.interest.record.Fixed.unsigned;
import static com.carddemo.interest.record.Fixed.zoned;

import java.math.BigDecimal;

/** CVTRA05Y, RECLN 350 (trailing FILLER X(20) is spaces). */
public record TransactionRecord(String tranId, String tranTypeCd, int tranCatCd, String tranSource, String tranDesc,
                                BigDecimal tranAmt, long merchantId, String merchantName, String merchantCity,
                                String merchantZip, String cardNum, String origTs, String procTs) {

    public static final int LENGTH = 350;

    public String format() {
        return pad(pad(tranId, 16) + pad(tranTypeCd, 2) + unsigned(tranCatCd, 4) + pad(tranSource, 10)
                + pad(tranDesc, 100) + zoned(tranAmt, 11, 2) + unsigned(merchantId, 9) + pad(merchantName, 50)
                + pad(merchantCity, 50) + pad(merchantZip, 10) + pad(cardNum, 16) + pad(origTs, 26)
                + pad(procTs, 26), LENGTH);
    }
}
