package com.carddemo.interest.load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.CodePage;

/** Strategy item TL4: the profile counts what the mapping gaps ask for. */
class ExtractProfileTest {

    @TempDir
    Path temp;

    private static ExtractProfile profileOf(Path dir) {
        Map<ExtractFile, DecodedExtract> extracts = new EnumMap<>(ExtractFile.class);
        for (ExtractFile file : ExtractFile.values()) {
            extracts.put(file, ExtractDecoder.decode(dir, file, ExtractFormat.FIXTURE));
        }
        return ExtractProfile.of(extracts, CodePage.FIXTURE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void fixtures_nothingFound(String caseName) {
        ExtractProfile profile = profileOf(Fixtures.input(caseName));

        System.out.println("TL4 profile " + caseName + ": " + profile.describe());
        assertTrue(profile.nothingFound(), profile.describe());
        // INT-R03 as coded: only i04 has a balance that takes the DEFAULT rate
        assertEquals(caseName.equals("i04-default-group-fallback") ? 1 : 0, profile.balancesWithoutGroupRate());
    }

    @Test
    void countsEveryKindOfFinding() {
        Path dir = LoadFiles.copyOf("i01-baseline", temp.resolve("in"));
        LoadFiles.edit(dir, ExtractFile.CARDS, lines -> {
            // a second card for the account of the first record (G08)
            lines.add(LoadFiles.overlay(lines.get(0), 0, "4999999999999999"));
            return lines;
        });
        LoadFiles.edit(dir, ExtractFile.ACCOUNTS, lines -> {
            // inactive (INT-H04), an F-signed credit limit (G02) and a FILLER that is not blank
            lines.set(0, LoadFiles.overlay(LoadFiles.overlay(LoadFiles.overlay(lines.get(0), 11, "N"), 35, "0"), 299, "x"));
            // a group without rates and without DEFAULT rates for type 09 below
            return lines;
        });
        LoadFiles.edit(dir, ExtractFile.BALANCES, lines -> {
            String first = lines.get(0);
            lines.add(LoadFiles.overlay(first, 11, "AB"));                                 // letters and digits for one account (G06), no rate at all
            lines.add(LoadFiles.overlay(LoadFiles.overlay(first, 0, "00000009999"), 17, "0000000000}")); // no account, minus zero (G03)
            return lines;
        });

        ExtractProfile profile = profileOf(dir);

        assertEquals(1, profile.accountsWithSeveralCards());
        assertEquals(1, profile.accountsWithMixedTypeCodes());
        assertEquals(Set.of("01", "AB"), profile.distinctTypeCodes());
        assertEquals(1, profile.minusZeroFields());
        assertEquals(1, profile.fSignedFields());
        assertEquals(1, profile.nonBlankFillers());
        assertEquals(1, profile.inactiveAccounts());
        assertEquals(1, profile.balancesWithoutAnyRate());
        assertEquals(1, profile.balancesWithoutAccount());
        assertFalse(profile.nothingFound());
    }
}
