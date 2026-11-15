package com.carddemo.interest.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/** The file adapters of the ports (FI-3) on fixture inputs and on synthetic files. */
class FileRunWorkspaceTest {

    @TempDir
    Path temp;

    private Path syntheticInput(List<String> balances) throws IOException {
        Path input = temp.resolve("input");
        Files.createDirectories(input);
        for (String name : List.of(FileRunWorkspace.ACCOUNTS_FILE, FileRunWorkspace.CARDS_FILE, FileRunWorkspace.RATES_FILE)) {
            Files.copy(Fixtures.input("i01-baseline").resolve(name), input.resolve(name));
        }
        Files.write(input.resolve(FileRunWorkspace.BALANCES_FILE), balances, StandardCharsets.ISO_8859_1);
        return input;
    }

    @Test
    void intR01_readsInKeyOrder() throws IOException {
        // synthetic, out of key order on the file: account, then type code, then category decide the order
        Path input = syntheticInput(List.of(
                "000000020020100010000024000{",
                "000000020010200010000000500{",
                "000000020010100020000005000{",
                "000000020010100010000010000{"));
        FileRunWorkspace workspace = FileRunWorkspace.open(input, temp.resolve("actual"), "t1");

        AccountBalances first = workspace.balanceSource().nextAccount().orElseThrow();
        AccountBalances second = workspace.balanceSource().nextAccount().orElseThrow();

        assertEquals(2001L, first.accountId());
        assertEquals(List.of("01/1", "01/2", "02/1"), first.balances().stream()
                .map(read -> read.balance().typeCode() + "/" + read.balance().categoryCode()).toList());
        assertEquals(2002L, second.accountId());
        assertEquals(Optional.empty(), workspace.balanceSource().nextAccount());
    }

    @Test
    void intR01_balancesOfOneAccountAreOneItem() {
        // i05-multi-category: account 2007 has two balances and is one item
        FileRunWorkspace workspace = FileRunWorkspace.open(Fixtures.input("i05-multi-category"), temp.resolve("actual"), "t2");

        AccountBalances first = workspace.balanceSource().nextAccount().orElseThrow();

        assertEquals(2007L, first.accountId());
        assertEquals(2, first.balances().size());
        // the echo is the 50-byte record image, trailing spaces included (CBACT04C:193)
        assertEquals("000000020070100010000010000{" + " ".repeat(22), first.balances().get(0).recordImage());
    }

    @Test
    void intQ04_onlyTheAccountEndingTheInputIsMarked() {
        // i01-baseline: accounts 2001 and 2002; 2002 ends the input
        FileRunWorkspace workspace = FileRunWorkspace.open(Fixtures.input("i01-baseline"), temp.resolve("actual"), "t3");

        assertFalse(workspace.balanceSource().nextAccount().orElseThrow().endsInput());
        assertTrue(workspace.balanceSource().nextAccount().orElseThrow().endsInput());
    }

    @Test
    void intT09_emptyInputGivesNoItem() throws IOException {
        FileRunWorkspace workspace = FileRunWorkspace.open(syntheticInput(List.of()), temp.resolve("actual"), "t4");

        assertEquals(Optional.empty(), workspace.balanceSource().nextAccount());
    }

    @Test
    void intR02_rateLookupByGroupTypeCategory() {
        // i01-baseline DISCGRP: GOLD/01/0001 = 12.00, STANDARD/01/0001 = 18.50, GOLD/01/0002 = 18.00; no SILVER
        FileRunWorkspace workspace = FileRunWorkspace.open(Fixtures.input("i01-baseline"), temp.resolve("actual"), "t5");

        assertEquals(Optional.of(new BigDecimal("12.00")), workspace.rateLookup().findRate("GOLD", "01", 1));
        assertEquals(Optional.of(new BigDecimal("18.50")), workspace.rateLookup().findRate("STANDARD", "01", 1));
        assertEquals(Optional.of(new BigDecimal("18.00")), workspace.rateLookup().findRate("GOLD", "01", 2));
        assertEquals(Optional.empty(), workspace.rateLookup().findRate("SILVER", "01", 1));
    }

    @Test
    void intR06_cardLookupByAccount() {
        FileRunWorkspace workspace = FileRunWorkspace.open(Fixtures.input("i01-baseline"), temp.resolve("actual"), "t6");

        assertEquals(List.of("4000000000002001"), workspace.cardLookup().cardNumbersOf(2001L));
        assertEquals(List.of(), workspace.cardLookup().cardNumbersOf(9999L));
    }

    @Test
    void intR08_missingInputFileStopsBeforeAnyOutput() throws IOException {
        Path input = syntheticInput(List.of("000000020010100010000010000{"));
        Files.delete(input.resolve(FileRunWorkspace.RATES_FILE));

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> FileRunWorkspace.open(input, temp.resolve("actual"), "t7"));

        assertEquals(StopReason.OPEN_INPUT, stop.reason());
        try (var entries = Files.list(temp)) {
            assertEquals(List.of("input"), entries.map(path -> path.getFileName().toString()).toList());
        }
    }

    @Test
    void e1_badDigitInABalanceRejectsTheRecord() throws IOException {
        FileRunWorkspace workspace = FileRunWorkspace.open(
                syntheticInput(List.of("000000020010100010000X10000{")), temp.resolve("actual"), "t8");

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> workspace.balanceSource().nextAccount());

        assertEquals(StopReason.RECORD_REJECTED, stop.reason());
    }

    @Test
    void intR08_discardedRunLeavesNothing() throws IOException {
        Path output = temp.resolve("actual");
        FileRunWorkspace workspace = FileRunWorkspace.open(Fixtures.input("i01-baseline"), output, "t9");
        workspace.runLog().line("START");

        workspace.discard();

        assertFalse(Files.exists(output));
        try (var entries = Files.list(temp)) {
            assertEquals(0, entries.count());
        }
    }

    @Test
    void rcJson_recordedFormat() {
        // the recorded rc.json of every interest case holds these bytes (FI-2)
        assertEquals(Fixtures.text(Fixtures.expected("i01-baseline").resolve("rc.json")), RecordedReturnCodes.json(0));
    }
}
