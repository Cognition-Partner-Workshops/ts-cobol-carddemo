package com.carddemo.interest.recon;

import java.util.Optional;

/** One finding of the trial-load profile (strategy item TL4): what was counted, the gap or rule that asks for it, the count. */
public record ProfileLine(String finding, String asksFor, Optional<Long> count) {

    public String countText() {
        return count.map(String::valueOf).orElse(ReconLine.MISSING);
    }
}
