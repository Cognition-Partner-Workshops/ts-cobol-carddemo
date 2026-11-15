package com.carddemo.interest.batch;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;

/**
 * Job parameters of interestCalcJob (architecture.md section 4): parmDate, runId, inputDir,
 * outputDir, format.
 */
public final class InterestJobParameters implements JobParametersValidator {

    public static final String PARM_DATE = "parmDate";
    public static final String RUN_ID = "runId";
    public static final String INPUT_DIR = "inputDir";
    public static final String OUTPUT_DIR = "outputDir";
    public static final String FORMAT = "format";

    /** INT-Q02: PARM-DATE is PIC X(10) (CBACT04C:176-178). */
    private static final int PARM_DATE_LENGTH = 10;
    /** The host format is ticket 03. */
    private static final String FIXTURE_FORMAT = "fixture";

    @Override
    public void validate(JobParameters parameters) throws JobParametersInvalidException {
        if (parameters == null) {
            throw new JobParametersInvalidException("job parameters are required");
        }
        for (String name : new String[] {PARM_DATE, RUN_ID, INPUT_DIR, OUTPUT_DIR, FORMAT}) {
            if (parameters.getString(name) == null || parameters.getString(name).isEmpty()) {
                throw new JobParametersInvalidException("job parameter " + name + " is required");
            }
        }
        // INT-Q02: the PARM is text and is never padded or parsed as a date; another length is refused
        // (the legacy reads 10 bytes whatever PARM-LENGTH says, CBACT04C:476).
        if (parameters.getString(PARM_DATE).length() != PARM_DATE_LENGTH) {
            throw new JobParametersInvalidException(
                    "INT-Q02: parmDate must be exactly " + PARM_DATE_LENGTH + " characters");
        }
        if (!FIXTURE_FORMAT.equals(parameters.getString(FORMAT))) {
            throw new JobParametersInvalidException("format must be fixture; the host format is ticket 03");
        }
    }
}
