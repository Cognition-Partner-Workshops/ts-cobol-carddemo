package com.carddemo.interest.load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.codec.RecordFormat;

/** TL4: each thing the profile counts, on the baseline with one synthetic change. */
class ExtractProfileTest {

    private static final String CASE = "i01-baseline";

    private static ExtractProfile profile(ExtractFile changed, UnaryOperator<List<String>> change) {
        ExtractDecoder decoder = new ExtractDecoder(RecordFormat.fixture());
        Map<ExtractFile, DecodedExtract> extracts = new EnumMap<>(ExtractFile.class);
        List<RejectedRecord> rejects = new ArrayList<>();
        for (ExtractFile file : ExtractFile.values()) {
            List<String> lines = new ArrayList<>(
                    new String(LoadFixtures.content(CASE, file), StandardCharsets.ISO_8859_1).lines().toList());
            if (file == changed) {
                lines = change.apply(lines);
            }
            byte[] content = (String.join("\n", lines) + "\n").getBytes(StandardCharsets.ISO_8859_1);
            extracts.put(file, decoder.decode(file, content, rejects).orElseThrow(() -> new AssertionError(rejects)));
        }
        return ExtractProfile.of(extracts, RecordFormat.fixture().codePage().space());
    }

    private static UnaryOperator<List<String>> record(int number, UnaryOperator<String> change) {
        return lines -> {
            lines.set(number - 1, change.apply(lines.get(number - 1)));
            return lines;
        };
    }

    @Test
    void g08_intH03_countsAccountsWithSeveralCards() {
        // a second card (another card number) for the account of record 1
        ExtractProfile profile = profile(ExtractFile.CARD_XREFS, lines -> {
            lines.add(LoadFixtures.charAt(15, '9').apply(lines.get(0)));
            return lines;
        });
        assertEquals(1, profile.accountsWithSeveralCards());
        assertFalse(profile.isClean());
    }

    @Test
    void g06_countsAccountsWithTypeCodesOfBothKinds() {
        // a second balance of the account of record 1 with type code A1 instead of 01
        ExtractProfile profile = profile(ExtractFile.CATEGORY_BALANCES, lines -> {
            lines.add(LoadFixtures.charAt(11, 'A').apply(lines.get(0)));
            return lines;
        });
        assertEquals(1, profile.accountsWithMixedTypeCodes());
        assertEquals(List.of("01", "A1"), List.copyOf(profile.balanceTypeCodes()));
    }

    @Test
    void intH04_countsAccountsThatAreNotActive() {
        // ACCT-ACTIVE-STATUS is byte 11 of CVACT01Y
        assertEquals(1, profile(ExtractFile.ACCOUNTS, record(2, LoadFixtures.charAt(11, 'N'))).accountsNotActive());
    }

    @Test
    void countsFillersThatAreNotBlank() {
        // byte 28 is the first byte of the FILLER of CVTRA01Y
        assertEquals(1, profile(ExtractFile.CATEGORY_BALANCES, record(1, line -> line + "Z")).nonBlankFillers());
    }

    @Test
    void intR08_countsBalancesWithoutARateAndWithoutADefaultRate() {
        // category 0001 becomes 0009: no group has a rate for type 01 category 0009
        assertEquals(1, profile(ExtractFile.CATEGORY_BALANCES, record(1, LoadFixtures.charAt(16, '9'))).balancesWithoutRate());
    }
}
