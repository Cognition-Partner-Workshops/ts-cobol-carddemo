package com.aws.carddemo.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "transaction_category")
public class TransactionCategory {

    @Id
    @Column(name = "cat_cd", length = 4)
    private String catCd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cat_type_cd", referencedColumnName = "type_cd", nullable = false)
    private TransactionType transactionType;

    @Column(name = "cat_desc", nullable = false, length = 50)
    private String catDesc;

    public String getCatCd() {
        return catCd;
    }

    public void setCatCd(String catCd) {
        this.catCd = catCd;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getCatDesc() {
        return catDesc;
    }

    public void setCatDesc(String catDesc) {
        this.catDesc = catDesc;
    }
}
