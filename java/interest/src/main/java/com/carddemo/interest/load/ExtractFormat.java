package com.carddemo.interest.load;

import com.carddemo.interest.codec.CodePage;

/**
 * How the unloaded files arrive (job parameter {@code format}). FIXTURE is the recorded format of
 * fixtures/interest (FI-2): one record per line, ISO-8859-1. HOST is the binary unload of strategy
 * item TL2: records of the fixed length one after the other, code page and signs as decision REL-08
 * proposes (cp037, C/D signs - unconfirmed).
 */
public enum ExtractFormat {

    FIXTURE(".txt"),
    HOST("");

    private final String fileSuffix;

    ExtractFormat(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    public static ExtractFormat ofParameter(String value) {
        return valueOf(value.toUpperCase(java.util.Locale.ROOT));
    }

    public CodePage codePage() {
        return this == FIXTURE ? CodePage.fixture() : CodePage.host();
    }

    String fileName(String datasetName) {
        return datasetName + fileSuffix;
    }
}
