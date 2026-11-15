package com.carddemo.interest.export;

import java.math.BigDecimal;
import java.nio.file.Path;

/** What one export wrote: the figures of reconciliation RC5 as they are in the files. */
public record ExportResult(
        long runId,
        Path outputDir,
        long transactionCount,
        BigDecimal tranAmtSum,
        long accountCount,
        BigDecimal acctCurrBalSum) {
}
