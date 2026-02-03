package com.aws.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Card entity - migrated from COBOL copybook CVACT02Y.cpy
 * Original COBOL record length: 150 bytes
 */
@Entity
@Table(name = "CARDDATA")
public class Card {

    @Id
    @Column(name = "CARD_NUM", length = 16)
    private String cardNum;

    @Column(name = "CARD_ACCT_ID")
    private Long cardAcctId;

    @Column(name = "CARD_CVV_CD")
    private Integer cardCvvCd;

    @Column(name = "CARD_EMBOSSED_NAME", length = 50)
    private String cardEmbossedName;

    @Column(name = "CARD_EXPIRAION_DATE", length = 10)
    private String cardExpirationDate;

    @Column(name = "CARD_ACTIVE_STATUS", length = 1)
    private String cardActiveStatus;

    public Card() {
    }

    public Card(String cardNum) {
        this.cardNum = cardNum;
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

    @Override
    public String toString() {
        return "Card{" +
                "cardNum='" + cardNum + '\'' +
                ", cardAcctId=" + cardAcctId +
                ", cardCvvCd=" + cardCvvCd +
                ", cardEmbossedName='" + cardEmbossedName + '\'' +
                ", cardExpirationDate='" + cardExpirationDate + '\'' +
                ", cardActiveStatus='" + cardActiveStatus + '\'' +
                '}';
    }
}
