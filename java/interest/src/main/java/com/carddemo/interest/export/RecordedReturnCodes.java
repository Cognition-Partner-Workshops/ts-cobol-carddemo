package com.carddemo.interest.export;

/**
 * rc.json of an exported run in the recorded format (FI-2): {@code json.dumps(indent=1, sort_keys=True)},
 * no final newline (tools/parity/record.py:275).
 *
 * <p>Only STEP15 is a step of INTCALC (app/jcl/INTCALC.jcl:22); a run that is exported ended
 * normally, so its return code is 0 (INT-T03). The recorded file lists five more steps under
 * INTCALC: they are steps of the harness's setup jobs that tools/parity/record.py:273 carries into
 * the first job's entry. They are written as 0 because that is the recorded shape; this job does
 * not run them (pitfall P4; see README.md of this package).
 */
final class RecordedReturnCodes {

    static final String FILE = "rc.json";
    /** INT-T03: GOBACK after a normal run (CBACT04C:232). */
    static final int NORMAL_END = 0;

    private static final String[] STEPS_IN_RECORDED_ORDER = {"STEP05", "STEP10", "STEP15", "STEP20", "STEP25", "STEP30"};
    private static final String INTCALC_STEP = "STEP15";

    private RecordedReturnCodes() {
    }

    static String json(int intcalcReturnCode) {
        StringBuilder steps = new StringBuilder();
        for (int i = 0; i < STEPS_IN_RECORDED_ORDER.length; i++) {
            String step = STEPS_IN_RECORDED_ORDER[i];
            steps.append("   \"").append(step).append("\": ")
                    .append(INTCALC_STEP.equals(step) ? intcalcReturnCode : 0)
                    .append(i < STEPS_IN_RECORDED_ORDER.length - 1 ? ",\n" : "\n");
        }
        return "{\n \"INTCALC\": {\n  \"maxrc\": " + intcalcReturnCode + ",\n  \"steps\": {\n" + steps + "  }\n }\n}";
    }
}
