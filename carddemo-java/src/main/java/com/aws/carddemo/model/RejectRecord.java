package com.aws.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Reject Record entity - used for storing rejected daily transactions
 * Based on the DALYREJS file structure in CBTRN02C.cbl
 * Record length: 430 bytes (350 transaction + 80 validation trailer)
 */
@Entity
@Table(name = "DALYREJS")
public class RejectRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "REJECT_TRAN_DATA", length = 350)
    private String rejectTranData;

    @Column(name = "VALIDATION_FAIL_REASON")
    private Integer validationFailReason;

    @Column(name = "VALIDATION_FAIL_REASON_DESC", length = 76)
    private String validationFailReasonDesc;

    public RejectRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRejectTranData() {
        return rejectTranData;
    }

    public void setRejectTranData(String rejectTranData) {
        this.rejectTranData = rejectTranData;
    }

    public Integer getValidationFailReason() {
        return validationFailReason;
    }

    public void setValidationFailReason(Integer validationFailReason) {
        this.validationFailReason = validationFailReason;
    }

    public String getValidationFailReasonDesc() {
        return validationFailReasonDesc;
    }

    public void setValidationFailReasonDesc(String validationFailReasonDesc) {
        this.validationFailReasonDesc = validationFailReasonDesc;
    }

    @Override
    public String toString() {
        return "RejectRecord{" +
                "id=" + id +
                ", validationFailReason=" + validationFailReason +
                ", validationFailReasonDesc='" + validationFailReasonDesc + '\'' +
                '}';
    }
}
