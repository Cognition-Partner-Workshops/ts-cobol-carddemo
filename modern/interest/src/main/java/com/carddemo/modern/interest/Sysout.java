package com.carddemo.modern.interest;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** DISPLAY output of the CBACT04C step (SYSOUT DD), in program order. */
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
