package com.aws.carddemo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_xref")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardXref {

    @Id
    @Column(name = "xref_card_num", length = 16)
    private String xrefCardNum;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "xref_card_num", insertable = false, updatable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "xref_cust_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "xref_acct_id", nullable = false)
    private Account account;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
