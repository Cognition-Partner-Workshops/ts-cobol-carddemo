package com.carddemo.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Authorization summary entity mapped from IMS segment PAUTSUM0 (copybook CIPAUSMY).
 * Original IMS database: DBPAUTP0 (HIDAM primary), root segment.
 * Primary key: PA-ACCT-ID (account ID)
 *
 * The OCCURS 5 TIMES clause for PA-ACCOUNT-STATUS is flattened to 5 individual columns.
 */
@Entity
@Table(name = "authorization_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizationSummary {

    @Id
    @Column(name = "acct_id")
    private Long acctId;

    @Column(name = "cust_id")
    private Long custId;

    @Column(name = "auth_status", length = 1)
    private String authStatus;

    @Column(name = "account_status_1", length = 2)
    private String accountStatus1;

    @Column(name = "account_status_2", length = 2)
    private String accountStatus2;

    @Column(name = "account_status_3", length = 2)
    private String accountStatus3;

    @Column(name = "account_status_4", length = 2)
    private String accountStatus4;

    @Column(name = "account_status_5", length = 2)
    private String accountStatus5;

    @Column(name = "credit_limit", precision = 11, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "cash_limit", precision = 11, scale = 2)
    private BigDecimal cashLimit;

    @Column(name = "credit_balance", precision = 11, scale = 2)
    private BigDecimal creditBalance;

    @Column(name = "cash_balance", precision = 11, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "approved_auth_count")
    private Integer approvedAuthCount;

    @Column(name = "declined_auth_count")
    private Integer declinedAuthCount;

    @Column(name = "approved_auth_amount", precision = 11, scale = 2)
    private BigDecimal approvedAuthAmount;

    @Column(name = "declined_auth_amount", precision = 11, scale = 2)
    private BigDecimal declinedAuthAmount;
}
