package com.carddemo.modern.interest;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Rows of the INTCALC clusters, mapped to and from their copybook field names. */
final class Records {

    private Records() {
    }

    /** CVTRA01Y TRAN-CAT-BAL-RECORD. */
    record TranCatBal(long acctId, String typeCd, int catCd, BigDecimal balance, String filler) {
        static TranCatBal of(Map<String, Object> r) {
            return new TranCatBal(num(r, "TRANCAT-ACCT-ID").longValueExact(), str(r, "TRANCAT-TYPE-CD"),
                    num(r, "TRANCAT-CD").intValueExact(), num(r, "TRAN-CAT-BAL"), str(r, "FILLER@28"));
        }

        Map<String, Object> fields() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("TRANCAT-ACCT-ID", BigDecimal.valueOf(acctId));
            m.put("TRANCAT-TYPE-CD", typeCd);
            m.put("TRANCAT-CD", BigDecimal.valueOf(catCd));
            m.put("TRAN-CAT-BAL", balance);
            m.put("FILLER@28", filler);
            return m;
        }
    }

    /** CVACT01Y ACCOUNT-RECORD. */
    record Account(long acctId, String activeStatus, BigDecimal currBal, BigDecimal creditLimit,
                   BigDecimal cashCreditLimit, String openDate, String expirationDate, String reissueDate,
                   BigDecimal currCycCredit, BigDecimal currCycDebit, String addrZip, String groupId, String filler) {
        static Account of(Map<String, Object> r) {
            return new Account(num(r, "ACCT-ID").longValueExact(), str(r, "ACCT-ACTIVE-STATUS"),
                    num(r, "ACCT-CURR-BAL"), num(r, "ACCT-CREDIT-LIMIT"), num(r, "ACCT-CASH-CREDIT-LIMIT"),
                    str(r, "ACCT-OPEN-DATE"), str(r, "ACCT-EXPIRAION-DATE"), str(r, "ACCT-REISSUE-DATE"),
                    num(r, "ACCT-CURR-CYC-CREDIT"), num(r, "ACCT-CURR-CYC-DEBIT"), str(r, "ACCT-ADDR-ZIP"),
                    str(r, "ACCT-GROUP-ID"), str(r, "FILLER@122"));
        }

        Map<String, Object> fields() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ACCT-ID", BigDecimal.valueOf(acctId));
            m.put("ACCT-ACTIVE-STATUS", activeStatus);
            m.put("ACCT-CURR-BAL", currBal);
            m.put("ACCT-CREDIT-LIMIT", creditLimit);
            m.put("ACCT-CASH-CREDIT-LIMIT", cashCreditLimit);
            m.put("ACCT-OPEN-DATE", openDate);
            m.put("ACCT-EXPIRAION-DATE", expirationDate);
            m.put("ACCT-REISSUE-DATE", reissueDate);
            m.put("ACCT-CURR-CYC-CREDIT", currCycCredit);
            m.put("ACCT-CURR-CYC-DEBIT", currCycDebit);
            m.put("ACCT-ADDR-ZIP", addrZip);
            m.put("ACCT-GROUP-ID", groupId);
            m.put("FILLER@122", filler);
            return m;
        }
    }

    /** CVACT03Y CARD-XREF-RECORD. */
    record CardXref(String cardNum, long custId, long acctId, String filler) {
        static CardXref of(Map<String, Object> r) {
            return new CardXref(str(r, "XREF-CARD-NUM"), num(r, "XREF-CUST-ID").longValueExact(),
                    num(r, "XREF-ACCT-ID").longValueExact(), str(r, "FILLER@36"));
        }
    }

    /** CVTRA02Y DIS-GROUP-RECORD. */
    record DisclosureGroup(String groupId, String typeCd, int catCd, BigDecimal intRate, String filler) {
        static DisclosureGroup of(Map<String, Object> r) {
            return new DisclosureGroup(str(r, "DIS-ACCT-GROUP-ID"), str(r, "DIS-TRAN-TYPE-CD"),
                    num(r, "DIS-TRAN-CAT-CD").intValueExact(), num(r, "DIS-INT-RATE"), str(r, "FILLER@22"));
        }
    }

    /** CVTRA05Y TRAN-RECORD (FILLER is spaces: CBACT04C builds the record field by field). */
    record Transaction(String tranId, String typeCd, int catCd, String source, String description,
                       BigDecimal amount, long merchantId, String merchantName, String merchantCity,
                       String merchantZip, String cardNum, String origTs, String procTs) {
        Map<String, Object> fields() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("TRAN-ID", tranId);
            m.put("TRAN-TYPE-CD", typeCd);
            m.put("TRAN-CAT-CD", BigDecimal.valueOf(catCd));
            m.put("TRAN-SOURCE", source);
            m.put("TRAN-DESC", description);
            m.put("TRAN-AMT", amount);
            m.put("TRAN-MERCHANT-ID", BigDecimal.valueOf(merchantId));
            m.put("TRAN-MERCHANT-NAME", merchantName);
            m.put("TRAN-MERCHANT-CITY", merchantCity);
            m.put("TRAN-MERCHANT-ZIP", merchantZip);
            m.put("TRAN-CARD-NUM", cardNum);
            m.put("TRAN-ORIG-TS", origTs);
            m.put("TRAN-PROC-TS", procTs);
            return m;
        }
    }

    private static BigDecimal num(Map<String, Object> r, String field) {
        Object v = r.get(field);
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        throw new IllegalArgumentException(field + " is not numeric: '" + v + "'");
    }

    private static String str(Map<String, Object> r, String field) {
        return String.valueOf(r.get(field));
    }
}
