package com.aws.carddemo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tran_cat_balance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(TranCatBalance.TranCatBalanceId.class)
public class TranCatBalance {

    @Id
    @Column(name = "trancat_acct_id")
    private Long trancatAcctId;

    @Id
    @Column(name = "trancat_type_cd", length = 2)
    private String trancatTypeCd;

    @Id
    @Column(name = "trancat_cd")
    private Integer trancatCd;

    @Column(name = "tran_cat_bal", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tranCatBal = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trancat_acct_id", insertable = false, updatable = false)
    private Account account;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranCatBalanceId implements Serializable {
        private Long trancatAcctId;
        private String trancatTypeCd;
        private Integer trancatCd;
    }
}
