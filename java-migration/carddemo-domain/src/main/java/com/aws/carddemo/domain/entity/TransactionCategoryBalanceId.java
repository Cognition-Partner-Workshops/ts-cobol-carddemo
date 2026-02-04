package com.aws.carddemo.domain.entity;

import lombok.*;
import java.io.Serializable;

/**
 * Composite primary key for TransactionCategoryBalance entity
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TransactionCategoryBalanceId implements Serializable {
    private Long account;
    private String transactionTypeCode;
    private Integer transactionCategoryCode;
}
