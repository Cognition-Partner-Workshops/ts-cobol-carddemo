package com.carddemo.posting.record;

import static com.carddemo.posting.record.Text.digits;
import static com.carddemo.posting.record.Text.field;
import static com.carddemo.posting.record.Text.pad;
import static com.carddemo.posting.record.Text.slice;
import static com.carddemo.posting.record.Text.unsigned;

import java.math.BigDecimal;

/**
 * Record layouts of the posting chain, one record per copybook. Offsets are copied from
 * app/cpy; this slice deliberately keeps its own copy instead of sharing a library with
 * other migrated chains.
 */
public final class Records {

    private Records() {
    }

    /** CVTRA06Y (DALYTRAN, 350) and CVTRA05Y (TRANSACT, 350) share one layout. */
    public record Transaction(String tranId, String typeCd, int catCd, String source, String description,
                              BigDecimal amount, String merchantId, String merchantName, String merchantCity,
                              String merchantZip, String cardNum, String origTs, String procTs) {

        public static final int LRECL = 350;

        public static Transaction parse(String record) {
            return new Transaction(field(record, 0, 16), field(record, 16, 2), (int) unsigned(record, 18, 4),
                    field(record, 22, 10), field(record, 32, 100), Zoned.decode(slice(record, 132, 11), 2),
                    field(record, 143, 9), field(record, 152, 50), field(record, 202, 50),
                    field(record, 252, 10), field(record, 262, 16), field(record, 278, 26),
                    field(record, 304, 26));
        }

        public String format() {
            return pad(tranId, 16) + pad(typeCd, 2) + digits(catCd, 4) + pad(source, 10) + pad(description, 100)
                    + Zoned.encode(amount, 9, 2, true) + pad(merchantId, 9) + pad(merchantName, 50)
                    + pad(merchantCity, 50) + pad(merchantZip, 10) + pad(cardNum, 16) + pad(origTs, 26)
                    + pad(procTs, 26) + pad("", 20);
        }

        public Transaction withProcTs(String timestamp) {
            return new Transaction(tranId, typeCd, catCd, source, description, amount, merchantId, merchantName,
                    merchantCity, merchantZip, cardNum, origTs, timestamp);
        }
    }

    /** CVACT01Y (ACCTDATA, 300). */
    public record Account(long acctId, String activeStatus, BigDecimal currBal, BigDecimal creditLimit,
                          BigDecimal cashCreditLimit, String openDate, String expirationDate, String reissueDate,
                          BigDecimal currCycCredit, BigDecimal currCycDebit, String addrZip, String groupId,
                          String filler) {

        public static final int LRECL = 300;

        public static Account parse(String record) {
            return new Account(unsigned(record, 0, 11), field(record, 11, 1), Zoned.decode(slice(record, 12, 12), 2),
                    Zoned.decode(slice(record, 24, 12), 2), Zoned.decode(slice(record, 36, 12), 2),
                    field(record, 48, 10), field(record, 58, 10), field(record, 68, 10),
                    Zoned.decode(slice(record, 78, 12), 2), Zoned.decode(slice(record, 90, 12), 2),
                    field(record, 102, 10), field(record, 112, 10), slice(record, 122, 178));
        }

        public String format() {
            return digits(acctId, 11) + pad(activeStatus, 1) + Zoned.encode(currBal, 10, 2, true)
                    + Zoned.encode(creditLimit, 10, 2, true) + Zoned.encode(cashCreditLimit, 10, 2, true)
                    + pad(openDate, 10) + pad(expirationDate, 10) + pad(reissueDate, 10)
                    + Zoned.encode(currCycCredit, 10, 2, true) + Zoned.encode(currCycDebit, 10, 2, true)
                    + pad(addrZip, 10) + pad(groupId, 10) + pad(filler, 178);
        }
    }

    /** CVACT03Y (CARDXREF, 50). */
    public record CardXref(String cardNum, long custId, long acctId) {

        public static CardXref parse(String record) {
            return new CardXref(field(record, 0, 16), unsigned(record, 16, 9), unsigned(record, 25, 11));
        }
    }

    /** CVTRA01Y (TCATBALF, 50). */
    public record TranCatBalance(long acctId, String typeCd, int catCd, BigDecimal balance, String filler) {

        public static final int LRECL = 50;

        public static TranCatBalance parse(String record) {
            return new TranCatBalance(unsigned(record, 0, 11), field(record, 11, 2), (int) unsigned(record, 13, 4),
                    Zoned.decode(slice(record, 17, 11), 2), slice(record, 28, 22));
        }

        /** TRAN-CAT-KEY as DISPLAYed by CBTRN02C. */
        public static String key(long acctId, String typeCd, int catCd) {
            return digits(acctId, 11) + pad(typeCd, 2) + digits(catCd, 4);
        }

        public String format() {
            return key(acctId, typeCd, catCd) + Zoned.encode(balance, 9, 2, true) + pad(filler, 22);
        }
    }

    /** CVTRA03Y (TRANTYPE, 60). */
    public record TranType(String typeCd, String description) {

        public static TranType parse(String record) {
            return new TranType(field(record, 0, 2), field(record, 2, 50));
        }
    }

    /** CVTRA04Y (TRANCATG, 60). */
    public record TranCategory(String typeCd, int catCd, String description) {

        public static TranCategory parse(String record) {
            return new TranCategory(field(record, 0, 2), (int) unsigned(record, 2, 4), field(record, 6, 50));
        }
    }

    /** tools/parity/layouts/REJTRAN.cpy: the daily record as read plus the validation trailer. */
    public static String reject(String dailyRecord, int reason, String reasonDescription) {
        return pad(dailyRecord, Transaction.LRECL) + digits(reason, 4) + pad(reasonDescription, 76);
    }
}
