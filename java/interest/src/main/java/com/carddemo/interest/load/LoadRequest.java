package com.carddemo.interest.load;

import java.nio.file.Path;

/** What one load needs: the PARM text of the run, where the four files are, their format, where the reject list goes. */
public record LoadRequest(String parmDate, Path inputDir, ExtractFormat format, Path rejectFile) {
}
