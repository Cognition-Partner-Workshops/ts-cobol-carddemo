package com.aws.carddemo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(name = "acct_id")
    private Long acctId;

    @Column(name = "acct_active_status", nullable = false, length = 1)
    @Builder.Default
    private String acctActiveStatus = "Y";

    @Column(name = "acct_curr_bal", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal acctCurrBal = BigDecimal.ZERO;

    @Column(name = "acct_credit_limit", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal acctCreditLimit = BigDecimal.ZERO;

    @Column(name = "acct_cash_credit_limit", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal acctCashCreditLimit = BigDecimal.ZERO;

    @Column(name = "acct_open_date", nullable = false)
    private LocalDate acctOpenDate;

    @Column(name = "acct_expiration_date", nullable = false)
    private LocalDate acctExpirationDate;

    @Column(name = "acct_reissue_date")
    private LocalDate acctReissueDate;

    @Column(name = "acct_curr_cyc_credit", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal acctCurrCycCredit = BigDecimal.ZERO;

    @Column(name = "acct_curr_cyc_debit", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal acctCurrCycDebit = BigDecimal.ZERO;

    @Column(name = "acct_addr_zip", length = 10)
    private String acctAddrZip;

    @Column(name = "acct_group_id", length = 10)
    private String acctGroupId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Card> cards = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return "Y".equals(this.acctActiveStatus);
    }

    public boolean isExpired() {
        return this.acctExpirationDate != null && LocalDate.now().isAfter(this.acctExpirationDate);
    }

    public BigDecimal getAvailableCredit() {
        return this.acctCreditLimit.subtract(this.acctCurrBal);
    }

    public BigDecimal getProjectedBalance(BigDecimal transactionAmount) {
        return this.acctCurrCycCredit
                .subtract(this.acctCurrCycDebit)
                .add(transactionAmount);
    }
}
