package com.carddemo.transactiontype.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "transaction_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCategory {
    @Id
    @Column(name = "category_code", length = 4)
    private String categoryCode;

    @Column(name = "category_description", length = 50)
    private String categoryDescription;

    @Column(name = "parent_category_code", length = 4)
    private String parentCategoryCode;

    @Column(name = "category_type", length = 20)
    private String categoryType;

    @Column(name = "merchant_category_code", length = 4)
    private String merchantCategoryCode;

    @Column(name = "reporting_group", length = 20)
    private String reportingGroup;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "display_order")
    private Integer displayOrder;
}
