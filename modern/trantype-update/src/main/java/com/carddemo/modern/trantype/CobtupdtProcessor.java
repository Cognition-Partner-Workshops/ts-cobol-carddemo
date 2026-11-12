package com.carddemo.modern.trantype;

import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;

/**
 * COBTUPDT (app/app-transaction-type-db2/cbl/COBTUPDT.cbl) per input record: the DISPLAY of
 * 1002-READ-RECORDS and 1003-TREAT-RECORD with its SQL paragraphs. Step-scoped; holds the
 * program's working storage that survives between records: WS-RETURN-MSG and RETURN-CODE.
 * Returns the DISPLAY lines of the record, in order, for the SYSTSPRT writer.
 *
 * <p>The SQL runs on the task's {@link Db2Attach} unit of work, not in the chunk transaction:
 * on z/OS nothing is committed until the end of the task.
 */
public class CobtupdtProcessor implements ItemProcessor<InputRecord, List<String>>, StepExecutionListener {

    static final String RETURN_CODE_KEY = "RETURN-CODE";

    private final TransactionTypeSql sql;
    /** 05 WS-RETURN-MSG PIC X(80) VALUE SPACES; never re-initialised between records. */
    private final char[] wsReturnMsg = " ".repeat(80).toCharArray();
    private int returnCode;

    public CobtupdtProcessor(TransactionTypeSql sql) {
        this.sql = sql;
    }

    @Override
    public List<String> process(InputRecord record) {
        List<String> display = new ArrayList<>();
        // 1002-READ-RECORDS: DISPLAY 'PROCESSING   ' WS-INPUT-REC
        display.add("PROCESSING   " + record.raw());
        treatRecord(record, display);
        return display;
    }

    /** 1003-TREAT-RECORD: EVALUATE INPUT-REC-TYPE. */
    private void treatRecord(InputRecord record, List<String> display) {
        switch (record.type()) {
            case "A" -> {
                display.add("ADDING RECORD");
                insertDb(record, display);
            }
            case "U" -> {
                display.add("UPDATING RECORD");
                updateDb(record, display);
            }
            case "D" -> {
                display.add("DELETING RECORD");
                deleteDb(record, display);
            }
            case "*" -> display.add("IGNORING COMMENTED LINE");
            default -> {
                CobolText.stringInto(wsReturnMsg, "ERROR: TYPE NOT VALID");
                abend(display);
            }
        }
    }

    /** 10031-INSERT-DB. */
    private void insertDb(InputRecord record, List<String> display) {
        int sqlcode = execSql(sql.insert(record.number(), record.description()));
        if (sqlcode == Sqlca.OK) {
            display.add("RECORD INSERTED SUCCESSFULLY");
        } else if (sqlcode < 0) {
            accessError(sqlcode, display);
        }
    }

    /** 10032-UPDATE-DB. The +100 branch is unreachable off-host: see {@link Sqlca}. */
    private void updateDb(InputRecord record, List<String> display) {
        int sqlcode = execSql(sql.update(record.number(), record.description()));
        if (sqlcode == Sqlca.OK) {
            display.add("RECORD UPDATED SUCCESSFULLY");
        } else if (sqlcode == Sqlca.NOT_FOUND) {
            CobolText.stringInto(wsReturnMsg, "No records found.");
            abend(display);
        } else if (sqlcode < 0) {
            accessError(sqlcode, display);
        }
    }

    /**
     * 10033-DELETE-DB. Legacy quirk kept (fixture t05-delete-missing): deleting a TR_TYPE that does
     * not exist is reported as RECORD DELETED SUCCESSFULLY with RC 0. The legacy run gets SQLCODE 0
     * for a zero-row DELETE ({@link Sqlca}), so the +100 "No records found." branch never fires.
     */
    private void deleteDb(InputRecord record, List<String> display) {
        int sqlcode = execSql(sql.delete(record.number()));
        if (sqlcode == Sqlca.OK) {
            display.add("RECORD DELETED SUCCESSFULLY");
        } else if (sqlcode == Sqlca.NOT_FOUND) {
            CobolText.stringInto(wsReturnMsg, "No records found.");
            abend(display);
        } else if (sqlcode < 0) {
            accessError(sqlcode, display);
        }
    }

    /**
     * EXEC SQL ... END-EXEC as Open COBOL ESQL expands it: the statement ends with
     * {@code CALL "OCESQLEndSQL"}, which returns 0, and a CALL stores its result in RETURN-CODE.
     * Legacy quirk kept: an RC 4 set by 9999-ABEND for an earlier record is reset to 0 by the
     * next SQL statement, so the step RC reflects only the records after the last SQL statement.
     */
    private int execSql(int sqlcode) {
        returnCode = 0;
        return sqlcode;
    }

    private void accessError(int sqlcode, List<String> display) {
        CobolText.stringInto(wsReturnMsg, "Error accessing:", " TRANSACTION_TYPE table. SQLCODE:",
                CobolText.minusMinus9(sqlcode));
        abend(display);
    }

    /** 9999-ABEND: DISPLAY WS-RETURN-MSG, MOVE 4 TO RETURN-CODE, and carry on with the next record. */
    private void abend(List<String> display) {
        display.add(new String(wsReturnMsg));
        returnCode = 4;
    }

    int returnCode() {
        return returnCode;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        stepExecution.getExecutionContext().putInt(RETURN_CODE_KEY, returnCode);
        return stepExecution.getExitStatus();
    }
}
