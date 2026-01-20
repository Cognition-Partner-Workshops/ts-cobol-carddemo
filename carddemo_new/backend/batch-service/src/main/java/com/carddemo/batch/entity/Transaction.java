package com.carddemo.batch.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @Column(name = "transaction_id", length = 16)
    private String transactionId;

    @Column(name = "type_code", length = 2)
    private String typeCode;

    @Column(name = "category_code", length = 4)
    private String categoryCode;

    @Column(name = "source", length = 10)
    private String source;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "merchant_id", length = 9)
    private String merchantId;

    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    @Column(name = "merchant_city", length = 30)
    private String merchantCity;

    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;

    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @Column(name = "account_id", length = 11)
    private String accountId;

    @Column(name = "original_timestamp")
    private LocalDateTime originalTimestamp;

    @Column(name = "posted_timestamp")
    private LocalDateTime postedTimestamp;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "interest_amount", precision = 12, scale = 2)
    private BigDecimal interestAmount;
}
