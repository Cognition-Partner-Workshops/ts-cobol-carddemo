package com.carddemo.common.entity;

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
public class Transaction extends BaseEntity {

    @Id
    @Column(name = "transaction_id", length = 16)
    private String transactionId;

    @Column(name = "transaction_type_code", length = 2, nullable = false)
    private String transactionTypeCode;

    @Column(name = "transaction_category_code", nullable = false)
    private Integer transactionCategoryCode;

    @Column(name = "transaction_source", length = 10)
    private String transactionSource;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "amount", precision = 11, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    @Column(name = "merchant_city", length = 50)
    private String merchantCity;

    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;

    @Column(name = "card_number", length = 16, nullable = false)
    private String cardNumber;

    @Column(name = "origination_timestamp", nullable = false)
    private LocalDateTime originationTimestamp;

    @Column(name = "processing_timestamp")
    private LocalDateTime processingTimestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_number", insertable = false, updatable = false)
    private Card card;

    public boolean isCredit() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isDebit() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
