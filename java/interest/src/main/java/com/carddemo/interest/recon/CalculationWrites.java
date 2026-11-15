package com.carddemo.interest.recon;

import com.carddemo.interest.codec.FieldSpec;
import com.carddemo.interest.load.ExtractFile;
import java.util.List;
import java.util.Set;

/**
 * The fields the calculation writes in a replica table: the three amounts of the account break
 * (rule INT-R07, app/cbl/CBACT04C.cbl:350-356). After the calculation these no longer equal the
 * unloaded file, so the round trip (RC3) leaves them out and RC5 accounts for them instead.
 */
final class CalculationWrites {

    private static final Set<String> ACCOUNT_FIELDS = Set.of("ACCT-CURR-BAL", "ACCT-CURR-CYC-CREDIT", "ACCT-CURR-CYC-DEBIT");

    private CalculationWrites() {
    }

    static List<FieldSpec> fields(ExtractFile file) {
        if (file != ExtractFile.ACCOUNT) {
            return List.of();
        }
        return file.layout().fields().stream().filter(field -> ACCOUNT_FIELDS.contains(field.name())).toList();
    }
}
