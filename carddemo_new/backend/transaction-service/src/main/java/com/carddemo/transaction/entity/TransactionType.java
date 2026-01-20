package com.carddemo.transaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transaction_types")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionType {
    @Id
    @Column(name = "type_code", length = 2)
    private String typeCode;

    @Column(name = "description", length = 50)
    private String description;
}
