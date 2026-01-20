package com.carddemo.transactiontype.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transaction_category_balances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCategoryBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", length = 11)
    private String accountId;

    @Column(name = "category_code", length = 4)
    private String categoryCode;

    @Column(name = "balance_date")
    private LocalDate balanceDate;

    @Column(name = "debit_total", precision = 12, scale = 2)
    private BigDecimal debitTotal;

    @Column(name = "credit_total", precision = 12, scale = 2)
    private BigDecimal creditTotal;

    @Column(name = "net_balance", precision = 12, scale = 2)
    private BigDecimal netBalance;

    @Column(name = "transaction_count")
    private Integer transactionCount;
}
