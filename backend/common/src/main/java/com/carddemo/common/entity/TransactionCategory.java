package com.carddemo.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "transaction_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(TransactionCategory.TransactionCategoryId.class)
public class TransactionCategory extends BaseEntity {

    @Id
    @Column(name = "transaction_type_code", length = 2)
    private String transactionTypeCode;

    @Id
    @Column(name = "transaction_category_code")
    private Integer transactionCategoryCode;

    @Column(name = "description", length = 50, nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type_code", insertable = false, updatable = false)
    private TransactionType transactionType;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionCategoryId implements Serializable {
        private String transactionTypeCode;
        private Integer transactionCategoryCode;
    }
}
