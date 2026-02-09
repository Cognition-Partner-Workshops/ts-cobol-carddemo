package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "authorization_details")
public class AuthorizationDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_detail_id")
    private Long authDetailId;

    @Column(name = "auth_id", nullable = false)
    private Long authId;

    @Column(name = "auth_date", length = 10)
    private String authDate;

    @Column(name = "auth_time", length = 8)
    private String authTime;

    @Column(name = "auth_amount", precision = 11, scale = 2)
    private BigDecimal authAmount;

    @Column(name = "auth_status", length = 1)
    private String authStatus;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    public AuthorizationDetail() {}

    public Long getAuthDetailId() { return authDetailId; }
    public void setAuthDetailId(Long authDetailId) { this.authDetailId = authDetailId; }
    public Long getAuthId() { return authId; }
    public void setAuthId(Long authId) { this.authId = authId; }
    public String getAuthDate() { return authDate; }
    public void setAuthDate(String authDate) { this.authDate = authDate; }
    public String getAuthTime() { return authTime; }
    public void setAuthTime(String authTime) { this.authTime = authTime; }
    public BigDecimal getAuthAmount() { return authAmount; }
    public void setAuthAmount(BigDecimal authAmount) { this.authAmount = authAmount; }
    public String getAuthStatus() { return authStatus; }
    public void setAuthStatus(String authStatus) { this.authStatus = authStatus; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
}
