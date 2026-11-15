package com.carddemo.interest.file;

/**
 * rc.json in the recorded format (FI-2): {@code json.dumps(indent=1, sort_keys=True)}, no final
 * newline (tools/parity/record.py:275).
 *
 * <p>The recorded file lists six steps under INTCALC. Only STEP15 is a step of INTCALC
 * (app/jcl/INTCALC.jcl:22). The other five are steps of the harness's setup jobs that
 * tools/parity/record.py:273 carries over into the first job's entry. They are written as 0, which
 * is what a recorded run that reached INTCALC holds; this job does not run them.
 */
final class RecordedReturnCodes {

    private static final String[] HARNESS_SETUP_STEPS_BEFORE = {"STEP05", "STEP10"};
    private static final String[] HARNESS_SETUP_STEPS_AFTER = {"STEP20", "STEP25", "STEP30"};

    private RecordedReturnCodes() {
    }

    static String json(int step15ReturnCode) {
        StringBuilder steps = new StringBuilder();
        for (String step : HARNESS_SETUP_STEPS_BEFORE) {
            steps.append("   \"").append(step).append("\": 0,\n");
        }
        steps.append("   \"STEP15\": ").append(step15ReturnCode).append(",\n");
        for (int i = 0; i < HARNESS_SETUP_STEPS_AFTER.length; i++) {
            steps.append("   \"").append(HARNESS_SETUP_STEPS_AFTER[i]).append("\": 0")
                    .append(i < HARNESS_SETUP_STEPS_AFTER.length - 1 ? ",\n" : "\n");
        }
        return "{\n \"INTCALC\": {\n  \"maxrc\": " + step15ReturnCode + ",\n  \"steps\": {\n" + steps + "  }\n }\n}";
    }
}
