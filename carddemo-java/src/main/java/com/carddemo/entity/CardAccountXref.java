package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

/**
 * Card-Account Cross-reference entity - migrated from VSAM file XREFFILE / copybook CVACT03Y.
 * Original record length: 50 bytes.
 */
@Entity
@Table(name = "card_account_xref")
@IdClass(CardAccountXrefId.class)
public class CardAccountXref {

    @Id
    @Column(name = "card_num", length = 16, nullable = false)
    @Size(max = 16)
    private String cardNum;

    @Id
    @Column(name = "cust_id", nullable = false)
    private Long custId;

    @Id
    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    public CardAccountXref() {
    }

    public CardAccountXref(String cardNum, Long custId, Long acctId) {
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
}
