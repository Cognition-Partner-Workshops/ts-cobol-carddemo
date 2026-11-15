package com.carddemo.interest;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.AccountInterest;
import com.carddemo.interest.domain.InterestCalculator;
import com.carddemo.interest.domain.RunLogLines;
import com.carddemo.interest.domain.TransactionCounter;
import com.carddemo.interest.file.FileRunWorkspace;
import com.carddemo.interest.policy.InterestPolicy;

/**
 * Runs the calculation on an input directory through the file adapters, without Spring: read one
 * account, calculate, write, as calcInterestStep does. For the unit tests of ticket 04 that need a
 * policy other than the configured one. PARM and clock are those of the recorded runs.
 */
public final class FileCaseRun {

    /** The harness clock 2022-07-05 10:00:00.00 (tools/runjcl/chains.json:45). */
    public static final Clock FROZEN = Clock.fixed(Instant.parse("2022-07-05T10:00:00Z"), ZoneOffset.UTC);

    private FileCaseRun() {
    }

    /** Runs to a normal end and returns the output directory. */
    public static Path run(Path inputDir, Path outputDir, InterestPolicy policy) {
        FileRunWorkspace workspace = FileRunWorkspace.open(inputDir, outputDir, "unit");
        InterestCalculator calculator = new InterestCalculator(workspace.accountStore(), workspace.cardLookup(),
                workspace.rateLookup(), workspace.runLog(), policy, FROZEN, new TransactionCounter(), Fixtures.PARM_DATE);
        workspace.runLog().line(RunLogLines.START);
        for (Optional<AccountBalances> item = workspace.balanceSource().nextAccount(); item.isPresent();
                item = workspace.balanceSource().nextAccount()) {
            AccountInterest result = calculator.calculate(item.get());
            result.transactions().forEach(workspace.transactionSink()::write);
            result.newBalance().ifPresent(balance -> workspace.accountStore().postInterest(result.accountId(), balance));
        }
        workspace.runLog().line(RunLogLines.END);
        workspace.commit(0);
        return outputDir;
    }
}
