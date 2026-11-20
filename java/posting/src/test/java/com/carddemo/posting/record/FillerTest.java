package com.carddemo.posting.record;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.carddemo.posting.record.Records.Account;
import com.carddemo.posting.record.Records.TranCatBalance;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FillerTest {

    @Test
    void accountRoundTripPreservesEveryFillerByte() {
        String prefix = "00000000001Y" + "00000000000{".repeat(3)
                + "2020-01-01" + "2030-01-01" + "2020-01-01"
                + "00000000000{".repeat(2) + " ".repeat(20);
        for (String filler : new String[]{" ".repeat(178), "Z\u0000\u00ff" + " ".repeat(175)}) {
            String record = prefix + filler;
            assertEquals(record, Account.parse(record).format());
        }
    }

    @Test
    void balanceRoundTripAndUpdatePreserveFiller() {
        for (String filler : new String[]{" ".repeat(22), "Z\u0000\u00ff" + " ".repeat(19)}) {
            String record = "00000000001010001" + "0000000000{" + filler;
            TranCatBalance parsed = TranCatBalance.parse(record);
            assertEquals(record, parsed.format());
            assertEquals(filler, new TranCatBalance(parsed.acctId(), parsed.typeCd(), parsed.catCd(),
                    BigDecimal.ONE, parsed.filler()).format().substring(28));
        }
    }
}
