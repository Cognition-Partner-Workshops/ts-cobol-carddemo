package com.carddemo.modern.trantype;

import java.util.Map;
import org.carddemo.modern.codec.Layouts;

/** One INPFILE record, WS-INPUT-REC of COBTUPDT ({@link Layouts#TRAN_TYPE_MAINT}). */
public record InputRecord(String raw, String type, String number, String description) {

    public static InputRecord of(String record) {
        Map<String, Object> f = Layouts.TRAN_TYPE_MAINT.decode(record);
        return new InputRecord(Layouts.TRAN_TYPE_MAINT.encode(f), (String) f.get("INPUT-REC-TYPE"),
                (String) f.get("INPUT-REC-NUMBER"), (String) f.get("INPUT-REC-DESC"));
    }
}
