package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.carddemo.batch.domain.CategoryBalance;
import com.carddemo.batch.domain.InterestRules;
import com.carddemo.batch.domain.PurchasePromotion;
import org.junit.jupiter.api.Test;

class PurchasePromotionTest {

    private static final BigDecimal ORDINARY_RATE = new BigDecimal("15.00");

    @Test
    void promotionalPurchaseRateAppliesOnlyToFirstThreeCycles() {
        CategoryBalance purchase = new CategoryBalance(51, "01", 1, new BigDecimal("1491.82"));
        for (int cycle = 1; cycle <= PurchasePromotion.CYCLES; cycle++) {
            assertThat(PurchasePromotion.rate(PurchasePromotion.GROUP, purchase, cycle, ORDINARY_RATE))
                    .isEqualByComparingTo("4.99");
        }
        for (int cycle : List.of(0, 4, 5)) {
            assertThat(PurchasePromotion.rate(PurchasePromotion.GROUP, purchase, cycle, ORDINARY_RATE))
                    .isEqualByComparingTo("15.00");
        }
    }

    @Test
    void otherCategoriesAndGroupsUseTheOrdinaryRate() {
        for (CategoryBalance category : List.of(
                new CategoryBalance(51, "01", 2, BigDecimal.ZERO),
                new CategoryBalance(51, "05", 1, BigDecimal.ZERO),
                new CategoryBalance(51, "07", 1, BigDecimal.ZERO))) {
            assertThat(PurchasePromotion.rate(PurchasePromotion.GROUP, category, 1, ORDINARY_RATE))
                    .isEqualByComparingTo("15.00");
        }
        for (String group : List.of("", "DEFAULT", "A000000000")) {
            assertThat(PurchasePromotion.rate(group, new CategoryBalance(51, "01", 1, BigDecimal.ZERO),
                    1, ORDINARY_RATE)).isEqualByComparingTo("15.00");
        }
        assertThat(PurchasePromotion.rate(null, new CategoryBalance(51, "01", 1, BigDecimal.ZERO),
                1, ORDINARY_RATE)).isEqualByComparingTo("15.00");
    }

    @Test
    void membershipAcceptsTrailingSpacePadding() {
        assertThat(PurchasePromotion.isMember("GOLDPROMO1   ")).isTrue();
    }

    @Test
    void monthlyInterestKeepsCobolTruncation() {
        BigDecimal balance = new BigDecimal("1491.82");
        assertThat(InterestRules.monthlyInterest(balance, new BigDecimal("4.99"), RoundingMode.DOWN))
                .isEqualByComparingTo("6.20");
        assertThat(InterestRules.monthlyInterest(balance, ORDINARY_RATE, RoundingMode.DOWN))
                .isEqualByComparingTo("18.64");
    }
}
