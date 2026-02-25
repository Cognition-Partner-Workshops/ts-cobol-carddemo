package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Migrated from VSAM XREFFILE / Copybook CVACT03Y (50-byte FB records).
 * COBOL: CARD-XREF-RECORD
 */
@Entity
@Table(name = "card_account_xref", indexes = {
    @Index(name = "idx_xref_acct_id", columnList = "xref_acct_id")
})
public class CardAccountXref {

    /** XREF-CARD-NUM PIC X(16) */
    @Id
    @Column(name = "xref_card_num", length = 16, nullable = false)
    private String xrefCardNum;

    /** XREF-CUST-ID PIC 9(09) */
    @Column(name = "xref_cust_id")
    private Long xrefCustId;

    /** XREF-ACCT-ID PIC 9(11) */
    @Column(name = "xref_acct_id")
    private Long xrefAcctId;

    public CardAccountXref() {
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
}
