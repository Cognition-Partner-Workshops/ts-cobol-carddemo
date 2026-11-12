package com.carddemo.modern.interest;

import org.springframework.stereotype.Component;

/** Set when STEP15 ends in 9999-ABEND-PROGRAM (CEE3ABD), so the runner reports RC 12. */
@Component
public class Step15Abend {

    private volatile boolean abended;

    void set() {
        abended = true;
    }

    void clear() {
        abended = false;
    }

    boolean abended() {
        return abended;
    }
}
