package com.carddemo.interest.load;

import java.util.List;

import com.carddemo.interest.codec.DecodedRecord;

/**
 * One unloaded file after decoding: the full-length source records in file order and, for a file
 * without rejects, the decoded record of each.
 */
public record DecodedExtract(ExtractFile file, String fileName, List<byte[]> sources, List<DecodedRecord> records,
        List<Reject> rejects) {
}
