package com.carddemo.interest.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import com.carddemo.interest.domain.HoleDecisions;
import com.carddemo.interest.policy.InterestPolicy.IdPrefix;
import com.carddemo.interest.policy.InterestPolicy.InactiveAccounts;
import com.carddemo.interest.policy.InterestPolicy.MultiCard;
import com.carddemo.interest.policy.InterestPolicy.NegativeBalance;
import com.carddemo.interest.policy.InterestPolicy.Overflow;
import com.carddemo.interest.policy.InterestPolicy.Rounding;
import com.carddemo.interest.policy.InterestPolicy.TimestampSource;
import com.carddemo.interest.policy.InterestPolicy.ZeroInterest;

/** The switch names of architecture.md section 8 (FI-5) as Spring binds them to the policy. */
class InterestPolicyBindingTest {

    private static InterestPolicy bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
                .bindOrCreate("interest.policy", InterestPolicy.class);
    }

    @Test
    void fi5_noPropertyGivesTheLegacyBehaviourOfEverySwitch() {
        assertEquals(InterestPolicy.legacy(), bind(Map.of()));
    }

    @Test
    void fi5_everySwitchBindsByItsName() {
        InterestPolicy policy = bind(Map.of(
                "interest.policy.rounding", "HALF_EVEN",
                "interest.policy.idPrefix", "RUN_DATE",
                "interest.policy.timestampSource", "CYCLE_DATE",
                "interest.policy.updateLastAccount", "true",
                "interest.policy.negativeBalance", "SKIP",
                "interest.policy.zeroInterest", "SKIP",
                "interest.policy.inactiveAccounts", "SKIP"));

        assertEquals(new InterestPolicy(Rounding.HALF_EVEN, IdPrefix.RUN_DATE, TimestampSource.CYCLE_DATE, true,
                NegativeBalance.SKIP, ZeroInterest.SKIP, InactiveAccounts.SKIP), policy);
    }

    @Test
    void intH03_multiCardDefaultIsStop() {
        assertEquals(MultiCard.STOP, bind(Map.of()).multiCard());
        assertEquals(MultiCard.STOP, InterestPolicy.legacy().multiCard());
        assertEquals(HoleDecisions.stop(), HoleDecisions.of(bind(Map.of())));
    }

    @Test
    void intH03_multiCardBindsByItsName() {
        InterestPolicy policy = bind(Map.of("interest.policy.multiCard", "FIRST_BY_CARD_NUM"));

        assertEquals(MultiCard.FIRST_BY_CARD_NUM, policy.multiCard());
        assertEquals(new HoleDecisions(MultiCard.FIRST_BY_CARD_NUM, Overflow.STOP), HoleDecisions.of(policy));
        // no other switch moves
        assertEquals(InterestPolicy.legacy(), new InterestPolicy(policy.rounding(), policy.idPrefix(),
                policy.timestampSource(), policy.updateLastAccount(), policy.negativeBalance(), policy.zeroInterest(),
                policy.inactiveAccounts()));
    }

    @Test
    void intH05_overflowDefaultIsStopAndBindsByItsName() {
        assertEquals(Overflow.STOP, bind(Map.of()).overflow());
        assertEquals(Overflow.STOP, bind(Map.of("interest.policy.overflow", "STOP")).overflow());
    }

    @Test
    void intH05_overflowWidenIsRefused() {
        // WIDEN (architecture.md section 8) is not built: the job does not start with it
        assertThrows(BindException.class, () -> bind(Map.of("interest.policy.overflow", "WIDEN")));
    }
}
