package com.carddemo.interest.load;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.DecodedRecord;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.LayoutField;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;

/**
 * The profile of strategy item TL4: what the mapping gaps ask to be counted in the unloaded files.
 * Every figure is a count; zero means nothing found.
 *
 * @param accountsWithSeveralCards accounts with more than one card cross-reference (G08, INT-H03)
 * @param accountsWithMixedTypeCodes accounts whose balances have type codes starting with a letter
 *        and type codes starting with a digit (G06)
 * @param distinctTypeCodes the TRANCAT-TYPE-CD values of the file (G06)
 * @param minusZeroFields signed fields holding minus zero (G03)
 * @param fSignedFields signed fields with an F sign (G02)
 * @param nonBlankFillers records whose FILLER is not all spaces
 * @param inactiveAccounts accounts whose ACCT-ACTIVE-STATUS is not Y (INT-H04)
 * @param balancesWithoutGroupRate balances with no rate for the account's group (INT-R03 falls back to DEFAULT)
 * @param balancesWithoutAnyRate balances with no rate for the group and none for DEFAULT (INT-R08)
 * @param balancesWithoutAccount balances whose account is not in the account file (INT-R08)
 */
public record ExtractProfile(long accountsWithSeveralCards, long accountsWithMixedTypeCodes, Set<String> distinctTypeCodes,
        long minusZeroFields, long fSignedFields, long nonBlankFillers, long inactiveAccounts,
        long balancesWithoutGroupRate, long balancesWithoutAnyRate, long balancesWithoutAccount) {

    private static final String DEFAULT_GROUP = "DEFAULT";

    static ExtractProfile of(Map<ExtractFile, DecodedExtract> extracts, CodePage codePage) {
        long minusZero = 0;
        long fSigned = 0;
        long nonBlankFillers = 0;
        for (DecodedExtract extract : extracts.values()) {
            RecordLayout layout = extract.file().table().layout();
            for (int i = 0; i < extract.records().size(); i++) {
                byte[] source = extract.sources().get(i);
                RoundTrip signs = RoundTrip.compare(layout, source,
                        layout.encode(extract.records().get(i), codePage, new RecordSource(extract.fileName(), i + 1L)),
                        codePage);
                minusZero += signs.minusZero();
                fSigned += signs.fSigned();
                if (!fillersBlank(layout, source, codePage)) {
                    nonBlankFillers++;
                }
            }
        }

        Map<String, Integer> cardsOfAccount = new HashMap<>();
        for (DecodedRecord card : extracts.get(ExtractFile.CARDS).records()) {
            cardsOfAccount.merge(card.number("XREF-ACCT-ID").toPlainString(), 1, Integer::sum);
        }
        Map<String, String> groupOfAccount = new HashMap<>();
        long inactive = 0;
        for (DecodedRecord account : extracts.get(ExtractFile.ACCOUNTS).records()) {
            groupOfAccount.put(account.number("ACCT-ID").toPlainString(), account.text("ACCT-GROUP-ID"));
            if (!"Y".equals(account.text("ACCT-ACTIVE-STATUS"))) {
                inactive++;
            }
        }
        Set<List<String>> rateKeys = new HashSet<>();
        for (DecodedRecord rate : extracts.get(ExtractFile.RATES).records()) {
            rateKeys.add(List.of(rate.text("DIS-ACCT-GROUP-ID"), rate.text("DIS-TRAN-TYPE-CD"),
                    rate.number("DIS-TRAN-CAT-CD").toPlainString()));
        }
        Set<String> typeCodes = new TreeSet<>();
        Map<String, Set<Boolean>> kindsOfAccount = new HashMap<>();
        long withoutGroupRate = 0;
        long withoutAnyRate = 0;
        long withoutAccount = 0;
        for (DecodedRecord balance : extracts.get(ExtractFile.BALANCES).records()) {
            String accountId = balance.number("TRANCAT-ACCT-ID").toPlainString();
            String typeCode = balance.text("TRANCAT-TYPE-CD");
            String category = balance.number("TRANCAT-CD").toPlainString();
            typeCodes.add(typeCode);
            kindsOfAccount.computeIfAbsent(accountId, id -> new HashSet<>())
                    .add(!typeCode.isEmpty() && Character.isDigit(typeCode.charAt(0)));
            String group = groupOfAccount.get(accountId);
            if (group == null) {
                withoutAccount++;
            } else if (!rateKeys.contains(List.of(group, typeCode, category))) {
                withoutGroupRate++;
                if (!rateKeys.contains(List.of(DEFAULT_GROUP, typeCode, category))) {
                    withoutAnyRate++;
                }
            }
        }
        return new ExtractProfile(
                cardsOfAccount.values().stream().filter(cards -> cards > 1).count(),
                kindsOfAccount.values().stream().filter(kinds -> kinds.size() > 1).count(),
                typeCodes, minusZero, fSigned, nonBlankFillers, inactive, withoutGroupRate, withoutAnyRate, withoutAccount);
    }

    /** True when nothing of TL4 was found. A balance that uses the DEFAULT rate is the coded rule INT-R03, not a finding. */
    public boolean nothingFound() {
        return accountsWithSeveralCards == 0 && accountsWithMixedTypeCodes == 0 && minusZeroFields == 0 && fSignedFields == 0
                && nonBlankFillers == 0 && inactiveAccounts == 0 && balancesWithoutAnyRate == 0 && balancesWithoutAccount == 0;
    }

    public String describe() {
        return "TL4 several-cards(G08)=" + accountsWithSeveralCards + " mixed-type-codes(G06)=" + accountsWithMixedTypeCodes
                + " type-codes=" + distinctTypeCodes + " minus-zero(G03)=" + minusZeroFields + " f-signed(G02)=" + fSignedFields
                + " non-blank-fillers=" + nonBlankFillers + " inactive-accounts(INT-H04)=" + inactiveAccounts
                + " balances-on-DEFAULT-rate(INT-R03)=" + balancesWithoutGroupRate
                + " balances-without-rate(INT-R08)=" + balancesWithoutAnyRate
                + " balances-without-account(INT-R08)=" + balancesWithoutAccount;
    }

    private static boolean fillersBlank(RecordLayout layout, byte[] record, CodePage codePage) {
        for (LayoutField field : layout.fields()) {
            if (field.kind() != FieldKind.FILLER) {
                continue;
            }
            byte[] spaces = new byte[field.length()];
            Arrays.fill(spaces, codePage.space());
            if (!Arrays.equals(record, field.offset(), field.end(), spaces, 0, spaces.length)) {
                return false;
            }
        }
        return true;
    }
}
