package com.carddemo.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "disclosure_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(DisclosureGroup.DisclosureGroupId.class)
public class DisclosureGroup extends BaseEntity {

    @Id
    @Column(name = "account_group_id", length = 10)
    private String accountGroupId;

    @Id
    @Column(name = "transaction_type_code", length = 2)
    private String transactionTypeCode;

    @Id
    @Column(name = "transaction_category_code")
    private Integer transactionCategoryCode;

    @Column(name = "interest_rate", precision = 6, scale = 2, nullable = false)
    private BigDecimal interestRate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisclosureGroupId implements Serializable {
        private String accountGroupId;
        private String transactionTypeCode;
        private Integer transactionCategoryCode;
    }
}
