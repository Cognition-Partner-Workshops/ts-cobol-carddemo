package org.carddemo.modern.trantypedynamic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.carddemo.modern.codec.Layouts;

/**
 * COBTUPDT: apply INPFILE add/update/delete records to TRANSACTION_TYPE.
 *
 * <p>Preserved COBOL defect: 1001-READ-NEXT-RECORDS ends with STOP RUN although CBTRNDYN CALLs this
 * program, so the run unit ends here. CBTRNDYN never displays "CBTRNDYN: END", and its RETURN-CODE 8
 * for a failed dynamic statement is replaced by COBTUPDT's own RETURN-CODE (0 when INPFILE applies cleanly).
 */
final class Cobtupdt implements Subprogram {
    private final Connection unit;
    private final List<String> sysout;
    private final List<Map<String, Object>> records;
    private int returnCode;

    Cobtupdt(Connection unit, List<String> sysout, List<Map<String, Object>> records) {
        this.unit = unit;
        this.sysout = sysout;
        this.records = records;
    }

    @Override
    public int call() throws SQLException {
        returnCode = 0;
        sysout.add("OPEN FILE OK");
        for (var record : records) {
            sysout.add("PROCESSING   " + Layouts.TRANTYPE_UPDATE.encode(record));
            var number = record.get("INPUT-REC-NUMBER").toString();
            // :INPUT-REC-DESC is PIC X(50): the host variable is bound with its trailing spaces.
            var description = record.get("INPUT-REC-DESC").toString();
            switch (record.get("INPUT-REC-TYPE").toString()) {
                case "A" -> {
                    sysout.add("ADDING RECORD");
                    int code = execute("INSERT INTO transaction_type (tr_type, tr_description) VALUES (?, ?)", number, description);
                    if (code == 0) sysout.add("RECORD INSERTED SUCCESSFULLY");
                    else if (code < 0) abend(accessError(code));
                }
                case "U" -> {
                    sysout.add("UPDATING RECORD");
                    int code = execute("UPDATE transaction_type SET tr_description = ? WHERE tr_type = ?", description, number);
                    report(code, "RECORD UPDATED SUCCESSFULLY");
                }
                case "D" -> {
                    sysout.add("DELETING RECORD");
                    int code = execute("DELETE FROM transaction_type WHERE tr_type = ?", number);
                    report(code, "RECORD DELETED SUCCESSFULLY");
                }
                case "*" -> sysout.add("IGNORING COMMENTED LINE");
                default -> abend("ERROR: TYPE NOT VALID");
            }
        }
        throw new StopRun(returnCode);
    }

    private void report(int code, String success) {
        if (code == 0) sysout.add(success);
        else if (code == SqlCodes.NOT_FOUND) abend("No records found.");
        else if (code < 0) abend(accessError(code));
    }

    private static String accessError(int code) {
        return "Error accessing: TRANSACTION_TYPE table. SQLCODE:" + SqlCodes.edited(code);
    }

    /** 9999-ABEND: display WS-RETURN-MSG PIC X(80), set RETURN-CODE 4 and carry on. */
    private void abend(String message) {
        sysout.add(String.format("%-80.80s", message));
        returnCode = 4;
    }

    private int execute(String sql, String... values) throws SQLException {
        var savepoint = unit.setSavepoint();
        try (var statement = unit.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setString(i + 1, values[i]);
            int rows = statement.executeUpdate();
            unit.releaseSavepoint(savepoint);
            return rows == 0 ? SqlCodes.NOT_FOUND : 0;
        } catch (SQLException e) {
            unit.rollback(savepoint);
            return SqlCodes.of(e);
        }
    }
}
