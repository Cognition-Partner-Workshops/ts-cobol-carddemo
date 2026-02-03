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

/**
 * JPA Entity representing a credit card transaction in the CardDemo system.
 * 
 * This entity maps to the 'transactions' table and stores all transaction-related information
 * including amount, merchant details, and timestamps. It represents the modernized version
 * of the COBOL TRANDATA-RECORD from the original mainframe application.
 * 
 * Transaction type codes:
 * - 'PU' = Purchase
 * - 'PM' = Payment
 * - 'CR' = Credit/Refund
 * - 'FE' = Fee
 * - 'IN' = Interest
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    /**
     * Unique transaction identifier (primary key).
     * Maximum length: 16 characters.
     */
    @Id
    @Column(name = "transaction_id", length = 16)
    private String transactionId;

    /**
     * Card number associated with this transaction.
     * Links to the Card entity.
     * Required field.
     */
    @NotNull
    @Column(name = "card_number", length = 16)
    private String cardNumber;

    /**
     * Transaction type code.
     * Examples: 'PU' (Purchase), 'PM' (Payment), 'CR' (Credit).
     * Required field.
     */
    @NotNull
    @Column(name = "transaction_type_code", length = 2)
    private String transactionTypeCode;

    /**
     * Transaction category code for classification.
     * Used for reporting and analytics.
     */
    @Column(name = "transaction_category_code", length = 4)
    private String transactionCategoryCode;

    /**
     * Source of the transaction.
     * Examples: 'POS', 'ONLINE', 'ATM', 'PHONE'.
     */
    @Column(name = "transaction_source", length = 10)
    private String transactionSource;

    /**
     * Human-readable description of the transaction.
     */
    @Column(name = "transaction_description", length = 100)
    private String transactionDescription;

    /**
     * Transaction amount.
     * Positive for debits (purchases), negative for credits (payments/refunds).
     */
    @Column(name = "transaction_amount", precision = 12, scale = 2)
    private BigDecimal transactionAmount;

    /**
     * Merchant identifier for the transaction.
     */
    @Column(name = "merchant_id", length = 9)
    private String merchantId;

    /**
     * Name of the merchant.
     */
    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    /**
     * City where the merchant is located.
     */
    @Column(name = "merchant_city", length = 50)
    private String merchantCity;

    /**
     * ZIP/postal code of the merchant location.
     */
    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;

    /**
     * Timestamp when the transaction was processed.
     */
    @Column(name = "transaction_timestamp")
    private LocalDateTime transactionTimestamp;

    /**
     * Original timestamp of the transaction (for batch imports).
     * May differ from transactionTimestamp for migrated data.
     */
    @Column(name = "original_timestamp")
    private LocalDateTime originalTimestamp;
}
