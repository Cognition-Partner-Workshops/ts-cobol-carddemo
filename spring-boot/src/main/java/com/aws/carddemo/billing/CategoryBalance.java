package com.aws.carddemo.billing;

import java.math.BigDecimal;

import com.aws.carddemo.account.Account;
import com.aws.carddemo.transaction.TransactionCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "category_balance")
@IdClass(CategoryBalanceId.class)
public class CategoryBalance {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acct_id", referencedColumnName = "id")
    private Account account;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cat_cd", referencedColumnName = "cat_cd")
    private TransactionCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public void setCategory(TransactionCategory category) {
        this.category = category;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
