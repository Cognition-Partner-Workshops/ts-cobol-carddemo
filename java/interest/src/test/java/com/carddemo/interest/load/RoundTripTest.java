package com.carddemo.interest.load;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.codec.CardDemoLayouts;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.jdbc.ReplicaTable;

/** Reconciliation RC3: differences are counted per gap id (G02, G03); any other difference fails. */
class RoundTripTest {

    private static final RecordLayout LAYOUT = CardDemoLayouts.CATEGORY_BALANCE;
    private static final RecordSource SOURCE = RecordSource.built("test");
    //                                   acct id    ty cat  balance
    private static final String PLUS = "00000002001" + "01" + "0001" + "0000010000{";

    private static byte[] record(String line, CodePage codePage) {
        byte[] fixture = RecordImage.bytes(RecordImage.fromLine(line, LAYOUT, SOURCE));
        return codePage == CodePage.FIXTURE ? fixture
                : LAYOUT.encode(LAYOUT.decode(fixture, CodePage.FIXTURE, SOURCE), codePage, SOURCE);
    }

    /** What the table gives back: the record decoded and encoded again. */
    private static byte[] reencoded(byte[] source, CodePage codePage) {
        return LAYOUT.encode(LAYOUT.decode(source, codePage, SOURCE), codePage, SOURCE);
    }

    @Test
    void equalRecord() {
        byte[] source = record(PLUS, CodePage.FIXTURE);

        assertEquals(new RoundTrip(1, 1, 0, 0, 0), RoundTrip.compare(LAYOUT, source, reencoded(source, CodePage.FIXTURE), CodePage.FIXTURE));
    }

    @Test
    void g02_fSignedAmountComesBackCSigned() {
        byte[] source = record(PLUS.replace("0000010000{", "00000100000"), CodePage.FIXTURE);

        assertEquals(new RoundTrip(1, 0, 1, 0, 0), RoundTrip.compare(LAYOUT, source, reencoded(source, CodePage.FIXTURE), CodePage.FIXTURE));
    }

    @Test
    void g03_minusZeroComesBackPlusZero() {
        byte[] source = record(PLUS.replace("0000010000{", "0000000000}"), CodePage.FIXTURE);

        assertEquals(new RoundTrip(1, 0, 0, 1, 0), RoundTrip.compare(LAYOUT, source, reencoded(source, CodePage.FIXTURE), CodePage.FIXTURE));
    }

    @Test
    void g02_and_g03_onHostBytes() {
        CodePage host = CodePage.host();
        byte[] fSigned = record(PLUS, host);
        fSigned[27] = (byte) 0xF0;
        byte[] minusZero = record(PLUS.replace("0000010000{", "0000000000{"), host);
        minusZero[27] = (byte) 0xD0;

        assertEquals(new RoundTrip(1, 0, 1, 0, 0), RoundTrip.compare(LAYOUT, fSigned, reencoded(fSigned, host), host));
        assertEquals(new RoundTrip(1, 0, 0, 1, 0), RoundTrip.compare(LAYOUT, minusZero, reencoded(minusZero, host), host));
    }

    @Test
    void aChangedSign_isNotExplainedByAGap() {
        byte[] source = record(PLUS.replace("0000010000{", "0000010000J"), CodePage.FIXTURE);
        byte[] wrong = LAYOUT.replace(source, "TRAN-CAT-BAL", new BigDecimal("1000.01"), CodePage.FIXTURE, SOURCE);

        assertEquals(new RoundTrip(1, 0, 0, 0, 1), RoundTrip.compare(LAYOUT, source, wrong, CodePage.FIXTURE));
    }

    @Test
    void aChangedDigitOrText_isNotExplainedByAGap() {
        byte[] source = record(PLUS, CodePage.FIXTURE);
        byte[] otherAmount = LAYOUT.replace(source, "TRAN-CAT-BAL", new BigDecimal("100.01"), CodePage.FIXTURE, SOURCE);
        byte[] otherType = LAYOUT.replace(source, "TRANCAT-TYPE-CD", "02", CodePage.FIXTURE, SOURCE);

        assertEquals(1, RoundTrip.compare(LAYOUT, source, otherAmount, CodePage.FIXTURE).other());
        assertEquals(1, RoundTrip.compare(LAYOUT, source, otherType, CodePage.FIXTURE).other());
    }

    @Test
    void aMissingRow_isNotExplainedByAGap() {
        assertEquals(new RoundTrip(1, 0, 0, 0, 1), RoundTrip.compare(LAYOUT, record(PLUS, CodePage.FIXTURE), null, CodePage.FIXTURE));
    }

    @Test
    void countsAddUp() {
        assertEquals(new RoundTrip(3, 1, 1, 1, 0),
                RoundTrip.NONE.plus(new RoundTrip(1, 1, 0, 0, 0)).plus(new RoundTrip(1, 0, 1, 0, 0)).plus(new RoundTrip(1, 0, 0, 1, 0)));
    }

    @Test
    void keyIsTheKeyFieldsOfTheRecord() {
        assertEquals("00000002001010001", RoundTrip.key(ReplicaTable.TRAN_CAT_BALANCE, record(PLUS, CodePage.FIXTURE)));
    }
}
