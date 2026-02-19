package com.carddemo.transactiontype.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "transaction_type_category")
@IdClass(TransactionTypeCategoryId.class)
public class TransactionTypeCategory {

    @Id
    @Column(name = "trc_type_code", length = 2, nullable = false)
    @NotBlank(message = "Type code is required")
    @Size(min = 1, max = 2, message = "Type code must be 1-2 characters")
    private String trcTypeCode;

    @Id
    @Column(name = "trc_type_category", length = 4, nullable = false)
    @NotBlank(message = "Type category is required")
    @Size(min = 1, max = 4, message = "Type category must be 1-4 characters")
    private String trcTypeCategory;

    @Column(name = "trc_cat_data", length = 50)
    @Size(max = 50, message = "Category data must not exceed 50 characters")
    private String trcCatData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trc_type_code", referencedColumnName = "tr_type",
            insertable = false, updatable = false)
    private TransactionType transactionType;

    public TransactionTypeCategory() {
    }

    public TransactionTypeCategory(String trcTypeCode, String trcTypeCategory, String trcCatData) {
        this.trcTypeCode = trcTypeCode;
        this.trcTypeCategory = trcTypeCategory;
        this.trcCatData = trcCatData;
    }

    public String getTrcTypeCode() {
        return trcTypeCode;
    }

    public void setTrcTypeCode(String trcTypeCode) {
        this.trcTypeCode = trcTypeCode;
    }

    public String getTrcTypeCategory() {
        return trcTypeCategory;
    }

    public void setTrcTypeCategory(String trcTypeCategory) {
        this.trcTypeCategory = trcTypeCategory;
    }

    public String getTrcCatData() {
        return trcCatData;
    }

    public void setTrcCatData(String trcCatData) {
        this.trcCatData = trcCatData;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
}
