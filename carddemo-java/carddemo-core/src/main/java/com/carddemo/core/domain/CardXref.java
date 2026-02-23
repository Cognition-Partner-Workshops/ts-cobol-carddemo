package com.carddemo.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Card-Account-Customer cross-reference entity mapped from COBOL copybook CVACT03Y.
 * Original VSAM file: AWS.M2.CARDDEMO.CARDXREF.PS (KSDS, 50-byte records)
 * Primary key: XREF-CARD-NUM PIC X(16)
 *
 * This table links cards to their owning customer and account.
 * VSAM AIX equivalents are implemented via database indexes.
 */
@Entity
@Table(name = "card_xref", indexes = {
        @Index(name = "idx_xref_cust_id", columnList = "cust_id"),
        @Index(name = "idx_xref_acct_id", columnList = "acct_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardXref {

    @Id
    @Column(name = "card_num", length = 16)
    private String cardNum;

    @NotNull
    @Column(name = "cust_id")
    private Long custId;

    @NotNull
    @Column(name = "acct_id")
    private Long acctId;
}
