package com.carddemo.common.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transaction_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionType extends BaseEntity {

    @Id
    @Column(name = "transaction_type_code", length = 2)
    private String transactionTypeCode;

    @Column(name = "description", length = 50, nullable = false)
    private String description;
}
