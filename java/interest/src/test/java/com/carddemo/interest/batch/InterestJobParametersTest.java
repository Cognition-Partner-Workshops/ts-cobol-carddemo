package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;

/** Job parameters of architecture.md section 4. */
class InterestJobParametersTest {

    private static JobParameters parameters(String parmDate, String format) {
        return new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, parmDate)
                .addString(InterestJobParameters.RUN_ID, "run-1")
                .addString(InterestJobParameters.INPUT_DIR, "in")
                .addString(InterestJobParameters.OUTPUT_DIR, "out")
                .addString(InterestJobParameters.FORMAT, format)
                .toJobParameters();
    }

    @Test
    void intQ02_tenCharacterParmAccepted() {
        assertDoesNotThrow(() -> new InterestJobParameters().validate(parameters("2022071800", "fixture")));
        // text, not a date: any ten characters pass
        assertDoesNotThrow(() -> new InterestJobParameters().validate(parameters("CYCLE-07-A", "fixture")));
    }

    @Test
    void intQ02_shortParmRefused() {
        JobParametersInvalidException refused = assertThrows(JobParametersInvalidException.class,
                () -> new InterestJobParameters().validate(parameters("20220718", "fixture")));

        assertTrue(refused.getMessage().contains("INT-Q02"));
    }

    @Test
    void intQ02_longParmRefused() {
        assertThrows(JobParametersInvalidException.class,
                () -> new InterestJobParameters().validate(parameters("20220718000", "fixture")));
    }

    @Test
    void hostFormatRefusedUntilTicket03() {
        assertThrows(JobParametersInvalidException.class,
                () -> new InterestJobParameters().validate(parameters("2022071800", "host")));
    }
}
