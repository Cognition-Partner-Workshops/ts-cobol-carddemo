package com.carddemo.interest.load;

import java.util.Map;

/** A finished load: the interest_run row it wrote, the figures per file (RC1-RC4) and the TL4 profile. */
public record LoadReport(long runId, Map<ExtractFile, TableFigures> figures, long g02Differences, long g03Differences,
        ExtractProfile profile) {
}
