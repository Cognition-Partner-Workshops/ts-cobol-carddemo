package com.aws.carddemo.billing;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "discount_group")
public class DiscountGroup {

    @Id
    @Column(name = "group_cd", length = 10)
    private String groupCd;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    public String getGroupCd() {
        return groupCd;
    }

    public void setGroupCd(String groupCd) {
        this.groupCd = groupCd;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }
}
