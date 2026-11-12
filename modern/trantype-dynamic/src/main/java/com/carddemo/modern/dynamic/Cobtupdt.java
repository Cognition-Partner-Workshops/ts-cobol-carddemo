package com.carddemo.modern.dynamic;

import java.sql.SQLException;
import java.util.Map;
import org.springframework.batch.item.ItemProcessor;

/** 1003-TREAT-RECORD: step-scoped COBTUPDT working storage, including WS-RETURN-MSG. */
public class Cobtupdt implements ItemProcessor<Map<String, Object>, Map<String, Object>> {
    private final RunUnit unit;
    private String message = " ".repeat(80);

    Cobtupdt(RunUnit unit) {
        this.unit = unit;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        unit.sysout.add("PROCESSING   " + record.get("TYPE") + record.get("NUMBER") + record.get("DESC"));
        return record;
    }

    /**
     * 10031-INSERT-DB, 10032-UPDATE-DB, 10033-DELETE-DB: errors continue without rollback.
     * Successful ocesql CALLs reset RETURN-CODE, including an earlier invalid-type RC 4.
     */
    void write(Map<String, Object> record) throws Exception {
        String type = record.get("TYPE").toString();
        if ("*".equals(type)) {
            unit.sysout.add("IGNORING COMMENTED LINE");
            return;
        }
        String verb = switch (type) {
            case "A" -> "ADDING";
            case "U" -> "UPDATING";
            case "D" -> "DELETING";
            default -> null;
        };
        if (verb == null) {
            abend("ERROR: TYPE NOT VALID");
            return;
        }
        unit.sysout.add(verb + " RECORD");
        String sql = switch (type) {
            case "A" -> "INSERT INTO carddemo.transaction_type (tr_type,tr_description) VALUES (?,?)";
            case "U" -> "UPDATE carddemo.transaction_type SET tr_description=? WHERE tr_type=?";
            default -> "DELETE FROM carddemo.transaction_type WHERE tr_type=?";
        };
        try (var statement = unit.connection.prepareStatement(sql)) {
            if ("A".equals(type)) {
                statement.setString(1, record.get("NUMBER").toString());
                statement.setString(2, record.get("DESC").toString());
            } else if ("U".equals(type)) {
                statement.setString(1, record.get("DESC").toString());
                statement.setString(2, record.get("NUMBER").toString());
            } else {
                statement.setString(1, record.get("NUMBER").toString());
            }
            statement.executeUpdate();
            unit.rc = 0;
            // ocesql reports successful zero-row DML as SQLCODE 0, not Db2 +100.
            unit.sysout.add("RECORD " + switch (type) {
                case "A" -> "INSERTED";
                case "U" -> "UPDATED";
                default -> "DELETED";
            } + " SUCCESSFULLY");
        } catch (SQLException error) {
            abend("Error accessing: TRANSACTION_TYPE table. SQLCODE:"
                    + String.format(java.util.Locale.ROOT, "%5d", RunUnit.sqlcode(error)));
        }
    }

    /** 9999-ABEND: overwrites RC 8 with 4; STRING does not clear trailing working storage. */
    void abend(String text) {
        message = text + message.substring(text.length());
        unit.sysout.add(message);
        unit.rc = 4;
    }
}
