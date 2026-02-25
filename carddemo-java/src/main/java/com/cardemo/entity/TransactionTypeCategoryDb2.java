package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Migrated from DB2 table CARDDEMO.TRANSACTION_TYPE_CATEGORY.
 * Part of optional module: app-transaction-type-db2
 */
@Entity
@Table(name = "transaction_type_category")
@IdClass(TransactionTypeCategoryDb2.TransactionTypeCategoryId.class)
public class TransactionTypeCategoryDb2 {

    @Id
    @Column(name = "trc_type_code", length = 2, nullable = false)
    private String trcTypeCode;

    @Id
    @Column(name = "trc_type_category", length = 4, nullable = false)
    private String trcTypeCategory;

    @Column(name = "trc_cat_data", length = 50)
    private String trcCatData;

    public TransactionTypeCategoryDb2() {
    }

    public String getTrcTypeCode() { return trcTypeCode; }
    public void setTrcTypeCode(String trcTypeCode) { this.trcTypeCode = trcTypeCode; }
    public String getTrcTypeCategory() { return trcTypeCategory; }
    public void setTrcTypeCategory(String trcTypeCategory) { this.trcTypeCategory = trcTypeCategory; }
    public String getTrcCatData() { return trcCatData; }
    public void setTrcCatData(String trcCatData) { this.trcCatData = trcCatData; }

    public static class TransactionTypeCategoryId implements Serializable {
        private String trcTypeCode;
        private String trcTypeCategory;

        public TransactionTypeCategoryId() {
        }

        public TransactionTypeCategoryId(String trcTypeCode, String trcTypeCategory) {
            this.trcTypeCode = trcTypeCode;
            this.trcTypeCategory = trcTypeCategory;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransactionTypeCategoryId that = (TransactionTypeCategoryId) o;
            return Objects.equals(trcTypeCode, that.trcTypeCode) &&
                   Objects.equals(trcTypeCategory, that.trcTypeCategory);
        }

        @Override
        public int hashCode() {
            return Objects.hash(trcTypeCode, trcTypeCategory);
        }
    }
}
