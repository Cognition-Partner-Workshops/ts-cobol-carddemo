package com.carddemo.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction entity mapped from COBOL copybook CVTRA05Y.
 * Original VSAM file: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS (350-byte records)
 * Primary key: TRAN-ID PIC X(16)
 */
@Entity
@Table(name = "transaction", indexes = {
        @Index(name = "idx_transaction_card_num", columnList = "card_num"),
        @Index(name = "idx_transaction_orig_ts", columnList = "orig_timestamp"),
        @Index(name = "idx_transaction_type_cat", columnList = "type_code, category_code"),
        @Index(name = "idx_transaction_merchant_id", columnList = "merchant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @Column(name = "tran_id", length = 16)
    private String tranId;

    @Column(name = "type_code", length = 2)
    private String typeCode;

    @Column(name = "category_code")
    private Integer categoryCode;

    @Column(name = "source", length = 10)
    private String source;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "amount", precision = 11, scale = 2)
    private BigDecimal amount;

    @Column(name = "merchant_id", length = 15)
    private String merchantId;

    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    @Column(name = "merchant_city", length = 50)
    private String merchantCity;

    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;

    @Column(name = "card_num", length = 16)
    private String cardNum;

    @Column(name = "orig_timestamp")
    private LocalDateTime origTimestamp;

    @Column(name = "proc_timestamp")
    private LocalDateTime procTimestamp;
}
