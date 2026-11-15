package com.carddemo.interest.load;

import java.util.List;

import com.carddemo.interest.codec.RecordValues;

/**
 * One unloaded file, decoded: its rows in file order, its figures (RC1, RC2), and how many amounts
 * come back with another sign byte when encoded again (RC3: G02 F-signed, G03 minus zero).
 */
public record DecodedExtract(ExtractFile file, List<RecordValues> rows, LoadFigures figures, long fSignedAmounts,
        long minusZeroAmounts) {

    public DecodedExtract {
        rows = List.copyOf(rows);
    }
}
