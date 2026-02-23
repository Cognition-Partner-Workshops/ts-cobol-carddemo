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
 * Disclosure group entity mapped from COBOL copybook CVTRA02Y.
 * Original VSAM file: AWS.M2.CARDDEMO.DISCGRP.PS (KSDS, 50-byte records)
 * Composite primary key: (DIS-ACCT-GROUP-ID, DIS-TRAN-TYPE-CD, DIS-TRAN-CAT-CD)
 *
 * Defines interest rates for each account group + transaction type/category combination.
 * Used by interest calculation batch job (CBACT04C/INTCALC).
 */
@Entity
@Table(name = "disclosure_group")
@IdClass(DisclosureGroupId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisclosureGroup {

    @Id
    @Column(name = "acct_group_id", length = 10)
    private String acctGroupId;

    @Id
    @Column(name = "tran_type_code", length = 2)
    private String tranTypeCode;

    @Id
    @Column(name = "tran_cat_code")
    private Integer tranCatCode;

    @Column(name = "interest_rate", precision = 6, scale = 2)
    private BigDecimal interestRate;
}
