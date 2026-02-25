package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Migrated from TCATBALF.PS / Copybook CVTRA01Y (50-byte FB records).
 * COBOL: TRAN-CAT-BAL-RECORD
 */
@Entity
@Table(name = "transaction_category_balances")
@IdClass(TransactionCategoryBalance.TransactionCategoryBalanceId.class)
public class TransactionCategoryBalance {

    /** TRANCAT-ACCT-ID PIC 9(11) */
    @Id
    @Column(name = "trancat_acct_id", nullable = false)
    private Long trancatAcctId;

    /** TRANCAT-TYPE-CD PIC X(02) */
    @Id
    @Column(name = "trancat_type_cd", length = 2, nullable = false)
    private String trancatTypeCd;

    /** TRANCAT-CD PIC 9(04) */
    @Id
    @Column(name = "trancat_cd", nullable = false)
    private Integer trancatCd;

    /** TRAN-CAT-BAL PIC S9(09)V99 */
    @Column(name = "tran_cat_bal", precision = 11, scale = 2)
    private BigDecimal tranCatBal;

    public TransactionCategoryBalance() {
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

    public BigDecimal getTranCatBal() {
        return tranCatBal;
    }

    public void setTranCatBal(BigDecimal tranCatBal) {
        this.tranCatBal = tranCatBal;
    }

    public static class TransactionCategoryBalanceId implements Serializable {
        private Long trancatAcctId;
        private String trancatTypeCd;
        private Integer trancatCd;

        public TransactionCategoryBalanceId() {
        }

        public TransactionCategoryBalanceId(Long trancatAcctId, String trancatTypeCd, Integer trancatCd) {
            this.trancatAcctId = trancatAcctId;
            this.trancatTypeCd = trancatTypeCd;
            this.trancatCd = trancatCd;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransactionCategoryBalanceId that = (TransactionCategoryBalanceId) o;
            return Objects.equals(trancatAcctId, that.trancatAcctId) &&
                   Objects.equals(trancatTypeCd, that.trancatTypeCd) &&
                   Objects.equals(trancatCd, that.trancatCd);
        }

        @Override
        public int hashCode() {
            return Objects.hash(trancatAcctId, trancatTypeCd, trancatCd);
        }
    }
}
