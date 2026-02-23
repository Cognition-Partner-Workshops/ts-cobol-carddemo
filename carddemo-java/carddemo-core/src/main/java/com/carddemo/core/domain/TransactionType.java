package com.carddemo.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transaction type entity mapped from COBOL copybook CVTRA03Y.
 * Original VSAM file: AWS.M2.CARDDEMO.TRANTYPE.PS (KSDS, 60-byte records)
 * Also mirrors DB2 table CARDDEMO.TRANSACTION_TYPE from the DB2 extension module.
 * Primary key: TRAN-TYPE PIC X(02)
 */
@Entity
@Table(name = "transaction_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionType {

    @Id
    @Column(name = "type_code", length = 2)
    private String typeCode;

    @NotNull
    @Column(name = "description", length = 50)
    private String description;
}
