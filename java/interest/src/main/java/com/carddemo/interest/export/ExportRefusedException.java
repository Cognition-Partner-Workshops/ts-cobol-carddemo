package com.carddemo.interest.export;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * The export does not take place: the run is not CALCULATED, or what would be written does not
 * agree with the figures recorded after the calculation (strategy item RC5). It is the one stop
 * exception of the stream (standards S6.2) with the reason of an output that cannot be written;
 * the message holds the run id, a status and counts, never account data (S6.5).
 */
public final class ExportRefusedException extends InterestRunStoppedException {

    private static final long serialVersionUID = 1L;

    ExportRefusedException(String detail) {
        super(StopReason.WRITE_OUTPUT, "export refused: " + detail);
    }
}
