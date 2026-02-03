package com.aws.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Daily Transaction entity - migrated from COBOL copybook CVTRA06Y.cpy
 * Original COBOL record length: 350 bytes
 * Used for batch processing of daily transactions before posting to master
 */
@Entity
@Table(name = "DALYTRAN")
public class DailyTransaction {

    @Id
    @Column(name = "DALYTRAN_ID", length = 16)
    private String dalytranId;

    @Column(name = "DALYTRAN_TYPE_CD", length = 2)
    private String dalytranTypeCd;

    @Column(name = "DALYTRAN_CAT_CD")
    private Integer dalytranCatCd;

    @Column(name = "DALYTRAN_SOURCE", length = 10)
    private String dalytranSource;

    @Column(name = "DALYTRAN_DESC", length = 100)
    private String dalytranDesc;

    @Column(name = "DALYTRAN_AMT", precision = 11, scale = 2)
    private BigDecimal dalytranAmt;

    @Column(name = "DALYTRAN_MERCHANT_ID")
    private Long dalytranMerchantId;

    @Column(name = "DALYTRAN_MERCHANT_NAME", length = 50)
    private String dalytranMerchantName;

    @Column(name = "DALYTRAN_MERCHANT_CITY", length = 50)
    private String dalytranMerchantCity;

    @Column(name = "DALYTRAN_MERCHANT_ZIP", length = 10)
    private String dalytranMerchantZip;

    @Column(name = "DALYTRAN_CARD_NUM", length = 16)
    private String dalytranCardNum;

    @Column(name = "DALYTRAN_ORIG_TS", length = 26)
    private String dalytranOrigTs;

    @Column(name = "DALYTRAN_PROC_TS", length = 26)
    private String dalytranProcTs;

    public DailyTransaction() {
    }

    public DailyTransaction(String dalytranId) {
        this.dalytranId = dalytranId;
    }

    public String getDalytranId() {
        return dalytranId;
    }

    public void setDalytranId(String dalytranId) {
        this.dalytranId = dalytranId;
    }

    public String getDalytranTypeCd() {
        return dalytranTypeCd;
    }

    public void setDalytranTypeCd(String dalytranTypeCd) {
        this.dalytranTypeCd = dalytranTypeCd;
    }

    public Integer getDalytranCatCd() {
        return dalytranCatCd;
    }

    public void setDalytranCatCd(Integer dalytranCatCd) {
        this.dalytranCatCd = dalytranCatCd;
    }

    public String getDalytranSource() {
        return dalytranSource;
    }

    public void setDalytranSource(String dalytranSource) {
        this.dalytranSource = dalytranSource;
    }

    public String getDalytranDesc() {
        return dalytranDesc;
    }

    public void setDalytranDesc(String dalytranDesc) {
        this.dalytranDesc = dalytranDesc;
    }

    public BigDecimal getDalytranAmt() {
        return dalytranAmt;
    }

    public void setDalytranAmt(BigDecimal dalytranAmt) {
        this.dalytranAmt = dalytranAmt;
    }

    public Long getDalytranMerchantId() {
        return dalytranMerchantId;
    }

    public void setDalytranMerchantId(Long dalytranMerchantId) {
        this.dalytranMerchantId = dalytranMerchantId;
    }

    public String getDalytranMerchantName() {
        return dalytranMerchantName;
    }

    public void setDalytranMerchantName(String dalytranMerchantName) {
        this.dalytranMerchantName = dalytranMerchantName;
    }

    public String getDalytranMerchantCity() {
        return dalytranMerchantCity;
    }

    public void setDalytranMerchantCity(String dalytranMerchantCity) {
        this.dalytranMerchantCity = dalytranMerchantCity;
    }

    public String getDalytranMerchantZip() {
        return dalytranMerchantZip;
    }

    public void setDalytranMerchantZip(String dalytranMerchantZip) {
        this.dalytranMerchantZip = dalytranMerchantZip;
    }

    public String getDalytranCardNum() {
        return dalytranCardNum;
    }

    public void setDalytranCardNum(String dalytranCardNum) {
        this.dalytranCardNum = dalytranCardNum;
    }

    public String getDalytranOrigTs() {
        return dalytranOrigTs;
    }

    public void setDalytranOrigTs(String dalytranOrigTs) {
        this.dalytranOrigTs = dalytranOrigTs;
    }

    public String getDalytranProcTs() {
        return dalytranProcTs;
    }

    public void setDalytranProcTs(String dalytranProcTs) {
        this.dalytranProcTs = dalytranProcTs;
    }

    @Override
    public String toString() {
        return "DailyTransaction{" +
                "dalytranId='" + dalytranId + '\'' +
                ", dalytranTypeCd='" + dalytranTypeCd + '\'' +
                ", dalytranCatCd=" + dalytranCatCd +
                ", dalytranSource='" + dalytranSource + '\'' +
                ", dalytranDesc='" + dalytranDesc + '\'' +
                ", dalytranAmt=" + dalytranAmt +
                ", dalytranMerchantId=" + dalytranMerchantId +
                ", dalytranMerchantName='" + dalytranMerchantName + '\'' +
                ", dalytranMerchantCity='" + dalytranMerchantCity + '\'' +
                ", dalytranMerchantZip='" + dalytranMerchantZip + '\'' +
                ", dalytranCardNum='" + dalytranCardNum + '\'' +
                ", dalytranOrigTs='" + dalytranOrigTs + '\'' +
                ", dalytranProcTs='" + dalytranProcTs + '\'' +
                '}';
    }
}
