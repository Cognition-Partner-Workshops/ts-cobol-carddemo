package com.carddemo.modern.trantype;

import java.util.List;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/** SYSTSPRT DD SYSOUT=*: writes each record's DISPLAY lines in input order. */
public class SysoutWriter implements ItemWriter<List<String>> {

    private final Sysout sysout;

    public SysoutWriter(Sysout sysout) {
        this.sysout = sysout;
    }

    @Override
    public void write(Chunk<? extends List<String>> chunk) {
        for (List<String> lines : chunk) {
            lines.forEach(sysout::display);
        }
    }
}
