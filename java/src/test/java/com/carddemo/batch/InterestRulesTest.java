package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.carddemo.batch.domain.InterestRules;
import org.junit.jupiter.api.Test;

class InterestRulesTest {

    @Test
    void fractionalCentsAreTruncatedNotRounded() {
        // 421.37 * 15.00 / 1200 = 5.267125
        assertThat(InterestRules.monthlyInterest(new BigDecimal("421.37"), new BigDecimal("15.00"), RoundingMode.DOWN))
                .isEqualByComparingTo("5.26");
        assertThat(InterestRules.monthlyInterest(new BigDecimal("421.37"), new BigDecimal("15.00"), RoundingMode.HALF_UP))
                .isEqualByComparingTo("5.27");
    }

    @Test
    void negativeBalancesTruncateTowardZero() {
        assertThat(InterestRules.monthlyInterest(new BigDecimal("-421.37"), new BigDecimal("15.00"), RoundingMode.DOWN))
                .isEqualByComparingTo("-5.26");
    }

    @Test
    void interestIdsCarryTheJclParmDate() {
        assertThat(InterestRules.interestTransactionId("2022071800", 7)).isEqualTo("2022071800000007");
        assertThat(InterestRules.interestDescription(53)).isEqualTo("Int. for a/c 00000000053");
    }
}
