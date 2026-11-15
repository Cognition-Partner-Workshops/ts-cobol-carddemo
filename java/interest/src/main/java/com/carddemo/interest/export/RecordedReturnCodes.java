package com.carddemo.interest.export;

/**
 * rc.json in the recorded shape (FI-2): {@code json.dumps(indent=1, sort_keys=True)}, no final
 * newline (tools/parity/record.py:275).
 *
 * <p>Gap of the ticket: the recorded file lists six steps under INTCALC, and only STEP15 is a step
 * of INTCALC (app/jcl/INTCALC.jcl:22). STEP05, STEP10, STEP20, STEP25 and STEP30 are steps of the
 * harness's setup jobs, carried into the entry of the first job by tools/parity/record.py:273. They
 * are written as 0, which is what a recorded run that reached INTCALC holds; this job runs none of
 * them. What the target should write outside the harness is open (modernization/design/skills.md).
 */
final class RecordedReturnCodes {

    static final String FILE = "rc.json";

    private static final String[] HARNESS_SETUP_STEPS_BEFORE = {"STEP05", "STEP10"};
    private static final String[] HARNESS_SETUP_STEPS_AFTER = {"STEP20", "STEP25", "STEP30"};

    private RecordedReturnCodes() {
    }

    static String json(int step15ReturnCode) {
        StringBuilder steps = new StringBuilder();
        for (String step : HARNESS_SETUP_STEPS_BEFORE) {
            steps.append("   \"").append(step).append("\": 0,\n");
        }
        steps.append("   \"STEP15\": ").append(step15ReturnCode);
        for (String step : HARNESS_SETUP_STEPS_AFTER) {
            steps.append(",\n   \"").append(step).append("\": 0");
        }
        return "{\n \"INTCALC\": {\n  \"maxrc\": " + step15ReturnCode + ",\n  \"steps\": {\n" + steps + "\n  }\n }\n}";
    }
}
