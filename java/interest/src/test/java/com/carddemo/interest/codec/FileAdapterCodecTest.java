package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.file.FileRunWorkspace;

/** The file adapters call the codec: a rejected input record names file, record number and field. */
class FileAdapterCodecTest {

    private static final String BALANCES = "AWS.M2.CARDDEMO.TCATBALF.PS.txt";
    private static final String ACCOUNTS = "AWS.M2.CARDDEMO.ACCTDATA.PS.txt";
    private static final String CARDS = "AWS.M2.CARDDEMO.CARDXREF.PS.txt";
    private static final String RATES = "AWS.M2.CARDDEMO.DISCGRP.PS.txt";

    @TempDir
    Path temp;

    /** The input of i01-baseline with the lines of one file replaced. */
    private Path inputWith(String fileName, List<String> lines) throws IOException {
        Path input = temp.resolve("input");
        Files.createDirectories(input);
        for (String name : List.of(BALANCES, ACCOUNTS, CARDS, RATES)) {
            Files.copy(Fixtures.input("i01-baseline").resolve(name), input.resolve(name));
        }
        Files.write(input.resolve(fileName), lines, StandardCharsets.ISO_8859_1);
        return input;
    }

    private List<String> linesOf(String fileName) {
        return new ArrayList<>(Fixtures.lines(Fixtures.input("i01-baseline").resolve(fileName)));
    }

    private String stopOnOpen(Path input) {
        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> FileRunWorkspace.open(input, temp.resolve("actual"), "t"));
        assertEquals(StopReason.RECORD_REJECTED, stop.reason());
        return stop.getMessage();
    }

    @Test
    void e1_nonDigitInARateNamesFileRecordAndField() throws IOException {
        List<String> rates = linesOf(RATES);
        rates.set(2, "GOLD      0100X100120{");

        assertEquals("RECORD_REJECTED (INT-R08): file " + RATES + " record 3 field DIS-TRAN-CAT-CD: is not numeric",
                stopOnOpen(inputWith(RATES, rates)));
    }

    @Test
    void e1_nonDigitInABalanceNamesTheRecordNumberOfTheFileNotOfTheKeyOrder() throws IOException {
        // the bad record is the first in key order and the second on the file
        Path input = inputWith(BALANCES, List.of("000000020020100010000024000{", "000000020010100010000X10000{"));
        FileRunWorkspace workspace = FileRunWorkspace.open(input, temp.resolve("actual"), "t");

        InterestRunStoppedException stop = assertThrows(InterestRunStoppedException.class,
                () -> workspace.balanceSource().nextAccount());

        assertEquals("RECORD_REJECTED (INT-R08): file " + BALANCES + " record 2 field TRAN-CAT-BAL: is not numeric",
                stop.getMessage());
    }

    @Test
    void e3_byteThatIsNotACharacterInACardNamesFileRecordAndField() throws IOException {
        // G07: low-value in XREF-CARD-NUM
        List<String> cards = linesOf(CARDS);
        cards.set(1, "\u0000" + cards.get(1).substring(1));

        assertEquals("RECORD_REJECTED (INT-R08): file " + CARDS + " record 2 field XREF-CARD-NUM: "
                + "holds a byte that is not a character of the code page fixture", stopOnOpen(inputWith(CARDS, cards)));
    }

    @Test
    void e1_fieldTheCalculationDoesNotReadIsCheckedToo() throws IOException {
        // ACCT-CREDIT-LIMIT (offset 24) is carried, not read (INT-H04); the codec decodes every field
        List<String> accounts = linesOf(ACCOUNTS);
        accounts.set(0, accounts.get(0).substring(0, 24) + "X" + accounts.get(0).substring(25));

        assertEquals("RECORD_REJECTED (INT-R08): file " + ACCOUNTS + " record 1 field ACCT-CREDIT-LIMIT: is not numeric",
                stopOnOpen(inputWith(ACCOUNTS, accounts)));
    }

    @Test
    void recordLongerThanTheLayoutNamesFileAndRecord() throws IOException {
        // 51 bytes for RECLN 50: rejected, not cut
        List<String> balances = linesOf(BALANCES);
        balances.set(1, balances.get(1) + " ".repeat(50 - balances.get(1).length()) + "X");

        assertEquals("RECORD_REJECTED (INT-R08): file " + BALANCES + " record 2 record CVTRA01Y: "
                + "the record is 51 bytes, the layout is 50", stopOnOpen(inputWith(BALANCES, balances)));
    }
}
