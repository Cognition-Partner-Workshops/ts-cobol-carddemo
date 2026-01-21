package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * JPA entity representing a transaction category balance.
 * Migrated from mainframe copybook: CVTRA01Y.cpy (TRAN-CAT-BAL-RECORD)
 *
 * <p>This entity uses a composite key consisting of account ID, type code, and category code.
 * It tracks the balance for each transaction category within an account.
 *
 * @see com.carddemo.repository.TransactionCategoryBalanceRepository
 */
@Entity
@Table(name = "transaction_category_balances")
public class TransactionCategoryBalance {

    @EmbeddedId
    private TransactionCategoryBalanceId id;

    @NotNull
    @Column(name = "balance", precision = 11, scale = 2, nullable = false)
    private BigDecimal balance;

    public TransactionCategoryBalance() {
    }

    public TransactionCategoryBalance(Long accountId, String typeCode, Integer categoryCode, BigDecimal balance) {
        this.id = new TransactionCategoryBalanceId(accountId, typeCode, categoryCode);
        this.balance = balance;
    }

    public TransactionCategoryBalanceId getId() {
        return id;
    }

    public void setId(TransactionCategoryBalanceId id) {
        this.id = id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Long getAccountId() {
        return id != null ? id.getAccountId() : null;
    }

    public String getTypeCode() {
        return id != null ? id.getTypeCode() : null;
    }

    public Integer getCategoryCode() {
        return id != null ? id.getCategoryCode() : null;
    }

    @Override
    public String toString() {
        return "TransactionCategoryBalance{" +
                "id=" + id +
                ", balance=" + balance +
                '}';
    }

    /**
     * Composite primary key for TransactionCategoryBalance entity.
     * Consists of account ID, type code, and category code.
     */
    @Embeddable
    public static class TransactionCategoryBalanceId implements Serializable {

        private static final long serialVersionUID = 1L;

        @Column(name = "account_id")
        private Long accountId;

        @Size(max = 2)
        @Column(name = "type_code", length = 2)
        private String typeCode;

        @Column(name = "category_code")
        private Integer categoryCode;

        public TransactionCategoryBalanceId() {
        }

        public TransactionCategoryBalanceId(Long accountId, String typeCode, Integer categoryCode) {
            this.accountId = accountId;
            this.typeCode = typeCode;
            this.categoryCode = categoryCode;
        }

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        public String getTypeCode() {
            return typeCode;
        }

        public void setTypeCode(String typeCode) {
            this.typeCode = typeCode;
        }

        public Integer getCategoryCode() {
            return categoryCode;
        }

        public void setCategoryCode(Integer categoryCode) {
            this.categoryCode = categoryCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransactionCategoryBalanceId that = (TransactionCategoryBalanceId) o;
            return Objects.equals(accountId, that.accountId) &&
                    Objects.equals(typeCode, that.typeCode) &&
                    Objects.equals(categoryCode, that.categoryCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountId, typeCode, categoryCode);
        }

        @Override
        public String toString() {
            return "TransactionCategoryBalanceId{" +
                    "accountId=" + accountId +
                    ", typeCode='" + typeCode + '\'' +
                    ", categoryCode=" + categoryCode +
                    '}';
        }
    }
}
