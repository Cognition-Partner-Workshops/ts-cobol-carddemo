package com.carddemo.interest;

import com.carddemo.interest.policy.InterestPolicy;
import com.carddemo.interest.policy.InterestPolicy.IdPrefix;
import com.carddemo.interest.policy.InterestPolicy.InactiveAccounts;
import com.carddemo.interest.policy.InterestPolicy.NegativeBalance;
import com.carddemo.interest.policy.InterestPolicy.Rounding;
import com.carddemo.interest.policy.InterestPolicy.TimestampSource;
import com.carddemo.interest.policy.InterestPolicy.ZeroInterest;

/** Test policies: the legacy behaviour with exactly one switch of ticket 04 set to another value. */
public final class PolicySwitches {

    private PolicySwitches() {
    }

    public static InterestPolicy rounding(Rounding value) {
        return new InterestPolicy(value, null, null, false, null, null, null);
    }

    public static InterestPolicy idPrefix(IdPrefix value) {
        return new InterestPolicy(null, value, null, false, null, null, null);
    }

    public static InterestPolicy timestampSource(TimestampSource value) {
        return new InterestPolicy(null, null, value, false, null, null, null);
    }

    public static InterestPolicy updateLastAccount(boolean value) {
        return new InterestPolicy(null, null, null, value, null, null, null);
    }

    public static InterestPolicy negativeBalance(NegativeBalance value) {
        return new InterestPolicy(null, null, null, false, value, null, null);
    }

    public static InterestPolicy zeroInterest(ZeroInterest value) {
        return new InterestPolicy(null, null, null, false, null, value, null);
    }

    public static InterestPolicy inactiveAccounts(InactiveAccounts value) {
        return new InterestPolicy(null, null, null, false, null, null, value);
    }
}
