package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Migrated from TRANTYPE.PS / Copybook CVTRA03Y (60-byte FB records).
 * COBOL: TRAN-TYPE-RECORD
 */
@Entity
@Table(name = "transaction_types")
public class TransactionType {

    /** TRAN-TYPE PIC X(02) */
    @Id
    @Column(name = "tran_type", length = 2, nullable = false)
    private String tranType;

    /** TRAN-TYPE-DESC PIC X(50) */
    @Column(name = "tran_type_desc", length = 50)
    private String tranTypeDesc;

    public TransactionType() {
    }

    public String getTranType() {
        return tranType;
    }

    public void setTranType(String tranType) {
        this.tranType = tranType;
    }

    public String getTranTypeDesc() {
        return tranTypeDesc;
    }

    public void setTranTypeDesc(String tranTypeDesc) {
        this.tranTypeDesc = tranTypeDesc;
    }
}
