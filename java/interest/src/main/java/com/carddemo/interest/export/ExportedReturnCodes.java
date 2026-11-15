package com.carddemo.interest.export;

/**
 * rc.json in the recorded format (FI-2): {@code json.dumps(indent=1, sort_keys=True)}, no final newline.
 *
 * <p>Gap of INT-T07: the recorded file lists six steps under INTCALC, of which only STEP15 is a step
 * of INTCALC (app/jcl/INTCALC.jcl). STEP05, STEP10, STEP20, STEP25 and STEP30 are steps of the
 * harness's setup jobs that the recorder carries into the first job's entry. The export mirrors that
 * shape and writes them as 0; the Java job does not run them.
 */
final class ExportedReturnCodes {

    private static final String[] STEPS = {"STEP05", "STEP10", "STEP15", "STEP20", "STEP25", "STEP30"};
    private static final String INTCALC_STEP = "STEP15";

    private ExportedReturnCodes() {
    }

    static String json(int step15ReturnCode) {
        StringBuilder steps = new StringBuilder();
        for (int i = 0; i < STEPS.length; i++) {
            steps.append("   \"").append(STEPS[i]).append("\": ")
                    .append(STEPS[i].equals(INTCALC_STEP) ? step15ReturnCode : 0)
                    .append(i < STEPS.length - 1 ? ",\n" : "\n");
        }
        return "{\n \"INTCALC\": {\n  \"maxrc\": " + step15ReturnCode + ",\n  \"steps\": {\n" + steps + "  }\n }\n}";
    }
}
