package com.carddemo.interest.record;

import static com.carddemo.interest.record.Fixed.pad;
import static com.carddemo.interest.record.Fixed.text;
import static com.carddemo.interest.record.Fixed.unsigned;
import static com.carddemo.interest.record.Fixed.zoned;

import java.math.BigDecimal;

/** CVACT01Y, RECLN 300. */
public record AccountRecord(long acctId, String activeStatus, BigDecimal currBal, BigDecimal creditLimit,
                            BigDecimal cashCreditLimit, String openDate, String expirationDate, String reissueDate,
                            BigDecimal currCycCredit, BigDecimal currCycDebit, String addrZip, String groupId,
                            String filler) {

    public static final int LENGTH = 300;

    public static AccountRecord parse(String line) {
        String r = pad(line, LENGTH);
        return new AccountRecord(unsigned(r, 0, 11), text(r, 11, 1), zoned(r, 12, 12, 2), zoned(r, 24, 12, 2),
                zoned(r, 36, 12, 2), text(r, 48, 10), text(r, 58, 10), text(r, 68, 10), zoned(r, 78, 12, 2),
                zoned(r, 90, 12, 2), text(r, 102, 10), text(r, 112, 10), text(r, 122, 178));
    }

    public String format() {
        return unsigned(acctId, 11) + pad(activeStatus, 1) + zoned(currBal, 12, 2) + zoned(creditLimit, 12, 2)
                + zoned(cashCreditLimit, 12, 2) + pad(openDate, 10) + pad(expirationDate, 10) + pad(reissueDate, 10)
                + zoned(currCycCredit, 12, 2) + zoned(currCycDebit, 12, 2) + pad(addrZip, 10) + pad(groupId, 10)
                + pad(filler, 178);
    }
}
