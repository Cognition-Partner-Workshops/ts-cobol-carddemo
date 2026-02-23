package com.carddemo.core.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key for TransactionCategory entity.
 * Maps the COBOL composite key: (TRAN-TYPE-CD, TRAN-CAT-CD)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TransactionCategoryId implements Serializable {

    private String typeCode;
    private Integer categoryCode;
}
