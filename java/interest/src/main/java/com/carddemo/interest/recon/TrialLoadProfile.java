package com.carddemo.interest.recon;

import java.math.BigDecimal;
import java.util.ArrayList;
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
 * The profile of TL4 over the four unloaded files: what the mapping gaps ask to be counted. Counts
 * only; no value of a record is shown except the distinct type codes (G06).
 */
public record TrialLoadProfile(long accountsWithSeveralCards, long accountsWithMixedTypeCodes, Set<String> typeCodes,
        long minusZeroFields, long fSignedFields, long nonBlankFillers, long inactiveAccounts,
        long balancesOnDefaultRate, long balancesWithoutRate, long balancesWithoutAccount) {

    private static final String DEFAULT_GROUP = "DEFAULT";

    public static TrialLoadProfile of(RecordFiles accounts, RecordFiles cards, RecordFiles balances, RecordFiles rates, CodePage codePage) {
        long minusZero = 0;
        long fSigned = 0;
        long nonBlankFillers = 0;
        for (RecordFiles file : List.of(accounts, cards, balances, rates)) {
            for (int i = 0; i < file.records().size(); i++) {
                byte[] source = file.sources().get(i);
                byte[] again = file.layout().encode(file.records().get(i), codePage, new RecordSource(file.fileName(), i + 1L));
                RoundTripCount signs = RoundTripCount.of(file.layout(), source, again, codePage);
                minusZero += signs.minusZero();
                fSigned += signs.fSigned();
                if (!fillersBlank(file.layout(), source, codePage)) {
                    nonBlankFillers++;
                }
            }
        }
        Map<String, Integer> cardsOfAccount = new HashMap<>();
        for (DecodedRecord card : cards.records()) {
            cardsOfAccount.merge(card.number("XREF-ACCT-ID").toPlainString(), 1, Integer::sum);
        }
        Map<String, String> groupOfAccount = new HashMap<>();
        long inactive = 0;
        for (DecodedRecord account : accounts.records()) {
            groupOfAccount.put(account.number("ACCT-ID").toPlainString(), account.text("ACCT-GROUP-ID"));
            if (!"Y".equals(account.text("ACCT-ACTIVE-STATUS"))) {
                inactive++;
            }
        }
        Set<List<String>> rateKeys = new HashSet<>();
        for (DecodedRecord rate : rates.records()) {
            rateKeys.add(List.of(rate.text("DIS-ACCT-GROUP-ID"), rate.text("DIS-TRAN-TYPE-CD"), rate.number("DIS-TRAN-CAT-CD").toPlainString()));
        }
        Set<String> typeCodes = new TreeSet<>();
        Map<String, Set<Boolean>> kindsOfAccount = new HashMap<>();
        long onDefault = 0;
        long withoutRate = 0;
        long withoutAccount = 0;
        for (DecodedRecord balance : balances.records()) {
            String accountId = balance.number("TRANCAT-ACCT-ID").toPlainString();
            String typeCode = balance.text("TRANCAT-TYPE-CD");
            String category = balance.number("TRANCAT-CD").toPlainString();
            typeCodes.add(typeCode);
            kindsOfAccount.computeIfAbsent(accountId, id -> new HashSet<>()).add(!typeCode.isEmpty() && Character.isDigit(typeCode.charAt(0)));
            String group = groupOfAccount.get(accountId);
            if (group == null) {
                withoutAccount++;
            } else if (!rateKeys.contains(List.of(group, typeCode, category))) {
                if (rateKeys.contains(List.of(DEFAULT_GROUP, typeCode, category))) {
                    onDefault++;
                } else {
                    withoutRate++;
                }
            }
        }
        return new TrialLoadProfile(
                cardsOfAccount.values().stream().filter(count -> count > 1).count(),
                kindsOfAccount.values().stream().filter(kinds -> kinds.size() > 1).count(),
                typeCodes, minusZero, fSigned, nonBlankFillers, inactive, onDefault, withoutRate, withoutAccount);
    }

    /** A balance on the DEFAULT rate is the coded rule INT-R03 and not a finding. */
    public boolean nothingFound() {
        return accountsWithSeveralCards == 0 && accountsWithMixedTypeCodes == 0 && minusZeroFields == 0 && fSignedFields == 0
                && nonBlankFillers == 0 && inactiveAccounts == 0 && balancesWithoutRate == 0 && balancesWithoutAccount == 0;
    }

    /** The lines of the report: what was counted, the gap or rule it belongs to, the count. */
    public List<String[]> rows() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"accounts with several cards", "G08, INT-H03", Long.toString(accountsWithSeveralCards)});
        rows.add(new String[] {"accounts with letter and digit type codes", "G06", Long.toString(accountsWithMixedTypeCodes)});
        rows.add(new String[] {"distinct type codes", "G06", String.join(" ", typeCodes)});
        rows.add(new String[] {"minus zero amounts", "G03", Long.toString(minusZeroFields)});
        rows.add(new String[] {"F-signed amounts", "G02", Long.toString(fSignedFields)});
        rows.add(new String[] {"records with a FILLER that is not blank", "E4", Long.toString(nonBlankFillers)});
        rows.add(new String[] {"accounts that are not active", "INT-H04", Long.toString(inactiveAccounts)});
        rows.add(new String[] {"balances without a rate of their group and without a DEFAULT rate", "INT-R08", Long.toString(balancesWithoutRate)});
        rows.add(new String[] {"balances without an account", "INT-R08", Long.toString(balancesWithoutAccount)});
        rows.add(new String[] {"balances on the DEFAULT rate (information, not a finding)", "INT-R03", Long.toString(balancesOnDefaultRate)});
        return rows;
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

    /** RC3 for one record: equal, or the fields that differ counted per gap; anything else is unexplained. */
    record RoundTripCount(long equal, long fSigned, long minusZero, long other) {

        static RoundTripCount of(RecordLayout layout, byte[] source, byte[] again, CodePage codePage) {
            if (again == null) {
                return new RoundTripCount(0, 0, 0, 1);
            }
            if (Arrays.equals(source, again)) {
                return new RoundTripCount(1, 0, 0, 0);
            }
            byte[] digits = "0123456789".getBytes(codePage.charset());
            long fSigned = 0;
            long minusZero = 0;
            long other = 0;
            RecordSource built = RecordSource.built(layout.copybook());
            for (LayoutField field : layout.fields()) {
                if (Arrays.equals(source, field.offset(), field.end(), again, field.offset(), field.end())) {
                    continue;
                }
                int last = field.end() - 1;
                boolean onlySignDiffers = field.kind() == FieldKind.SIGNED_ZONED
                        && Arrays.equals(source, field.offset(), last, again, field.offset(), last);
                boolean digitSign = false;
                for (byte digit : digits) {
                    digitSign |= digit == source[last];
                }
                if (!onlySignDiffers) {
                    other++;
                } else if (digitSign) {
                    fSigned++;
                } else if (((BigDecimal) layout.read(again, field.name(), codePage, built)).signum() == 0) {
                    minusZero++;
                } else {
                    other++;
                }
            }
            return new RoundTripCount(0, fSigned, minusZero, other);
        }
    }
}
