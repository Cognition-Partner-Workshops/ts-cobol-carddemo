package com.aws.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Transaction Category Balance entity - migrated from COBOL copybook CVTRA01Y.cpy
 * Original COBOL record length: 50 bytes
 * Tracks balance by account, transaction type, and category
 */
@Entity
@Table(name = "TCATBALF")
public class TransactionCategoryBalance {

    @EmbeddedId
    private TransactionCategoryBalanceKey id;

    @Column(name = "TRAN_CAT_BAL", precision = 11, scale = 2)
    private BigDecimal tranCatBal;

    public TransactionCategoryBalance() {
    }

    public TransactionCategoryBalance(TransactionCategoryBalanceKey id) {
        this.id = id;
    }

    public TransactionCategoryBalanceKey getId() {
        return id;
    }

    public void setId(TransactionCategoryBalanceKey id) {
        this.id = id;
    }

    public BigDecimal getTranCatBal() {
        return tranCatBal;
    }

    public void setTranCatBal(BigDecimal tranCatBal) {
        this.tranCatBal = tranCatBal;
    }

    @Override
    public String toString() {
        return "TransactionCategoryBalance{" +
                "id=" + id +
                ", tranCatBal=" + tranCatBal +
                '}';
    }

    @Embeddable
    public static class TransactionCategoryBalanceKey implements Serializable {

        @Column(name = "TRANCAT_ACCT_ID")
        private Long trancatAcctId;

        @Column(name = "TRANCAT_TYPE_CD", length = 2)
        private String trancatTypeCd;

        @Column(name = "TRANCAT_CD")
        private Integer trancatCd;

        public TransactionCategoryBalanceKey() {
        }

        public TransactionCategoryBalanceKey(Long trancatAcctId, String trancatTypeCd, Integer trancatCd) {
            this.trancatAcctId = trancatAcctId;
            this.trancatTypeCd = trancatTypeCd;
            this.trancatCd = trancatCd;
        }

        public Long getTrancatAcctId() {
            return trancatAcctId;
        }

        public void setTrancatAcctId(Long trancatAcctId) {
            this.trancatAcctId = trancatAcctId;
        }

        public String getTrancatTypeCd() {
            return trancatTypeCd;
        }

        public void setTrancatTypeCd(String trancatTypeCd) {
            this.trancatTypeCd = trancatTypeCd;
        }

        public Integer getTrancatCd() {
            return trancatCd;
        }

        public void setTrancatCd(Integer trancatCd) {
            this.trancatCd = trancatCd;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransactionCategoryBalanceKey that = (TransactionCategoryBalanceKey) o;
            return Objects.equals(trancatAcctId, that.trancatAcctId) &&
                    Objects.equals(trancatTypeCd, that.trancatTypeCd) &&
                    Objects.equals(trancatCd, that.trancatCd);
        }

        @Override
        public int hashCode() {
            return Objects.hash(trancatAcctId, trancatTypeCd, trancatCd);
        }

        @Override
        public String toString() {
            return "TransactionCategoryBalanceKey{" +
                    "trancatAcctId=" + trancatAcctId +
                    ", trancatTypeCd='" + trancatTypeCd + '\'' +
                    ", trancatCd=" + trancatCd +
                    '}';
        }
    }
}
