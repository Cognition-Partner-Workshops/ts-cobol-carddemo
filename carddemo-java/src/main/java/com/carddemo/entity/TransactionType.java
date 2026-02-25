package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Transaction Type entity - migrated from VSAM file TRANTYPE / copybook CVTRA03Y
 * and DB2 table CARDDEMO.TRANSACTION_TYPE.
 * Original VSAM record length: 60 bytes.
 */
@Entity
@Table(name = "transaction_types")
public class TransactionType {

    @Id
    @Column(name = "type_cd", length = 2, nullable = false)
    @NotBlank
    @Size(max = 2)
    private String typeCd;

    @Column(name = "type_desc", length = 50)
    @Size(max = 50)
    private String typeDesc;

    public TransactionType() {
    }

    public TransactionType(String typeCd, String typeDesc) {
        this.typeCd = typeCd;
        this.typeDesc = typeDesc;
    }

    public String getTypeCd() {
        return typeCd;
    }

    public void setTypeCd(String typeCd) {
        this.typeCd = typeCd;
    }

    public String getTypeDesc() {
        return typeDesc;
    }

    public void setTypeDesc(String typeDesc) {
        this.typeDesc = typeDesc;
    }
}
