package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Migrated from IMS DB segment PAUTSUM0 (Authorization Summary - root segment).
 * Part of optional module: app-authorization-ims-db2-mq
 */
@Entity
@Table(name = "auth_summary", indexes = {
    @Index(name = "idx_auth_summary_card", columnList = "card_num")
})
public class AuthSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "card_num", length = 16, nullable = false)
    private String cardNum;

    @Column(name = "acct_id")
    private Long acctId;

    @Column(name = "cust_id")
    private Long custId;

    @Column(name = "total_auth_amt", precision = 12, scale = 2)
    private BigDecimal totalAuthAmt;

    @Column(name = "auth_count")
    private Integer authCount;

    @Column(name = "last_auth_ts", length = 26)
    private String lastAuthTs;

    public AuthSummary() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardNum() {
        return cardNum;
    }

    public void setCardNum(String cardNum) {
        this.cardNum = cardNum;
    }

    public Long getAcctId() {
        return acctId;
    }

    public void setAcctId(Long acctId) {
        this.acctId = acctId;
    }

    public Long getCustId() {
        return custId;
    }

    public void setCustId(Long custId) {
        this.custId = custId;
    }

    public BigDecimal getTotalAuthAmt() {
        return totalAuthAmt;
    }

    public void setTotalAuthAmt(BigDecimal totalAuthAmt) {
        this.totalAuthAmt = totalAuthAmt;
    }

    public Integer getAuthCount() {
        return authCount;
    }

    public void setAuthCount(Integer authCount) {
        this.authCount = authCount;
    }

    public String getLastAuthTs() {
        return lastAuthTs;
    }

    public void setLastAuthTs(String lastAuthTs) {
        this.lastAuthTs = lastAuthTs;
    }
}
