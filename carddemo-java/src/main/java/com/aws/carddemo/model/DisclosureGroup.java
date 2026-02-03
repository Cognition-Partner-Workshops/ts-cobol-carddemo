package com.aws.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Disclosure Group entity - migrated from COBOL copybook CVTRA02Y.cpy
 * Original COBOL record length: 50 bytes
 * Defines interest rates by account group, transaction type, and category
 */
@Entity
@Table(name = "DISCGRP")
public class DisclosureGroup {

    @EmbeddedId
    private DisclosureGroupKey id;

    @Column(name = "DIS_INT_RATE", precision = 6, scale = 2)
    private BigDecimal disIntRate;

    public DisclosureGroup() {
    }

    public DisclosureGroup(DisclosureGroupKey id) {
        this.id = id;
    }

    public DisclosureGroupKey getId() {
        return id;
    }

    public void setId(DisclosureGroupKey id) {
        this.id = id;
    }

    public BigDecimal getDisIntRate() {
        return disIntRate;
    }

    public void setDisIntRate(BigDecimal disIntRate) {
        this.disIntRate = disIntRate;
    }

    @Override
    public String toString() {
        return "DisclosureGroup{" +
                "id=" + id +
                ", disIntRate=" + disIntRate +
                '}';
    }

    @Embeddable
    public static class DisclosureGroupKey implements Serializable {

        @Column(name = "DIS_ACCT_GROUP_ID", length = 10)
        private String disAcctGroupId;

        @Column(name = "DIS_TRAN_TYPE_CD", length = 2)
        private String disTranTypeCd;

        @Column(name = "DIS_TRAN_CAT_CD")
        private Integer disTranCatCd;

        public DisclosureGroupKey() {
        }

        public DisclosureGroupKey(String disAcctGroupId, String disTranTypeCd, Integer disTranCatCd) {
            this.disAcctGroupId = disAcctGroupId;
            this.disTranTypeCd = disTranTypeCd;
            this.disTranCatCd = disTranCatCd;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DisclosureGroupKey that = (DisclosureGroupKey) o;
            return Objects.equals(disAcctGroupId, that.disAcctGroupId) &&
                    Objects.equals(disTranTypeCd, that.disTranTypeCd) &&
                    Objects.equals(disTranCatCd, that.disTranCatCd);
        }

        @Override
        public int hashCode() {
            return Objects.hash(disAcctGroupId, disTranTypeCd, disTranCatCd);
        }

        @Override
        public String toString() {
            return "DisclosureGroupKey{" +
                    "disAcctGroupId='" + disAcctGroupId + '\'' +
                    ", disTranTypeCd='" + disTranTypeCd + '\'' +
                    ", disTranCatCd=" + disTranCatCd +
                    '}';
        }
    }
}
