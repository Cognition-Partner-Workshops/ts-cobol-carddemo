package com.aws.carddemo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @Column(name = "card_num", length = 16)
    private String cardNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_acct_id", nullable = false)
    private Account account;

    @Column(name = "card_cvv_cd", nullable = false, length = 3)
    private String cardCvvCd;

    @Column(name = "card_embossed_name", length = 50)
    private String cardEmbossedName;

    @Column(name = "card_expiration_date", nullable = false)
    private LocalDate cardExpirationDate;

    @Column(name = "card_active_status", nullable = false, length = 1)
    @Builder.Default
    private String cardActiveStatus = "Y";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @OneToOne(mappedBy = "card", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CardXref cardXref;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return "Y".equals(this.cardActiveStatus);
    }

    public boolean isExpired() {
        return this.cardExpirationDate != null && LocalDate.now().isAfter(this.cardExpirationDate);
    }

    public String getMaskedCardNumber() {
        if (cardNum == null || cardNum.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNum.substring(cardNum.length() - 4);
    }
}
