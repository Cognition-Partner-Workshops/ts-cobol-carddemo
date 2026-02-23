package com.carddemo.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Transaction category balance entity mapped from COBOL copybook CVTRA01Y.
 * Original VSAM file: AWS.M2.CARDDEMO.TCATBALF.PS (KSDS, 50-byte records)
 * Composite primary key: (TRANCAT-ACCT-ID, TRANCAT-TYPE-CD, TRANCAT-CD)
 *
 * Tracks running balance for each account's transaction type/category combination.
 * Used by interest calculation batch job (CBACT04C/INTCALC).
 */
@Entity
@Table(name = "tran_cat_balance")
@IdClass(TransactionCategoryBalanceId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionCategoryBalance {

    @Id
    @Column(name = "acct_id")
    private Long acctId;

    @Id
    @Column(name = "type_code", length = 2)
    private String typeCode;

    @Id
    @Column(name = "category_code")
    private Integer categoryCode;

    @Column(name = "balance", precision = 11, scale = 2)
    private BigDecimal balance;
}
