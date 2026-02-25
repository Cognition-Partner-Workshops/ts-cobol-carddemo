package com.carddemo.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for CardAccountXref entity.
 */
public class CardAccountXrefId implements Serializable {

    private String cardNum;
    private Long custId;
    private Long acctId;

    public CardAccountXrefId() {
    }

    public CardAccountXrefId(String cardNum, Long custId, Long acctId) {
        this.cardNum = cardNum;
        this.custId = custId;
        this.acctId = acctId;
    }

    public String getCardNum() {
        return cardNum;
    }

    public void setCardNum(String cardNum) {
        this.cardNum = cardNum;
    }

    public Long getCustId() {
        return custId;
    }

    public void setCustId(Long custId) {
        this.custId = custId;
    }

    public Long getAcctId() {
        return acctId;
    }

    public void setAcctId(Long acctId) {
        this.acctId = acctId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardAccountXrefId that = (CardAccountXrefId) o;
        return Objects.equals(cardNum, that.cardNum)
                && Objects.equals(custId, that.custId)
                && Objects.equals(acctId, that.acctId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardNum, custId, acctId);
    }
}
