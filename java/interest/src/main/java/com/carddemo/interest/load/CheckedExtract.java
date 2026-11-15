package com.carddemo.interest.load;

import com.carddemo.interest.codec.RecordValues;
import java.util.List;

/**
 * One unloaded file after the boundary check: the decoded rows in file order, each row encoded
 * again (the image the tables must give back, RC3), the round-trip differences counted per gap id
 * (G02 F-signed, G03 minus zero), the fillers that are not blank (TL4) and the rejects.
 */
record CheckedExtract(ExtractFile file, List<RecordValues> rows, List<byte[]> encodedRows, long g02Differences,
        long g03Differences, long nonBlankFillers, List<LoadReject> rejects) {
}
