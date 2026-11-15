package com.carddemo.interest.port;

import java.math.BigDecimal;
import java.util.Optional;

/** FI-3. Yearly rate in percent by disclosure group, transaction type and category (INT-R02, CBACT04C:415-420). */
public interface RateLookup {

    Optional<BigDecimal> findRate(String groupId, String typeCode, int categoryCode);
}
