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

@Entity
@Table(name = "cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @NotNull
    @Column(name = "account_id", length = 11)
    private String accountId;

    @Column(name = "card_cvv_code", length = 3)
    private String cardCvvCode;

    @Column(name = "card_embossed_name", length = 50)
    private String cardEmbossedName;

    @Column(name = "card_expiry_date", length = 10)
    private String cardExpiryDate;

    @Column(name = "card_active_status", length = 1)
    private String cardActiveStatus;
}
