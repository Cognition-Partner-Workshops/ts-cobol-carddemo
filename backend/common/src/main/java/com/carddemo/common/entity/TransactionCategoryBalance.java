package com.carddemo.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "transaction_category_balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(TransactionCategoryBalance.TransactionCategoryBalanceId.class)
public class TransactionCategoryBalance extends BaseEntity {

    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Id
    @Column(name = "transaction_type_code", length = 2)
    private String transactionTypeCode;

    @Id
    @Column(name = "transaction_category_code")
    private Integer transactionCategoryCode;

    @Column(name = "balance", precision = 11, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionCategoryBalanceId implements Serializable {
        private Long accountId;
        private String transactionTypeCode;
        private Integer transactionCategoryCode;
    }
}
