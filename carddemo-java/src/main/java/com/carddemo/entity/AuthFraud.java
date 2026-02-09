package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "auth_fraud")
public class AuthFraud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fraud_id")
    private Long fraudId;

    @Column(name = "card_num", nullable = false, length = 16)
    private String cardNum;

    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Column(name = "fraud_date", length = 10)
    private String fraudDate;

    @Column(name = "fraud_amount", precision = 11, scale = 2)
    private BigDecimal fraudAmount;

    @Column(name = "fraud_reason", length = 100)
    private String fraudReason;

    public AuthFraud() {}

    public Long getFraudId() { return fraudId; }
    public void setFraudId(Long fraudId) { this.fraudId = fraudId; }
    public String getCardNum() { return cardNum; }
    public void setCardNum(String cardNum) { this.cardNum = cardNum; }
    public Long getAcctId() { return acctId; }
    public void setAcctId(Long acctId) { this.acctId = acctId; }
    public String getFraudDate() { return fraudDate; }
    public void setFraudDate(String fraudDate) { this.fraudDate = fraudDate; }
    public BigDecimal getFraudAmount() { return fraudAmount; }
    public void setFraudAmount(BigDecimal fraudAmount) { this.fraudAmount = fraudAmount; }
    public String getFraudReason() { return fraudReason; }
    public void setFraudReason(String fraudReason) { this.fraudReason = fraudReason; }
}
