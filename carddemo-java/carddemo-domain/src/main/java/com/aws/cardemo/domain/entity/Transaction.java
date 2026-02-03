package com.aws.cardemo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @Column(name = "transaction_id", length = 16)
    private String transactionId;

    @NotNull
    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @NotNull
    @Column(name = "transaction_type_code", length = 2)
    private String transactionTypeCode;

    @Column(name = "transaction_category_code", length = 4)
    private String transactionCategoryCode;

    @Column(name = "transaction_source", length = 10)
    private String transactionSource;

    @Column(name = "transaction_description", length = 100)
    private String transactionDescription;

    @Column(name = "transaction_amount", precision = 12, scale = 2)
    private BigDecimal transactionAmount;

    @Column(name = "merchant_id", length = 9)
    private String merchantId;

    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    @Column(name = "merchant_city", length = 50)
    private String merchantCity;

    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;

    @Column(name = "transaction_timestamp")
    private LocalDateTime transactionTimestamp;

    @Column(name = "original_timestamp")
    private LocalDateTime originalTimestamp;
}
