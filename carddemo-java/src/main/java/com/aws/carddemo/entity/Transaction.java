package com.aws.carddemo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @Column(name = "tran_id", length = 16)
    private String tranId;

    @Column(name = "tran_type_cd", nullable = false, length = 2)
    private String tranTypeCd;

    @Column(name = "tran_cat_cd", nullable = false)
    private Integer tranCatCd;

    @Column(name = "tran_source", length = 10)
    private String tranSource;

    @Column(name = "tran_desc", length = 100)
    private String tranDesc;

    @Column(name = "tran_amt", nullable = false, precision = 11, scale = 2)
    private BigDecimal tranAmt;

    @Column(name = "tran_merchant_id")
    private Long tranMerchantId;

    @Column(name = "tran_merchant_name", length = 50)
    private String tranMerchantName;

    @Column(name = "tran_merchant_city", length = 50)
    private String tranMerchantCity;

    @Column(name = "tran_merchant_zip", length = 10)
    private String tranMerchantZip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tran_card_num", nullable = false)
    private Card card;

    @Column(name = "tran_orig_ts", nullable = false)
    private LocalDateTime tranOrigTs;

    @Column(name = "tran_proc_ts")
    private LocalDateTime tranProcTs;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isCredit() {
        return this.tranAmt != null && this.tranAmt.compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean isDebit() {
        return this.tranAmt != null && this.tranAmt.compareTo(BigDecimal.ZERO) < 0;
    }
}
