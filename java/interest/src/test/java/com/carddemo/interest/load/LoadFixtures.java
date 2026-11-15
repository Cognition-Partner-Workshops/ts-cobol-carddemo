package com.carddemo.interest.load;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import com.carddemo.interest.Fixtures;

/** The seven cases with the figures of the reconciliation table of strategy-interest.md, typed by hand (standards S7.4). */
final class LoadFixtures {

    private LoadFixtures() {
    }

    /** case, accounts, cards, balances, rates, SUM(acct_curr_bal), SUM(tran_cat_bal), SUM(dis_int_rate). */
    static Stream<Arguments> reconciliationTable() {
        return Stream.of(
                Arguments.of("i01-baseline", 2L, 2L, 2L, 15L, "3400.00", "3400.00", "120.50"),
                Arguments.of("i02-tie-half-cent", 2L, 2L, 2L, 15L, "2470.00", "2470.00", "120.50"),
                Arguments.of("i03-zero-rate-skipped", 1L, 1L, 2L, 15L, "700.00", "700.00", "120.50"),
                Arguments.of("i04-default-group-fallback", 1L, 1L, 1L, 15L, "1000.00", "1000.00", "120.50"),
                Arguments.of("i05-multi-category", 1L, 1L, 2L, 15L, "1500.00", "1500.00", "120.50"),
                Arguments.of("i06-negative-balance", 1L, 1L, 1L, 15L, "-300.00", "-300.00", "120.50"),
                Arguments.of("i07-nontie-rounding", 2L, 2L, 2L, 15L, "1778.10", "1778.10", "120.50"));
    }

    static BigDecimal amount(String text) {
        return new BigDecimal(text);
    }

    static byte[] content(String caseName, ExtractFile file) {
        try {
            return Files.readAllBytes(Fixtures.input(caseName).resolve(file.fileName()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A copy of the four input files of a case in a new directory, one record of one file changed. */
    static Path copyWith(String caseName, Path directory, ExtractFile changed, int recordNumber, UnaryOperator<String> change) {
        try {
            Files.createDirectories(directory);
            for (ExtractFile file : ExtractFile.values()) {
                List<String> lines = new ArrayList<>(Fixtures.lines(Fixtures.input(caseName).resolve(file.fileName())));
                if (file == changed) {
                    lines.set(recordNumber - 1, change.apply(lines.get(recordNumber - 1)));
                }
                Files.write(directory.resolve(file.fileName()), lines, StandardCharsets.ISO_8859_1);
            }
            return directory;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The record with the character at a position (from 0) replaced. */
    static UnaryOperator<String> charAt(int position, char replacement) {
        return line -> line.substring(0, position) + replacement + line.substring(position + 1);
    }
}
