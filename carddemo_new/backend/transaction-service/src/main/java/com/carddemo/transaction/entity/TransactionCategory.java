package com.carddemo.transaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transaction_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCategory {
    @Id
    @Column(name = "category_code", length = 4)
    private String categoryCode;

    @Column(name = "description", length = 50)
    private String description;
}
