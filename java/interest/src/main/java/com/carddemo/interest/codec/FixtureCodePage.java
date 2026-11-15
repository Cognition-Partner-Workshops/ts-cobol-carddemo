package com.carddemo.interest.codec;

import java.nio.charset.Charset;

/** The charset of the recorded fixture files (FI-2); the code page itself is {@link CodePage#FIXTURE}. */
public final class FixtureCodePage {

    public static final Charset CHARSET = CodePage.FIXTURE.charset();

    private FixtureCodePage() {
    }
}
