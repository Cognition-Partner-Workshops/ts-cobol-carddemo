package com.carddemo.creditcard;

/**
 * Java equivalent of the COBOL CARD-RECORD copybook (CVACT02Y.cpy).
 *
 * <pre>
 * 01  CARD-RECORD.
 *     05  CARD-NUM               PIC X(16).
 *     05  CARD-ACCT-ID           PIC 9(11).
 *     05  CARD-CVV-CD            PIC 9(03).
 *     05  CARD-EMBOSSED-NAME     PIC X(50).
 *     05  CARD-EXPIRAION-DATE    PIC X(10).   (YYYY-MM-DD format)
 *     05  CARD-ACTIVE-STATUS     PIC X(01).   ('Y' or 'N')
 * </pre>
 */
public class CardRecord {

    private String cardNumber;
    private String accountId;
    private String cvvCode;
    private String embossedName;
    private String expirationDate;
    private String activeStatus;

    public CardRecord() {
    }

    public CardRecord(String cardNumber, String accountId, String cvvCode,
                      String embossedName, String expirationDate, String activeStatus) {
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        this.cvvCode = cvvCode;
        this.embossedName = embossedName;
        this.expirationDate = expirationDate;
        this.activeStatus = activeStatus;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCvvCode() {
        return cvvCode;
    }

    public void setCvvCode(String cvvCode) {
        this.cvvCode = cvvCode;
    }

    public String getEmbossedName() {
        return embossedName;
    }

    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Extracts the expiry year (first 4 characters of YYYY-MM-DD format).
     * COBOL reference: CARD-EXPIRAION-DATE(1:4)
     */
    public String getExpiryYear() {
        if (expirationDate != null && expirationDate.length() >= 4) {
            return expirationDate.substring(0, 4);
        }
        return null;
    }

    /**
     * Extracts the expiry month (characters 6-7 of YYYY-MM-DD format).
     * COBOL reference: CARD-EXPIRAION-DATE(6:2)
     */
    public String getExpiryMonth() {
        if (expirationDate != null && expirationDate.length() >= 7) {
            return expirationDate.substring(5, 7);
        }
        return null;
    }

    /**
     * Extracts the expiry day (characters 9-10 of YYYY-MM-DD format).
     * COBOL reference: CARD-EXPIRAION-DATE(9:2)
     */
    public String getExpiryDay() {
        if (expirationDate != null && expirationDate.length() >= 10) {
            return expirationDate.substring(8, 10);
        }
        return null;
    }
}
