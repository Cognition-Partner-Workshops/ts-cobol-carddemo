package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Migrated from DB2 table CARDDEMO.TRANSACTION_TYPE.
 * Part of optional module: app-transaction-type-db2
 */
@Entity
@Table(name = "transaction_type")
public class TransactionTypeDb2 {

    @Id
    @Column(name = "tr_type", length = 2, nullable = false)
    private String trType;

    @Column(name = "tr_description", length = 50)
    private String trDescription;

    public TransactionTypeDb2() {
    }

    public String getTrType() { return trType; }
    public void setTrType(String trType) { this.trType = trType; }
    public String getTrDescription() { return trDescription; }
    public void setTrDescription(String trDescription) { this.trDescription = trDescription; }
}
