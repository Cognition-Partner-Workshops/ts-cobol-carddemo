package com.aws.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Card Cross-Reference entity - migrated from COBOL copybook CVACT03Y.cpy
 * Original COBOL record length: 50 bytes
 * Links card numbers to customer and account IDs
 */
@Entity
@Table(name = "CARDXREF")
public class CardXref {

    @Id
    @Column(name = "XREF_CARD_NUM", length = 16)
    private String xrefCardNum;

    @Column(name = "XREF_CUST_ID")
    private Long xrefCustId;

    @Column(name = "XREF_ACCT_ID")
    private Long xrefAcctId;

    public CardXref() {
    }

    public CardXref(String xrefCardNum) {
        this.xrefCardNum = xrefCardNum;
    }

    public String getXrefCardNum() {
        return xrefCardNum;
    }

    public void setXrefCardNum(String xrefCardNum) {
        this.xrefCardNum = xrefCardNum;
    }

    public Long getXrefCustId() {
        return xrefCustId;
    }

    public void setXrefCustId(Long xrefCustId) {
        this.xrefCustId = xrefCustId;
    }

    public Long getXrefAcctId() {
        return xrefAcctId;
    }

    public void setXrefAcctId(Long xrefAcctId) {
        this.xrefAcctId = xrefAcctId;
    }

    @Override
    public String toString() {
        return "CardXref{" +
                "xrefCardNum='" + xrefCardNum + '\'' +
                ", xrefCustId=" + xrefCustId +
                ", xrefAcctId=" + xrefAcctId +
                '}';
    }
}
