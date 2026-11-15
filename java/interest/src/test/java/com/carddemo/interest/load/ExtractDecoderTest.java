package com.carddemo.interest.load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/** Strategy item TL3: every record is decoded or named on the reject list with file, record number and field. */
class ExtractDecoderTest {

    private static final String CASE = "i01-baseline";
    private static final String BALANCES = "AWS.M2.CARDDEMO.TCATBALF.PS.txt";

    @TempDir
    Path temp;

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void everyRecordOfEveryCaseDecodes(String caseName) {
        for (ExtractFile file : ExtractFile.values()) {
            DecodedExtract extract = ExtractDecoder.decode(Fixtures.input(caseName), file, ExtractFormat.FIXTURE);

            assertEquals(List.of(), extract.rejects());
            assertEquals(Fixtures.lines(Fixtures.input(caseName).resolve(extract.fileName())).size(), extract.records().size());
            assertEquals(extract.sources().size(), extract.records().size());
        }
    }

    @Test
    void notNumericAmount_isRejectedWithFileRecordAndField() {
        Path dir = LoadFiles.copyOf(CASE, temp.resolve("in"));
        LoadFiles.edit(dir, ExtractFile.BALANCES, lines -> {
            lines.set(1, LoadFiles.overlay(lines.get(1), 20, "X"));
            return lines;
        });

        DecodedExtract extract = ExtractDecoder.decode(dir, ExtractFile.BALANCES, ExtractFormat.FIXTURE);

        assertEquals(1, extract.rejects().size());
        Reject reject = extract.rejects().get(0);
        assertEquals(BALANCES, reject.file());
        assertEquals(2, reject.recordNumber());
        assertEquals("TRAN-CAT-BAL", reject.field());
        assertTrue(reject.reason().contains("E2"), reject.reason());
        assertEquals(List.of(), extract.records());
    }

    @Test
    void everyBrokenFieldOfARecordIsListed() {
        Path dir = LoadFiles.copyOf(CASE, temp.resolve("in"));
        LoadFiles.edit(dir, ExtractFile.BALANCES, lines -> {
            lines.set(0, LoadFiles.overlay(LoadFiles.overlay(lines.get(0), 0, "A"), 14, "?"));
            return lines;
        });

        List<Reject> rejects = ExtractDecoder.decode(dir, ExtractFile.BALANCES, ExtractFormat.FIXTURE).rejects();

        assertEquals(List.of("TRANCAT-ACCT-ID", "TRANCAT-CD"), rejects.stream().map(Reject::field).toList());
        assertEquals(List.of(1L, 1L), rejects.stream().map(Reject::recordNumber).toList());
    }

    @Test
    void recordLongerThanTheCopybook_isRejected() {
        Path dir = LoadFiles.copyOf(CASE, temp.resolve("in"));
        LoadFiles.edit(dir, ExtractFile.CARDS, lines -> {
            lines.set(1, LoadFiles.overlay(lines.get(1), 50, "Z"));
            return lines;
        });

        List<Reject> rejects = ExtractDecoder.decode(dir, ExtractFile.CARDS, ExtractFormat.FIXTURE).rejects();

        assertEquals(1, rejects.size());
        assertEquals(2, rejects.get(0).recordNumber());
        assertEquals("(record)", rejects.get(0).field());
        assertEquals("record is 51 bytes, CVACT03Y is 50", rejects.get(0).reason());
    }

    @Test
    void repeatedKey_isRejected_rc4() {
        Path dir = LoadFiles.copyOf(CASE, temp.resolve("in"));
        LoadFiles.edit(dir, ExtractFile.ACCOUNTS, lines -> {
            lines.add(lines.get(0));
            return lines;
        });

        List<Reject> rejects = ExtractDecoder.decode(dir, ExtractFile.ACCOUNTS, ExtractFormat.FIXTURE).rejects();

        assertEquals(1, rejects.size());
        assertEquals(3, rejects.get(0).recordNumber());
        assertEquals("ACCT-ID", rejects.get(0).field());
        assertEquals("key already in record 1 (RC4)", rejects.get(0).reason());
    }

    @Test
    void missingFile_stopsTheRun() {
        InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class,
                () -> ExtractDecoder.decode(temp.resolve("nowhere"), ExtractFile.RATES, ExtractFormat.FIXTURE));

        assertEquals(StopReason.OPEN_INPUT, stopped.reason());
    }

    @Test
    void hostFile_decodesToTheSameValues() {
        Path host = LoadFiles.hostCopyOf(CASE, temp.resolve("host"));
        for (ExtractFile file : ExtractFile.values()) {
            DecodedExtract fromFixture = ExtractDecoder.decode(Fixtures.input(CASE), file, ExtractFormat.FIXTURE);
            DecodedExtract fromHost = ExtractDecoder.decode(host, file, ExtractFormat.HOST);

            assertEquals(List.of(), fromHost.rejects());
            assertEquals(fromFixture.records().size(), fromHost.records().size());
            for (int i = 0; i < fromHost.records().size(); i++) {
                for (var field : file.table().layout().fields()) {
                    if (field.kind() == com.carddemo.interest.codec.FieldKind.TEXT) {
                        assertEquals(fromFixture.records().get(i).text(field.name()), fromHost.records().get(i).text(field.name()));
                    } else if (field.kind() != com.carddemo.interest.codec.FieldKind.FILLER) {
                        assertEquals(fromFixture.records().get(i).number(field.name()), fromHost.records().get(i).number(field.name()));
                    }
                }
            }
        }
    }

    @Test
    void hostFileNotEndingOnARecordBoundary_isRejected() throws Exception {
        Path host = LoadFiles.hostCopyOf(CASE, temp.resolve("host"));
        Path cards = host.resolve(ExtractFile.CARDS.fileName(ExtractFormat.HOST));
        byte[] content = Files.readAllBytes(cards);
        Files.write(cards, java.util.Arrays.copyOf(content, content.length - 7));

        List<Reject> rejects = ExtractDecoder.decode(host, ExtractFile.CARDS, ExtractFormat.HOST).rejects();

        assertEquals(1, rejects.size());
        assertEquals(2, rejects.get(0).recordNumber());
        assertEquals("record is 43 bytes, CVACT03Y is 50", rejects.get(0).reason());
    }
}
