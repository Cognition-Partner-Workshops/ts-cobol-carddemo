package com.carddemo.interest.load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.codec.CodePage;

class ExtractFormatTest {

    @Test
    void theTwoValuesOfTheJobParameter() {
        assertEquals(ExtractFormat.FIXTURE, ExtractFormat.of("fixture"));
        assertEquals(ExtractFormat.HOST, ExtractFormat.of("host"));
        assertThrows(IllegalArgumentException.class, () -> ExtractFormat.of("csv"));
    }

    @Test
    void fileNamesAndCodePages() {
        assertEquals("AWS.M2.CARDDEMO.ACCTDATA.PS.txt", ExtractFile.ACCOUNTS.fileName(ExtractFormat.FIXTURE));
        assertEquals("AWS.M2.CARDDEMO.ACCTDATA.PS", ExtractFile.ACCOUNTS.fileName(ExtractFormat.HOST));
        assertEquals(CodePage.FIXTURE, ExtractFormat.FIXTURE.codePage());
        // REL-08 is OPEN: cp037 is the proposed option
        assertEquals("IBM037", ExtractFormat.HOST.codePage().charset().name());
    }
}
