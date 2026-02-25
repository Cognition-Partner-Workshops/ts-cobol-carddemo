package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

/**
 * Card entity - migrated from VSAM file CARDFILE / copybook CVACT02Y.
 * Original record length: 150 bytes.
 *
 * Fields mapped from COBOL:
 *   CARD-NUM              PIC X(16)  -> cardNum
 *   CARD-ACCT-ID          PIC 9(11)  -> acctId
 *   CARD-CVV-CD           PIC 9(03)  -> cvvCode
 *   CARD-EMBOSSED-NAME    PIC X(50)  -> embossedName
 *   CARD-EXPIRAION-DATE   PIC X(10)  -> expirationDate
 *   CARD-ACTIVE-STATUS    PIC X(01)  -> activeStatus
 */
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @Column(name = "card_num", length = 16, nullable = false)
    @Size(max = 16)
    private String cardNum;

    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Column(name = "cvv_code")
    private Integer cvvCode;

    @Column(name = "embossed_name", length = 50)
    @Size(max = 50)
    private String embossedName;

    @Column(name = "expiration_date", length = 10)
    @Size(max = 10)
    private String expirationDate;

    @Column(name = "active_status", length = 1)
    @Size(max = 1)
    private String activeStatus;

    public Card() {
    }

    public String getCardNum() {
        return cardNum;
    }

    public void setCardNum(String cardNum) {
        this.cardNum = cardNum;
    }

    public Long getAcctId() {
        return acctId;
    }

    public void setAcctId(Long acctId) {
        this.acctId = acctId;
    }

    public Integer getCvvCode() {
        return cvvCode;
    }

    public void setCvvCode(Integer cvvCode) {
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
}
