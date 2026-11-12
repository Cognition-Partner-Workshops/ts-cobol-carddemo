package com.carddemo.modern.posting;

import java.util.List;
import org.springframework.stereotype.Component;

/** Step completion snapshots survive the step-scoped COBOL working storage. */
@Component
public class PostingOutput {
    List<String> postingSysout = List.of();
    List<String> reportSysout = List.of();
    List<String> report = List.of();
    int postingRc;
    int reportRc;
}
