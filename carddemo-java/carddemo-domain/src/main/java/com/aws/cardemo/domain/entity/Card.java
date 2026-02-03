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

/**
 * JPA Entity representing a credit card in the CardDemo system.
 * 
 * This entity maps to the 'cards' table and stores all card-related information
 * including card number, CVV, expiry date, and activation status. It represents
 * the modernized version of the COBOL CARDDATA-RECORD from the original mainframe application.
 * 
 * Card active status codes:
 * - 'Y' = Active (card can be used for transactions)
 * - 'N' = Inactive (card is disabled)
 * 
 * Note: Card data includes sensitive information (CVV) that should be handled securely
 * and access should be restricted. CVV should never be stored in production systems
 * per PCI-DSS compliance requirements.
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    /**
     * 16-digit card number (primary key).
     * This is the unique identifier for the card.
     */
    @Id
    @Column(name = "card_number", length = 16)
    private String cardNumber;

    /**
     * Associated account identifier.
     * Links the card to its parent account.
     * Required field.
     */
    @NotNull
    @Column(name = "account_id", length = 11)
    private String accountId;

    /**
     * Card Verification Value (CVV) code.
     * 3-digit security code on the back of the card.
     * Note: Should not be stored in production per PCI-DSS.
     */
    @Column(name = "card_cvv_code", length = 3)
    private String cardCvvCode;

    /**
     * Name embossed on the physical card.
     * Typically the cardholder's name.
     */
    @Column(name = "card_embossed_name", length = 50)
    private String cardEmbossedName;

    /**
     * Card expiration date.
     * Format: MM/YYYY or similar.
     */
    @Column(name = "card_expiry_date", length = 10)
    private String cardExpiryDate;

    /**
     * Card activation status.
     * 'Y' = Active (can be used for transactions)
     * 'N' = Inactive (disabled)
     */
    @Column(name = "card_active_status", length = 1)
    private String cardActiveStatus;
}
