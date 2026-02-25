package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Migrated from TRANCATG.PS / Copybook CVTRA04Y (60-byte FB records).
 * COBOL: TRAN-CAT-RECORD
 */
@Entity
@Table(name = "transaction_categories")
@IdClass(TransactionCategory.TransactionCategoryId.class)
public class TransactionCategory {

    /** TRAN-TYPE-CD PIC X(02) */
    @Id
    @Column(name = "tran_type_cd", length = 2, nullable = false)
    private String tranTypeCd;

    /** TRAN-CAT-CD PIC 9(04) */
    @Id
    @Column(name = "tran_cat_cd", nullable = false)
    private Integer tranCatCd;

    /** TRAN-CAT-TYPE-DESC PIC X(50) */
    @Column(name = "tran_cat_type_desc", length = 50)
    private String tranCatTypeDesc;

    public TransactionCategory() {
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

    public String getTranCatTypeDesc() {
        return tranCatTypeDesc;
    }

    public void setTranCatTypeDesc(String tranCatTypeDesc) {
        this.tranCatTypeDesc = tranCatTypeDesc;
    }

    public static class TransactionCategoryId implements Serializable {
        private String tranTypeCd;
        private Integer tranCatCd;

        public TransactionCategoryId() {
        }

        public TransactionCategoryId(String tranTypeCd, Integer tranCatCd) {
            this.tranTypeCd = tranTypeCd;
            this.tranCatCd = tranCatCd;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransactionCategoryId that = (TransactionCategoryId) o;
            return Objects.equals(tranTypeCd, that.tranTypeCd) &&
                   Objects.equals(tranCatCd, that.tranCatCd);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tranTypeCd, tranCatCd);
        }
    }
}
