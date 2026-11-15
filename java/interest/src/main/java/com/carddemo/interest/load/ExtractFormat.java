package com.carddemo.interest.load;

import com.carddemo.interest.codec.CodePage;

/** The two formats of an unloaded file (job parameter {@code format}). */
public enum ExtractFormat {
    /** FI-2: one record per line, ISO-8859-1, trailing spaces removed; files end in .txt. */
    FIXTURE("fixture", ".txt"),
    /** TL2: fixed-length records without separator in the host code page (REL-08: cp037, proposed). */
    HOST("host", "");

    private final String parameterValue;
    private final String fileSuffix;

    ExtractFormat(String parameterValue, String fileSuffix) {
        this.parameterValue = parameterValue;
        this.fileSuffix = fileSuffix;
    }

    public static ExtractFormat of(String parameterValue) {
        for (ExtractFormat format : values()) {
            if (format.parameterValue.equals(parameterValue)) {
                return format;
            }
        }
        throw new IllegalArgumentException("format must be fixture or host");
    }

    public String parameterValue() {
        return parameterValue;
    }

    public String fileSuffix() {
        return fileSuffix;
    }

    public CodePage codePage() {
        return this == FIXTURE ? CodePage.FIXTURE : CodePage.host();
    }
}
