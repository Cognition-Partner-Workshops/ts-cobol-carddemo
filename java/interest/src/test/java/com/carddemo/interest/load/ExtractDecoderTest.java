package com.carddemo.interest.load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.carddemo.interest.codec.RecordFormat;

/** Decoding of the unloaded files before a table is touched: TL3 rejects, RC1-RC4 figures, TL4 profile. */
class ExtractDecoderTest {

    private static final String CASE = "i01-baseline";
    /** CVTRA01Y: TRANCAT-ACCT-ID 0-10, TRANCAT-TYPE-CD 11-12, TRANCAT-CD 13-16, TRAN-CAT-BAL 17-27 (sign in 27). */
    private static final int BALANCE_FIRST_DIGIT = 17;
    private static final int BALANCE_SIGN = 27;

    private final ExtractDecoder decoder = new ExtractDecoder(RecordFormat.fixture());

    private static byte[] bytes(String... lines) {
        return (String.join("\n", lines) + "\n").getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String[] balanceLines() {
        return new String(LoadFixtures.content(CASE, ExtractFile.CATEGORY_BALANCES), StandardCharsets.ISO_8859_1).split("\n");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.carddemo.interest.load.LoadFixtures#reconciliationTable")
    void rc1Rc2_figuresOfEveryCaseEqualTheReconciliationTable(String caseName, long accounts, long cards, long balances,
            long rates, String accountBalance, String categoryBalance, String rate) {
        List<RejectedRecord> rejects = new ArrayList<>();
        Map<ExtractFile, DecodedExtract> extracts = new EnumMap<>(ExtractFile.class);
        for (ExtractFile file : ExtractFile.values()) {
            extracts.put(file, decoder.decode(file, LoadFixtures.content(caseName, file), rejects).orElseThrow());
        }
        assertEquals(List.of(), rejects);
        assertEquals(accounts, extracts.get(ExtractFile.ACCOUNTS).figures().count());
        assertEquals(cards, extracts.get(ExtractFile.CARD_XREFS).figures().count());
        assertEquals(balances, extracts.get(ExtractFile.CATEGORY_BALANCES).figures().count());
        assertEquals(rates, extracts.get(ExtractFile.DISCLOSURE_GROUPS).figures().count());
        assertEquals(LoadFixtures.amount(accountBalance), extracts.get(ExtractFile.ACCOUNTS).figures().sum("acct_curr_bal"));
        assertEquals(LoadFixtures.amount(categoryBalance),
                extracts.get(ExtractFile.CATEGORY_BALANCES).figures().sum("tran_cat_bal"));
        assertEquals(LoadFixtures.amount(rate), extracts.get(ExtractFile.DISCLOSURE_GROUPS).figures().sum("dis_int_rate"));
        // TL4 on the fixtures: nothing found, type codes are digits only (G06)
        ExtractProfile profile = ExtractProfile.of(extracts, RecordFormat.fixture().codePage().space());
        assertTrue(profile.isClean(), profile.toString());
        assertTrue(profile.balanceTypeCodes().stream().allMatch(code -> code.chars().allMatch(Character::isDigit)));
        System.out.println("TL4 profile " + caseName + ": " + profile);
    }

    @Test
    void rc2_hashTotalsOfTheBaseline() {
        // accounts 2001 and 2002 in all three files: 2001 + 2002 = 4003
        List<RejectedRecord> rejects = new ArrayList<>();
        assertEquals("4003", decoder.decode(ExtractFile.ACCOUNTS, LoadFixtures.content(CASE, ExtractFile.ACCOUNTS), rejects)
                .orElseThrow().figures().sum("acct_id").toPlainString());
        assertEquals("4003", decoder.decode(ExtractFile.CARD_XREFS, LoadFixtures.content(CASE, ExtractFile.CARD_XREFS), rejects)
                .orElseThrow().figures().sum("xref_acct_id").toPlainString());
        assertEquals("4003", decoder.decode(ExtractFile.CATEGORY_BALANCES,
                LoadFixtures.content(CASE, ExtractFile.CATEGORY_BALANCES), rejects)
                .orElseThrow().figures().sum("trancat_acct_id").toPlainString());
    }

    @Test
    void tl3_aBadDigitIsRejectedWithFileRecordNumberAndField() {
        String[] lines = balanceLines();
        lines[1] = LoadFixtures.charAt(BALANCE_FIRST_DIGIT, 'X').apply(lines[1]);
        List<RejectedRecord> rejects = new ArrayList<>();

        Optional<DecodedExtract> extract = decoder.decode(ExtractFile.CATEGORY_BALANCES, bytes(lines), rejects);

        assertTrue(extract.isEmpty());
        assertEquals(1, rejects.size());
        assertEquals("AWS.M2.CARDDEMO.TCATBALF.PS.txt", rejects.get(0).file());
        assertEquals(2, rejects.get(0).recordNumber());
        assertEquals("TRAN-CAT-BAL", rejects.get(0).field());
        assertFalse(rejects.get(0).line().contains("2002"), "a reject line holds no value of the record (S6.5)");
    }

    @Test
    void tl3_everyBadRecordOfAFileIsListed() {
        String[] lines = balanceLines();
        lines[0] = LoadFixtures.charAt(0, 'A').apply(lines[0]);
        lines[1] = LoadFixtures.charAt(BALANCE_SIGN, '?').apply(lines[1]);
        List<RejectedRecord> rejects = new ArrayList<>();

        decoder.decode(ExtractFile.CATEGORY_BALANCES, bytes(lines), rejects);

        assertEquals(List.of("1 TRANCAT-ACCT-ID", "2 TRAN-CAT-BAL"),
                rejects.stream().map(reject -> reject.recordNumber() + " " + reject.field()).toList());
    }

    @Test
    void tl3_aRecordLongerThanItsLayoutIsRejectedAsARecord() {
        String[] lines = balanceLines();
        lines[1] = lines[1] + " ".repeat(30) + "X";
        List<RejectedRecord> rejects = new ArrayList<>();

        assertTrue(decoder.decode(ExtractFile.CATEGORY_BALANCES, bytes(lines), rejects).isEmpty());

        assertEquals(1, rejects.size());
        assertEquals(2, rejects.get(0).recordNumber());
        assertEquals(RejectedRecord.WHOLE_RECORD, rejects.get(0).field());
    }

    @Test
    void rc4_aKeyThatOccursTwiceIsRejected() {
        String[] lines = balanceLines();
        List<RejectedRecord> rejects = new ArrayList<>();

        assertTrue(decoder.decode(ExtractFile.CATEGORY_BALANCES, bytes(lines[0], lines[1], lines[0]), rejects).isEmpty());

        assertEquals(1, rejects.size());
        assertEquals(3, rejects.get(0).recordNumber());
        assertEquals("TRANCAT-ACCT-ID+TRANCAT-TYPE-CD+TRANCAT-CD", rejects.get(0).field());
    }

    @Test
    void g02_anFSignedAmountIsLoadedAndCounted() {
        // 0000001000 0{ becomes 0000001000 00: the same value 1000.00 with a plain digit as its last byte
        String[] lines = balanceLines();
        lines[0] = LoadFixtures.charAt(BALANCE_SIGN, '0').apply(lines[0]);
        List<RejectedRecord> rejects = new ArrayList<>();

        DecodedExtract extract = decoder.decode(ExtractFile.CATEGORY_BALANCES, bytes(lines), rejects).orElseThrow();

        assertEquals(1, extract.fSignedAmounts());
        assertEquals(0, extract.minusZeroAmounts());
        // 1000.00 + 2400.00
        assertEquals(LoadFixtures.amount("3400.00"), extract.figures().sum("tran_cat_bal"));
    }

    @Test
    void g03_minusZeroIsLoadedAsZeroAndCounted() {
        // the balance of record 1 becomes 0000000000} = -0
        String[] lines = balanceLines();
        lines[0] = lines[0].substring(0, BALANCE_FIRST_DIGIT) + "0000000000}";
        List<RejectedRecord> rejects = new ArrayList<>();

        DecodedExtract extract = decoder.decode(ExtractFile.CATEGORY_BALANCES, bytes(lines), rejects).orElseThrow();

        assertEquals(1, extract.minusZeroAmounts());
        assertEquals(0, extract.fSignedAmounts());
        // 0.00 + 2400.00
        assertEquals(LoadFixtures.amount("2400.00"), extract.figures().sum("tran_cat_bal"));
    }
}
