package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

/**
 * Transaction Category entity - migrated from VSAM file TRANCATG / copybook CVTRA04Y
 * and DB2 table CARDDEMO.TRANSACTION_TYPE_CATEGORY.
 * Original VSAM record length: 60 bytes.
 */
@Entity
@Table(name = "transaction_categories")
@IdClass(TransactionCategoryId.class)
public class TransactionCategory {

    @Id
    @Column(name = "type_cd", length = 2, nullable = false)
    @Size(max = 2)
    private String typeCd;

    @Id
    @Column(name = "cat_cd", nullable = false)
    private Integer catCd;

    @Column(name = "cat_type_desc", length = 50)
    @Size(max = 50)
    private String catTypeDesc;

    public TransactionCategory() {
    }

    public TransactionCategory(String typeCd, Integer catCd, String catTypeDesc) {
        this.typeCd = typeCd;
        this.catCd = catCd;
        this.catTypeDesc = catTypeDesc;
    }

    public String getTypeCd() {
        return typeCd;
    }

    public void setTypeCd(String typeCd) {
        this.typeCd = typeCd;
    }

    public Integer getCatCd() {
        return catCd;
    }

    public void setCatCd(Integer catCd) {
        this.catCd = catCd;
    }

    public String getCatTypeDesc() {
        return catTypeDesc;
    }

    public void setCatTypeDesc(String catTypeDesc) {
        this.catTypeDesc = catTypeDesc;
    }
}
