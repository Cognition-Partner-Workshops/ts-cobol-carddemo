package com.carddemo.interest.codec;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * The one place the code page is named (standards S3.7, data-map rule E3, gap G01).
 * The pilot handles the fixture code page only; the host code page is ticket 03.
 */
public final class FixtureCodePage {

    /** Fixture files are ISO-8859-1, one record per line (tools/parity/record.py:51). */
    public static final Charset CHARSET = StandardCharsets.ISO_8859_1;

    private FixtureCodePage() {
    }
}
