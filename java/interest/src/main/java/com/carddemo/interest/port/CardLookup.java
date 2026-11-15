package com.carddemo.interest.port;

import java.util.List;

/** FI-3. Card numbers by account id, the alternate key of the cross-reference (CBACT04C:34-39, 393-413). */
public interface CardLookup {

    /** Every card number of the account, in card-number order; empty when there is none (INT-H03 for several). */
    List<String> cardNumbersOf(long accountId);
}
