package com.aws.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Transaction entity - migrated from COBOL copybook CVTRA05Y.cpy
 * Original COBOL record length: 350 bytes
 */
@Entity
@Table(name = "TRANSACT")
public class Transaction {

    @Id
    @Column(name = "TRAN_ID", length = 16)
    private String tranId;

    @Column(name = "TRAN_TYPE_CD", length = 2)
    private String tranTypeCd;

    @Column(name = "TRAN_CAT_CD")
    private Integer tranCatCd;

    @Column(name = "TRAN_SOURCE", length = 10)
    private String tranSource;

    @Column(name = "TRAN_DESC", length = 100)
    private String tranDesc;

    @Column(name = "TRAN_AMT", precision = 11, scale = 2)
    private BigDecimal tranAmt;

    @Column(name = "TRAN_MERCHANT_ID")
    private Long tranMerchantId;

    @Column(name = "TRAN_MERCHANT_NAME", length = 50)
    private String tranMerchantName;

    @Column(name = "TRAN_MERCHANT_CITY", length = 50)
    private String tranMerchantCity;

    @Column(name = "TRAN_MERCHANT_ZIP", length = 10)
    private String tranMerchantZip;

    @Column(name = "TRAN_CARD_NUM", length = 16)
    private String tranCardNum;

    @Column(name = "TRAN_ORIG_TS", length = 26)
    private String tranOrigTs;

    @Column(name = "TRAN_PROC_TS", length = 26)
    private String tranProcTs;

    public Transaction() {
    }

    public Transaction(String tranId) {
        this.tranId = tranId;
    }

    public String getTranId() {
        return tranId;
    }

    public void setTranId(String tranId) {
        this.tranId = tranId;
    }

    public String getTranTypeCd() {
        return tranTypeCd;
    }

    public void setTranTypeCd(String tranTypeCd) {
        this.tranTypeCd = tranTypeCd;
    }

    public Integer getTranCatCd() {
        return tranCatCd;
    }

    public void setTranCatCd(Integer tranCatCd) {
        this.tranCatCd = tranCatCd;
    }

    public String getTranSource() {
        return tranSource;
    }

    public void setTranSource(String tranSource) {
        this.tranSource = tranSource;
    }

    public String getTranDesc() {
        return tranDesc;
    }

    public void setTranDesc(String tranDesc) {
        this.tranDesc = tranDesc;
    }

    public BigDecimal getTranAmt() {
        return tranAmt;
    }

    public void setTranAmt(BigDecimal tranAmt) {
        this.tranAmt = tranAmt;
    }

    public Long getTranMerchantId() {
        return tranMerchantId;
    }

    public void setTranMerchantId(Long tranMerchantId) {
        this.tranMerchantId = tranMerchantId;
    }

    public String getTranMerchantName() {
        return tranMerchantName;
    }

    public void setTranMerchantName(String tranMerchantName) {
        this.tranMerchantName = tranMerchantName;
    }

    public String getTranMerchantCity() {
        return tranMerchantCity;
    }

    public void setTranMerchantCity(String tranMerchantCity) {
        this.tranMerchantCity = tranMerchantCity;
    }

    public String getTranMerchantZip() {
        return tranMerchantZip;
    }

    public void setTranMerchantZip(String tranMerchantZip) {
        this.tranMerchantZip = tranMerchantZip;
    }

    public String getTranCardNum() {
        return tranCardNum;
    }

    public void setTranCardNum(String tranCardNum) {
        this.tranCardNum = tranCardNum;
    }

    public String getTranOrigTs() {
        return tranOrigTs;
    }

    public void setTranOrigTs(String tranOrigTs) {
        this.tranOrigTs = tranOrigTs;
    }

    public String getTranProcTs() {
        return tranProcTs;
    }

    public void setTranProcTs(String tranProcTs) {
        this.tranProcTs = tranProcTs;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "tranId='" + tranId + '\'' +
                ", tranTypeCd='" + tranTypeCd + '\'' +
                ", tranCatCd=" + tranCatCd +
                ", tranSource='" + tranSource + '\'' +
                ", tranDesc='" + tranDesc + '\'' +
                ", tranAmt=" + tranAmt +
                ", tranMerchantId=" + tranMerchantId +
                ", tranMerchantName='" + tranMerchantName + '\'' +
                ", tranMerchantCity='" + tranMerchantCity + '\'' +
                ", tranMerchantZip='" + tranMerchantZip + '\'' +
                ", tranCardNum='" + tranCardNum + '\'' +
                ", tranOrigTs='" + tranOrigTs + '\'' +
                ", tranProcTs='" + tranProcTs + '\'' +
                '}';
    }
}
