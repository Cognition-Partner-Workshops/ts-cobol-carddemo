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

import java.time.LocalDate;

/**
 * Card entity mapped from COBOL copybook CVACT02Y.
 * Original VSAM file: AWS.M2.CARDDEMO.CARDDATA.PS (KSDS, 150-byte records)
 * Primary key: CARD-NUM PIC X(16)
 */
@Entity
@Table(name = "card", indexes = {
        @Index(name = "idx_card_acct_id", columnList = "acct_id"),
        @Index(name = "idx_card_active_status", columnList = "active_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @Column(name = "card_num", length = 16)
    private String cardNum;

    @NotNull
    @Column(name = "acct_id")
    private Long acctId;

    @Column(name = "cvv_code")
    private Integer cvvCode;

    @Column(name = "embossed_name", length = 50)
    private String embossedName;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "active_status", length = 1)
    private String activeStatus;
}
