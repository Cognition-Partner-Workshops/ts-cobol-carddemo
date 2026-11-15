package com.carddemo.interest.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import com.carddemo.interest.policy.InterestPolicy.IdPrefix;
import com.carddemo.interest.policy.InterestPolicy.InactiveAccounts;
import com.carddemo.interest.policy.InterestPolicy.NegativeBalance;
import com.carddemo.interest.policy.InterestPolicy.MultiCard;
import com.carddemo.interest.policy.InterestPolicy.Overflow;
import com.carddemo.interest.policy.InterestPolicy.Rounding;
import com.carddemo.interest.policy.InterestPolicy.TimestampSource;
import com.carddemo.interest.policy.InterestPolicy.ZeroInterest;

/**
 * The switches of architecture.md section 8 (FI-5): each is bound from interest.policy.* under its
 * frozen name, its default is the legacy behaviour, and its other value decides the other way.
 */
class InterestPolicyTest {

    private static InterestPolicy bound(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
                .bind("interest.policy", InterestPolicy.class)
                .orElseGet(() -> new InterestPolicy(null, null, null, false, null, null, null, null, null));
    }

    @Test
    void fi5_nothingConfiguredIsTheLegacyBehaviour() {
        assertEquals(InterestPolicy.legacy(), bound(Map.of()));
        assertEquals(new InterestPolicy(Rounding.TRUNCATE, IdPrefix.PARM, TimestampSource.RUN_CLOCK, false,
                NegativeBalance.CHARGE, ZeroInterest.WRITE, InactiveAccounts.CHARGE, MultiCard.STOP, Overflow.STOP),
                InterestPolicy.legacy());
    }

    @Test
    void fi5_applicationYmlNamesEverySwitchWithItsLegacyDefault() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));
        PropertySource<?> yml = sources.get(0);

        assertEquals("TRUNCATE", yml.getProperty("interest.policy.rounding"));
        assertEquals("PARM", yml.getProperty("interest.policy.idPrefix"));
        assertEquals("RUN_CLOCK", yml.getProperty("interest.policy.timestampSource"));
        assertEquals(false, yml.getProperty("interest.policy.updateLastAccount"));
        assertEquals("CHARGE", yml.getProperty("interest.policy.negativeBalance"));
        assertEquals("WRITE", yml.getProperty("interest.policy.zeroInterest"));
        assertEquals("CHARGE", yml.getProperty("interest.policy.inactiveAccounts"));
        assertEquals("STOP", yml.getProperty("interest.policy.multiCard"));
        assertEquals("STOP", yml.getProperty("interest.policy.overflow"));
    }

    @Test
    void fi5_everySwitchIsBoundUnderItsFrozenName() {
        InterestPolicy policy = bound(Map.of(
                "interest.policy.rounding", "HALF_EVEN",
                "interest.policy.idPrefix", "RUN_DATE",
                "interest.policy.timestampSource", "CYCLE_DATE",
                "interest.policy.updateLastAccount", "true",
                "interest.policy.negativeBalance", "SKIP",
                "interest.policy.zeroInterest", "SKIP",
                "interest.policy.inactiveAccounts", "SKIP",
                "interest.policy.multiCard", "FIRST_BY_CARD_NUM",
                "interest.policy.overflow", "WIDEN"));

        assertEquals(new InterestPolicy(Rounding.HALF_EVEN, IdPrefix.RUN_DATE, TimestampSource.CYCLE_DATE, true,
                NegativeBalance.SKIP, ZeroInterest.SKIP, InactiveAccounts.SKIP, MultiCard.FIRST_BY_CARD_NUM, Overflow.WIDEN), policy);
    }

    @Test
    void fi5_unknownValueOfASwitchIsRefused() {
        assertThrows(BindException.class, () -> bound(Map.of("interest.policy.negativeBalance", "REFUND")));
    }

    @Test
    void intH03_stopsUnlessSwitchedToTheLowestCardNumber() {
        assertEquals(false, bound(Map.of()).intH03TakesTheLowestCardNumber());
        assertEquals(true, bound(Map.of("interest.policy.multiCard", "FIRST_BY_CARD_NUM")).intH03TakesTheLowestCardNumber());
    }

    @Test
    void intH05_stopsUnlessSwitchedToWiden() {
        assertEquals(true, bound(Map.of()).intH05StopsWhenAValueDoesNotFit());
        assertEquals(false, bound(Map.of("interest.policy.overflow", "WIDEN")).intH05StopsWhenAValueDoesNotFit());
    }

    @Test
    void intQ01_roundingModeOfEachValue() {
        assertEquals(RoundingMode.DOWN, bound(Map.of()).intQ01InterestRoundingMode());
        assertEquals(RoundingMode.HALF_UP, bound(Map.of("interest.policy.rounding", "HALF_UP")).intQ01InterestRoundingMode());
        assertEquals(RoundingMode.HALF_EVEN,
                bound(Map.of("interest.policy.rounding", "HALF_EVEN")).intQ01InterestRoundingMode());
    }

    @Test
    void intQ03_cycleDateOnlyWhenSwitched() {
        assertFalse(bound(Map.of()).intQ03StampsTheCycleDate());
        assertTrue(bound(Map.of("interest.policy.timestampSource", "CYCLE_DATE")).intQ03StampsTheCycleDate());
    }

    @Test
    void intQ04_lastAccountUpdatedOnlyWhenSwitched() {
        assertFalse(bound(Map.of()).intQ04UpdatesAccountEndingInput());
        assertTrue(bound(Map.of("interest.policy.updateLastAccount", "true")).intQ04UpdatesAccountEndingInput());
    }

    @Test
    void intQ05_creditBalanceChargedUnlessSwitched() {
        InterestPolicy skip = bound(Map.of("interest.policy.negativeBalance", "SKIP"));

        assertTrue(bound(Map.of()).intQ05ChargesBalance(new BigDecimal("-0.01")));
        assertFalse(skip.intQ05ChargesBalance(new BigDecimal("-0.01")));
        // a zero or debit balance is not a credit balance
        assertTrue(skip.intQ05ChargesBalance(new BigDecimal("0.00")));
        assertTrue(skip.intQ05ChargesBalance(new BigDecimal("0.01")));
    }

    @Test
    void intQ06_zeroInterestWrittenUnlessSwitched() {
        InterestPolicy skip = bound(Map.of("interest.policy.zeroInterest", "SKIP"));

        assertTrue(bound(Map.of()).intQ06WritesInterest(new BigDecimal("0.00")));
        assertFalse(skip.intQ06WritesInterest(new BigDecimal("0.00")));
        assertTrue(skip.intQ06WritesInterest(new BigDecimal("0.01")));
        assertTrue(skip.intQ06WritesInterest(new BigDecimal("-0.01")));
    }

    @Test
    void intH04_everyAccountChargedUnlessSwitched() {
        InterestPolicy skip = bound(Map.of("interest.policy.inactiveAccounts", "SKIP"));

        assertTrue(bound(Map.of()).intH04ChargesAccount("N"));
        assertTrue(bound(Map.of()).intH04ChargesAccount(""));
        assertTrue(skip.intH04ChargesAccount("Y"));
        // option B of the register: every flag that is not Y is skipped
        assertFalse(skip.intH04ChargesAccount("N"));
        assertFalse(skip.intH04ChargesAccount(""));
    }
}
