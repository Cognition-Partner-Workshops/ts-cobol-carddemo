package com.cardemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for creating new transactions.
 * Migrated from COTRN02C (CT02 transaction) online transaction entry.
 */
public class TransactionRequest {

    @NotBlank(message = "Card number is required")
    private String cardNum;

    @NotBlank(message = "Transaction type code is required")
    private String tranTypeCd;

    @NotNull(message = "Transaction category code is required")
    private Integer tranCatCd;

    private String tranSource;
    private String tranDesc;

    @NotNull(message = "Transaction amount is required")
    private BigDecimal tranAmt;

    private Long tranMerchantId;
    private String tranMerchantName;
    private String tranMerchantCity;
    private String tranMerchantZip;

    public TransactionRequest() {
    }

    public String getCardNum() { return cardNum; }
    public void setCardNum(String cardNum) { this.cardNum = cardNum; }
    public String getTranTypeCd() { return tranTypeCd; }
    public void setTranTypeCd(String tranTypeCd) { this.tranTypeCd = tranTypeCd; }
    public Integer getTranCatCd() { return tranCatCd; }
    public void setTranCatCd(Integer tranCatCd) { this.tranCatCd = tranCatCd; }
    public String getTranSource() { return tranSource; }
    public void setTranSource(String tranSource) { this.tranSource = tranSource; }
    public String getTranDesc() { return tranDesc; }
    public void setTranDesc(String tranDesc) { this.tranDesc = tranDesc; }
    public BigDecimal getTranAmt() { return tranAmt; }
    public void setTranAmt(BigDecimal tranAmt) { this.tranAmt = tranAmt; }
    public Long getTranMerchantId() { return tranMerchantId; }
    public void setTranMerchantId(Long tranMerchantId) { this.tranMerchantId = tranMerchantId; }
    public String getTranMerchantName() { return tranMerchantName; }
    public void setTranMerchantName(String tranMerchantName) { this.tranMerchantName = tranMerchantName; }
    public String getTranMerchantCity() { return tranMerchantCity; }
    public void setTranMerchantCity(String tranMerchantCity) { this.tranMerchantCity = tranMerchantCity; }
    public String getTranMerchantZip() { return tranMerchantZip; }
    public void setTranMerchantZip(String tranMerchantZip) { this.tranMerchantZip = tranMerchantZip; }
}
