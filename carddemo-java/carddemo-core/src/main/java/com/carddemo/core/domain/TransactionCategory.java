package com.carddemo.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transaction category entity mapped from COBOL copybook CVTRA04Y.
 * Original VSAM file: AWS.M2.CARDDEMO.TRANCATG.PS (KSDS, 60-byte records)
 * Also mirrors DB2 table CARDDEMO.TRANSACTION_TYPE_CATEGORY from the DB2 extension module.
 * Composite primary key: (TRAN-TYPE-CD, TRAN-CAT-CD)
 */
@Entity
@Table(name = "transaction_category")
@IdClass(TransactionCategoryId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionCategory {

    @Id
    @Column(name = "type_code", length = 2)
    private String typeCode;

    @Id
    @Column(name = "category_code")
    private Integer categoryCode;

    @NotNull
    @Column(name = "description", length = 50)
    private String description;
}
