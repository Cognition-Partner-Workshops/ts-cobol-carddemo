package com.carddemo.interest.export;

import com.carddemo.interest.codec.RecordFormat;

/**
 * The two forms of the exported datasets (job parameter {@code format}, architecture.md section 4).
 * FIXTURE is the recorded format the comparator reads (FI-2): one record per line, ISO-8859-1,
 * file name {@code <DSN>.txt}. HOST is what goes back to the mainframe (strategy CO2): fixed-length
 * records with no separator in the host code page, file name {@code <DSN>}; the code page is the
 * unconfirmed default of the codec (REL-08, option A).
 */
public enum ExportFormat {
    FIXTURE(".txt"),
    HOST("");

    private final String fileSuffix;

    ExportFormat(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    /** The value of the job parameter: {@code fixture} or {@code host}. */
    public static ExportFormat of(String parameter) {
        for (ExportFormat format : values()) {
            if (format.name().equalsIgnoreCase(parameter)) {
                return format;
            }
        }
        throw new ExportRefusedException("format must be fixture or host");
    }

    RecordFormat recordFormat() {
        return this == FIXTURE ? RecordFormat.fixture() : RecordFormat.host();
    }

    String fileOf(String datasetName) {
        return datasetName + fileSuffix;
    }
}
