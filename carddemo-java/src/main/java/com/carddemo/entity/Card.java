package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    @Column(name = "card_num", length = 16)
    private String cardNum;

    @Column(name = "acct_id")
    private Long acctId;

    @Column(name = "cvv_cd")
    private Integer cvvCd;

    @Column(name = "embossed_name", length = 50)
    private String embossedName;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "active_status", length = 1)
    private String activeStatus;

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

    public Integer getCvvCd() {
        return cvvCd;
    }

    public void setCvvCd(Integer cvvCd) {
        this.cvvCd = cvvCd;
    }

    public String getEmbossedName() {
        return embossedName;
    }

    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }
}
