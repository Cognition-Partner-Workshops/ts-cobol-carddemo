package com.carddemo.batch;

import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * One run of the batch path.
 *
 * @param inputDir   decoded inputs written by the shared loader (spec/harness)
 * @param outputDir  where the canonical outputs go, one JSONL file per dataset
 * @param jobs       batch path jobs in run order, POSTTRAN then INTCALC as in the JCL
 * @param runTimestamp processing timestamp, in the DB2 format the programs write
 * @param interestParmDate the INTCALC.jcl PARM value, prefix of interest transaction ids
 * @param interestRounding DOWN is the COBOL truncation; anything else is a control run
 */
@ConfigurationProperties(prefix = "carddemo")
public record RunProperties(
        Path inputDir,
        Path outputDir,
        @DefaultValue({"POSTTRAN", "INTCALC"}) List<String> jobs,
        @DefaultValue("2022-07-18-01.00.00.000000") String runTimestamp,
        @DefaultValue("2022071800") String interestParmDate,
        @DefaultValue("DOWN") RoundingMode interestRounding) {
}
