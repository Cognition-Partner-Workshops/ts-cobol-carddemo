package com.carddemo.interest.load;

import java.util.Map;

import com.carddemo.interest.jdbc.ReplicaTable;

/**
 * What one successful load recorded: the interest_run row, the figures of RC1 and RC2, the round
 * trip of RC3, whether the lowest and highest key of each table are the first and last record of
 * its file (RC4) and the profile of TL4.
 */
public record LoadResult(long runId, LoadFigures figures, RoundTrip roundTrip,
        Map<ReplicaTable, Boolean> keyRangeEqualsFileEnds, ExtractProfile profile) {
}
