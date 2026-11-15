package com.carddemo.interest.codec;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * The character set of the recorded fixture files (data-map rule E3, gap G01). It is named here and
 * nowhere else; {@link CodePage#fixture()} is the code page built on it (standards S3.7).
 */
public final class FixtureCodePage {

    /** Fixture files are ISO-8859-1, one record per line (tools/parity/record.py:51). */
    public static final Charset CHARSET = StandardCharsets.ISO_8859_1;

    private FixtureCodePage() {
    }

    /** The bytes of a record image: an image holds one character per byte. */
    public static byte[] bytes(String image) {
        return image.getBytes(CHARSET);
    }

    /** The record image of the bytes, one character per byte. */
    public static String image(byte[] record) {
        return new String(record, CHARSET);
    }
}
