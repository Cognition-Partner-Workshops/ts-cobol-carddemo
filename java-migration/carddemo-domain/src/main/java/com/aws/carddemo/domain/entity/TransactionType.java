package com.aws.carddemo.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Transaction Type entity - for the Transaction Type Management Module
 * Migrated from DB2 tables in app-transaction-type-db2
 */
@Entity
@Table(name = "transaction_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionType {

    @Id
    @Size(max = 2)
    @Column(name = "type_code", length = 2)
    private String typeCode;

    @NotBlank
    @Size(max = 50)
    @Column(name = "type_description", length = 50, nullable = false)
    private String typeDescription;

    @Column(name = "category_code")
    private Integer categoryCode;

    @Size(max = 50)
    @Column(name = "category_description", length = 50)
    private String categoryDescription;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Version
    private Long version;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
