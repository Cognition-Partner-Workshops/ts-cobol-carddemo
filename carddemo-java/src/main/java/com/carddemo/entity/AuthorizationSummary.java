package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "authorization_summary")
public class AuthorizationSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_id")
    private Long authId;

    @Column(name = "card_num", nullable = false, length = 16)
    private String cardNum;

    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Column(name = "total_auth_amt", precision = 11, scale = 2)
    private BigDecimal totalAuthAmt;

    @Column(name = "auth_count")
    private Integer authCount;

    @Column(name = "last_auth_date", length = 10)
    private String lastAuthDate;

    public AuthorizationSummary() {}

    public Long getAuthId() { return authId; }
    public void setAuthId(Long authId) { this.authId = authId; }
    public String getCardNum() { return cardNum; }
    public void setCardNum(String cardNum) { this.cardNum = cardNum; }
    public Long getAcctId() { return acctId; }
    public void setAcctId(Long acctId) { this.acctId = acctId; }
    public BigDecimal getTotalAuthAmt() { return totalAuthAmt; }
    public void setTotalAuthAmt(BigDecimal totalAuthAmt) { this.totalAuthAmt = totalAuthAmt; }
    public Integer getAuthCount() { return authCount; }
    public void setAuthCount(Integer authCount) { this.authCount = authCount; }
    public String getLastAuthDate() { return lastAuthDate; }
    public void setLastAuthDate(String lastAuthDate) { this.lastAuthDate = lastAuthDate; }
}
