package com.carddemo.authorization.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "authorization_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_id", length = 20, unique = true)
    private String authId;

    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @Column(name = "account_id", length = 11)
    private String accountId;

    @Column(name = "merchant_id", length = 15)
    private String merchantId;

    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    @Column(name = "merchant_category_code", length = 4)
    private String merchantCategoryCode;

    @Column(name = "merchant_city", length = 30)
    private String merchantCity;

    @Column(name = "merchant_state", length = 2)
    private String merchantState;

    @Column(name = "merchant_country", length = 3)
    private String merchantCountry;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "transaction_type", length = 10)
    private String transactionType;

    @Column(name = "pos_entry_mode", length = 3)
    private String posEntryMode;

    @Column(name = "request_timestamp")
    private LocalDateTime requestTimestamp;

    @Column(name = "response_timestamp")
    private LocalDateTime responseTimestamp;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "response_code", length = 3)
    private String responseCode;

    @Column(name = "decline_reason", length = 100)
    private String declineReason;

    @Column(name = "available_credit_before", precision = 12, scale = 2)
    private BigDecimal availableCreditBefore;

    @Column(name = "available_credit_after", precision = 12, scale = 2)
    private BigDecimal availableCreditAfter;
}
