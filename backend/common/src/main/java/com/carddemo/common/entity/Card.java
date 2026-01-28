package com.carddemo.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card extends BaseEntity {

    @Id
    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "cvv_code", length = 3, nullable = false)
    private String cvvCode;

    @Column(name = "embossed_name", length = 50, nullable = false)
    private String embossedName;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "active_status", length = 1, nullable = false)
    private String activeStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    public boolean isActive() {
        return "Y".equals(activeStatus);
    }

    public boolean isExpired() {
        return expirationDate.isBefore(LocalDate.now());
    }

    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
