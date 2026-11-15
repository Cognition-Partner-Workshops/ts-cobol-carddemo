package com.carddemo.interest.export;

import java.io.IOException;
import java.io.OutputStream;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.RecordImage;

/**
 * The two formats of the job parameter {@code format} (architecture.md section 4).
 * <ul>
 * <li>{@code fixture}: the recorded format (FI-2) - one record per line, trailing spaces cut,
 * ISO-8859-1, file name {@code <DSN>.txt}.</li>
 * <li>{@code host}: fixed-length records one after the other, no separator, in the host code page,
 * file name {@code <DSN>}. The code page is cp037 with C/D signs as proposed by REL-08 (open, gap G01).</li>
 * </ul>
 * The code page itself stays in the codec ({@link CodePage}, standards S3.7).
 */
public enum ExportFormat {
    FIXTURE("fixture", ".txt"),
    HOST("host", "");

    private final String parameterValue;
    private final String fileSuffix;

    ExportFormat(String parameterValue, String fileSuffix) {
        this.parameterValue = parameterValue;
        this.fileSuffix = fileSuffix;
    }

    /** The format a job parameter names; any other text is refused. */
    public static ExportFormat ofParameter(String value) {
        for (ExportFormat format : values()) {
            if (format.parameterValue.equals(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("format must be fixture or host");
    }

    public String parameterValue() {
        return parameterValue;
    }

    public CodePage codePage() {
        return this == FIXTURE ? CodePage.fixture() : CodePage.host();
    }

    String datasetFile(String datasetName) {
        return datasetName + fileSuffix;
    }

    void write(OutputStream out, byte[] record) throws IOException {
        if (this == FIXTURE) {
            out.write(RecordImage.toLine(record, codePage()).getBytes(codePage().charset()));
            out.write('\n');
        } else {
            out.write(record);
        }
    }
}
