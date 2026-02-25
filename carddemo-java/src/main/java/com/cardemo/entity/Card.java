package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Migrated from VSAM CARDFILE / Copybook CVACT02Y (150-byte FB records).
 * COBOL: CARD-RECORD
 */
@Entity
@Table(name = "cards")
public class Card {

    /** CARD-NUM PIC X(16) */
    @Id
    @Column(name = "card_num", length = 16, nullable = false)
    private String cardNum;

    /** CARD-ACCT-ID PIC 9(11) */
    @Column(name = "card_acct_id")
    private Long cardAcctId;

    /** CARD-CVV-CD PIC 9(03) */
    @Column(name = "card_cvv_cd")
    private Integer cardCvvCd;

    /** CARD-EMBOSSED-NAME PIC X(50) */
    @Column(name = "card_embossed_name", length = 50)
    private String cardEmbossedName;

    /** CARD-EXPIRAION-DATE PIC X(10) */
    @Column(name = "card_expiration_date", length = 10)
    private String cardExpirationDate;

    /** CARD-ACTIVE-STATUS PIC X(01) */
    @Column(name = "card_active_status", length = 1)
    private String cardActiveStatus;

    public Card() {
    }

    public String getCardNum() {
        return cardNum;
    }

    public void setCardNum(String cardNum) {
        this.cardNum = cardNum;
    }

    public Long getCardAcctId() {
        return cardAcctId;
    }

    public void setCardAcctId(Long cardAcctId) {
        this.cardAcctId = cardAcctId;
    }

    public Integer getCardCvvCd() {
        return cardCvvCd;
    }

    public void setCardCvvCd(Integer cardCvvCd) {
        this.cardCvvCd = cardCvvCd;
    }

    public String getCardEmbossedName() {
        return cardEmbossedName;
    }

    public void setCardEmbossedName(String cardEmbossedName) {
        this.cardEmbossedName = cardEmbossedName;
    }

    public String getCardExpirationDate() {
        return cardExpirationDate;
    }

    public void setCardExpirationDate(String cardExpirationDate) {
        this.cardExpirationDate = cardExpirationDate;
    }

    public String getCardActiveStatus() {
        return cardActiveStatus;
    }

    public void setCardActiveStatus(String cardActiveStatus) {
        this.cardActiveStatus = cardActiveStatus;
    }
}
