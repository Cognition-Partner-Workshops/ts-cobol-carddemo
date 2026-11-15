package com.carddemo.interest.port;

import com.carddemo.interest.domain.InterestTransaction;

/** FI-3. The interest transactions of the run, in the order written (CBACT04C:500). */
public interface TransactionSink {

    void write(InterestTransaction transaction);
}
