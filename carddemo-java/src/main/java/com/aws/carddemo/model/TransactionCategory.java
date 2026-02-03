package com.aws.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Transaction Category entity - migrated from COBOL copybook CVTRA04Y.cpy
 * Original COBOL record length: 60 bytes
 * Reference data for transaction category types
 */
@Entity
@Table(name = "TRANCATG")
public class TransactionCategory {

    @EmbeddedId
    private TransactionCategoryKey id;

    @Column(name = "TRAN_CAT_TYPE_DESC", length = 50)
    private String tranCatTypeDesc;

    public TransactionCategory() {
    }

    public TransactionCategory(TransactionCategoryKey id) {
        this.id = id;
    }

    public TransactionCategoryKey getId() {
        return id;
    }

    public void setId(TransactionCategoryKey id) {
        this.id = id;
    }

    public String getTranCatTypeDesc() {
        return tranCatTypeDesc;
    }

    public void setTranCatTypeDesc(String tranCatTypeDesc) {
        this.tranCatTypeDesc = tranCatTypeDesc;
    }

    @Override
    public String toString() {
        return "TransactionCategory{" +
                "id=" + id +
                ", tranCatTypeDesc='" + tranCatTypeDesc + '\'' +
                '}';
    }

    @Embeddable
    public static class TransactionCategoryKey implements Serializable {

        @Column(name = "TRAN_TYPE_CD", length = 2)
        private String tranTypeCd;

        @Column(name = "TRAN_CAT_CD")
        private Integer tranCatCd;

        public TransactionCategoryKey() {
        }

        public TransactionCategoryKey(String tranTypeCd, Integer tranCatCd) {
            this.tranTypeCd = tranTypeCd;
            this.tranCatCd = tranCatCd;
        }

        public String getTranTypeCd() {
            return tranTypeCd;
        }

        public void setTranTypeCd(String tranTypeCd) {
            this.tranTypeCd = tranTypeCd;
        }

        public Integer getTranCatCd() {
            return tranCatCd;
        }

        public void setTranCatCd(Integer tranCatCd) {
            this.tranCatCd = tranCatCd;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransactionCategoryKey that = (TransactionCategoryKey) o;
            return Objects.equals(tranTypeCd, that.tranTypeCd) && Objects.equals(tranCatCd, that.tranCatCd);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tranTypeCd, tranCatCd);
        }

        @Override
        public String toString() {
            return "TransactionCategoryKey{" +
                    "tranTypeCd='" + tranTypeCd + '\'' +
                    ", tranCatCd=" + tranCatCd +
                    '}';
        }
    }
}
