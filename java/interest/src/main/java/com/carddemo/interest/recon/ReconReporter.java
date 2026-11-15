package com.carddemo.interest.recon;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalLong;

import org.springframework.stereotype.Component;

import com.carddemo.interest.codec.CardDemoLayouts;
import com.carddemo.interest.load.ExtractFile;
import com.carddemo.interest.load.ExtractFormat;

/**
 * Produces the reconciliation report of a case: reads the unloaded files of the input directory,
 * the run and the tables (read-only), and the exported files of the output directory.
 */
@Component
public class ReconReporter {

    private static final String DATASETS = "datasets";
    private static final String TRANSACTIONS_DATASET = "AWS.M2.CARDDEMO.SYSTRAN.G0001V00";
    private static final String ACCOUNTS_DATASET = "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS";

    private final ReconDatabase database;

    public ReconReporter(ReconDatabase database) {
        this.database = database;
    }

    /**
     * @param runId the interest_run row to report on; empty for the newest one
     */
    public ReconReport report(String title, Path inputDir, Path outputDir, ExtractFormat format, OptionalLong runId) {
        return ReconReport.of(title, unloaded(inputDir, format), database.read(runId, format.codePage()), exported(outputDir, format),
                format.codePage());
    }

    static ReconReport.Unloaded unloaded(Path inputDir, ExtractFormat format) {
        Map<ExtractFile, RecordFiles> files = new EnumMap<>(ExtractFile.class);
        for (ExtractFile file : ExtractFile.values()) {
            files.put(file, RecordFiles.read(inputDir.resolve(file.fileName(format)), file.table().layout(), format));
        }
        return new ReconReport.Unloaded(files);
    }

    static ReconReport.Exported exported(Path outputDir, ExtractFormat format) {
        Path datasets = outputDir.resolve(DATASETS);
        return new ReconReport.Exported(
                RecordFiles.read(datasets.resolve(TRANSACTIONS_DATASET + format.fileSuffix()), CardDemoLayouts.TRANSACTION, format),
                RecordFiles.read(datasets.resolve(ACCOUNTS_DATASET + format.fileSuffix()), CardDemoLayouts.ACCOUNT, format));
    }
}
