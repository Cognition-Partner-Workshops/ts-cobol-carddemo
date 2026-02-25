package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Migrated from DISCGRP.PS / Copybook CVTRA02Y (50-byte FB records).
 * COBOL: DIS-GROUP-RECORD
 * Composite key: (DIS-ACCT-GROUP-ID, DIS-TRAN-TYPE-CD, DIS-TRAN-CAT-CD)
 */
@Entity
@Table(name = "disclosure_groups")
@IdClass(DisclosureGroup.DisclosureGroupId.class)
public class DisclosureGroup {

    /** DIS-ACCT-GROUP-ID PIC X(10) */
    @Id
    @Column(name = "dis_acct_group_id", length = 10, nullable = false)
    private String disAcctGroupId;

    /** DIS-TRAN-TYPE-CD PIC X(02) */
    @Id
    @Column(name = "dis_tran_type_cd", length = 2, nullable = false)
    private String disTranTypeCd;

    /** DIS-TRAN-CAT-CD PIC 9(04) */
    @Id
    @Column(name = "dis_tran_cat_cd", nullable = false)
    private Integer disTranCatCd;

    /** DIS-INT-RATE PIC S9(04)V99 */
    @Column(name = "dis_int_rate", precision = 6, scale = 2)
    private BigDecimal disIntRate;

    public DisclosureGroup() {
    }

    public String getDisAcctGroupId() {
        return disAcctGroupId;
    }

    public void setDisAcctGroupId(String disAcctGroupId) {
        this.disAcctGroupId = disAcctGroupId;
    }

    public String getDisTranTypeCd() {
        return disTranTypeCd;
    }

    public void setDisTranTypeCd(String disTranTypeCd) {
        this.disTranTypeCd = disTranTypeCd;
    }

    public Integer getDisTranCatCd() {
        return disTranCatCd;
    }

    public void setDisTranCatCd(Integer disTranCatCd) {
        this.disTranCatCd = disTranCatCd;
    }

    public BigDecimal getDisIntRate() {
        return disIntRate;
    }

    public void setDisIntRate(BigDecimal disIntRate) {
        this.disIntRate = disIntRate;
    }

    /**
     * Composite key class for DisclosureGroup.
     */
    public static class DisclosureGroupId implements Serializable {
        private String disAcctGroupId;
        private String disTranTypeCd;
        private Integer disTranCatCd;

        public DisclosureGroupId() {
        }

        public DisclosureGroupId(String disAcctGroupId, String disTranTypeCd, Integer disTranCatCd) {
            this.disAcctGroupId = disAcctGroupId;
            this.disTranTypeCd = disTranTypeCd;
            this.disTranCatCd = disTranCatCd;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DisclosureGroupId that = (DisclosureGroupId) o;
            return Objects.equals(disAcctGroupId, that.disAcctGroupId) &&
                   Objects.equals(disTranTypeCd, that.disTranTypeCd) &&
                   Objects.equals(disTranCatCd, that.disTranCatCd);
        }

        @Override
        public int hashCode() {
            return Objects.hash(disAcctGroupId, disTranTypeCd, disTranCatCd);
        }
    }
}
