package com.carddemo.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "card_account_xref")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(CardAccountXref.CardAccountXrefId.class)
public class CardAccountXref extends BaseEntity {

    @Id
    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Id
    @Column(name = "account_id")
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_number", insertable = false, updatable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardAccountXrefId implements Serializable {
        private String cardNumber;
        private Long customerId;
        private Long accountId;
    }
}
