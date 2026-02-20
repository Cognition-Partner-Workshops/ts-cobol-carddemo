package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_authorizations")
public class PendingAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_num", length = 16)
    private String cardNum;

    @Column(name = "acct_id")
    private Long acctId;

    @Column(name = "auth_amount", precision = 11, scale = 2)
    private BigDecimal authAmount;

    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    @Column(name = "auth_status", length = 20)
    private String authStatus;

    @Column(name = "auth_date")
    private LocalDateTime authDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "fraud_flag")
    private Boolean fraudFlag;

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

    public BigDecimal getAuthAmount() {
        return authAmount;
    }

    public void setAuthAmount(BigDecimal authAmount) {
        this.authAmount = authAmount;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(String authStatus) {
        this.authStatus = authStatus;
    }

    public LocalDateTime getAuthDate() {
        return authDate;
    }

    public void setAuthDate(LocalDateTime authDate) {
        this.authDate = authDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Boolean getFraudFlag() {
        return fraudFlag;
    }

    public void setFraudFlag(Boolean fraudFlag) {
        this.fraudFlag = fraudFlag;
    }
}
