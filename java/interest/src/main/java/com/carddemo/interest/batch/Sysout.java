package com.carddemo.interest.batch;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/** The STEP15 SYSOUT: CBACT04C's DISPLAY lines, which operators read and parity compares. */
@Component
public class Sysout {

    private final List<String> lines = new ArrayList<>();
    private boolean abended;

    public synchronized void display(String line) {
        lines.add(line);
    }

    public synchronized List<String> lines() {
        return List.copyOf(lines);
    }

    synchronized void abend() {
        abended = true;
    }

    public synchronized boolean abended() {
        return abended;
    }
}
