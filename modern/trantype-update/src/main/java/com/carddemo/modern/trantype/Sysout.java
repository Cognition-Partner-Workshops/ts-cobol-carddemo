package com.carddemo.modern.trantype;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** DISPLAY output of MNTTRDB2 STEP1 (SYSTSPRT DD): DSNRUN and COBTUPDT lines, in program order. */
@Component
public class Sysout {

    private final List<String> lines = new ArrayList<>();

    public synchronized void display(String line) {
        lines.add(line);
    }

    public synchronized List<String> lines() {
        return List.copyOf(lines);
    }

    public synchronized void clear() {
        lines.clear();
    }
}
