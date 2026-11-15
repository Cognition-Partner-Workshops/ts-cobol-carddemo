package com.carddemo.interest.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import com.carddemo.interest.policy.InterestPolicy.IdPrefix;
import com.carddemo.interest.policy.InterestPolicy.InactiveAccounts;
import com.carddemo.interest.policy.InterestPolicy.NegativeBalance;
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
}
