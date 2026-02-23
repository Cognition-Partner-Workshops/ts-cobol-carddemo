package com.carddemo.core.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key for TransactionCategoryBalance entity.
 * Maps the COBOL composite key: (TRANCAT-ACCT-ID, TRANCAT-TYPE-CD, TRANCAT-CD)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TransactionCategoryBalanceId implements Serializable {

    private Long acctId;
    private String typeCode;
    private Integer categoryCode;
}
