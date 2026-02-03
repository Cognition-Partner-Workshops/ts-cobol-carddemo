package com.aws.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Transaction Type entity - migrated from COBOL copybook CVTRA03Y.cpy
 * Original COBOL record length: 60 bytes
 * Reference data for transaction types
 */
@Entity
@Table(name = "TRANTYPE")
public class TransactionType {

    @Id
    @Column(name = "TRAN_TYPE", length = 2)
    private String tranType;

    @Column(name = "TRAN_TYPE_DESC", length = 50)
    private String tranTypeDesc;

    public TransactionType() {
    }

    public TransactionType(String tranType) {
        this.tranType = tranType;
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

    @Override
    public String toString() {
        return "TransactionType{" +
                "tranType='" + tranType + '\'' +
                ", tranTypeDesc='" + tranTypeDesc + '\'' +
                '}';
    }
}
