package com.carddemo.authorization.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    @Id
    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @Column(name = "account_id", length = 11)
    private String accountId;

    @Column(name = "customer_id", length = 9)
    private String customerId;

    @Column(name = "card_status", length = 1)
    private String cardStatus;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "cvv", length = 3)
    private String cvv;
}
