package com.carddemo.interest.load;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordLayout.Kind;
import com.carddemo.interest.codec.RecordValues;

/**
 * The trial-load profile of strategy TL4: what the mapping gaps ask to be counted in the files.
 * It holds counts and the distinct type codes (reference data), never a value of an account.
 */
public record ExtractProfile(
        long accountsWithSeveralCards,
        Set<String> balanceTypeCodes,
        long accountsWithMixedTypeCodes,
        long minusZeroAmounts,
        long fSignedAmounts,
        long nonBlankFillers,
        long accountsNotActive,
        long balancesWithoutRate) {

    private static final String ACTIVE = "Y";
    private static final String DEFAULT_GROUP = "DEFAULT";

    public ExtractProfile {
        balanceTypeCodes = Collections.unmodifiableSortedSet(new TreeSet<>(balanceTypeCodes));
    }

    public static ExtractProfile of(Map<ExtractFile, DecodedExtract> extracts, byte space) {
        List<RecordValues> accounts = extracts.get(ExtractFile.ACCOUNTS).rows();
        List<RecordValues> cards = extracts.get(ExtractFile.CARD_XREFS).rows();
        List<RecordValues> balances = extracts.get(ExtractFile.CATEGORY_BALANCES).rows();
        List<RecordValues> rates = extracts.get(ExtractFile.DISCLOSURE_GROUPS).rows();

        // G08 (INT-H03)
        long severalCards = cards.stream()
                .collect(Collectors.groupingBy(card -> card.unsigned("XREF-ACCT-ID"), Collectors.counting()))
                .values().stream().filter(count -> count > 1).count();
        // G06: one account with type codes that start with a digit and type codes that do not
        long mixedTypeCodes = balances.stream()
                .collect(Collectors.groupingBy(balance -> balance.unsigned("TRANCAT-ACCT-ID"),
                        Collectors.mapping(balance -> startsWithDigit(balance.text("TRANCAT-TYPE-CD")), Collectors.toSet())))
                .values().stream().filter(kinds -> kinds.size() > 1).count();
        long nonBlankFillers = extracts.values().stream().mapToLong(extract -> nonBlankFillers(extract, space)).sum();
        // INT-H04
        long notActive = accounts.stream().filter(account -> !ACTIVE.equals(account.text("ACCT-ACTIVE-STATUS"))).count();
        // INT-R08: neither the group of the account nor DEFAULT has a rate; a balance without an account is not counted here
        Set<List<Object>> rateKeys = rates.stream()
                .map(rate -> List.<Object>of(rate.text("DIS-ACCT-GROUP-ID"), rate.text("DIS-TRAN-TYPE-CD"), rate.unsigned("DIS-TRAN-CAT-CD")))
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, String> groupByAccount = accounts.stream()
                .collect(Collectors.toMap(account -> account.unsigned("ACCT-ID"), account -> account.text("ACCT-GROUP-ID")));
        long withoutRate = balances.stream().filter(balance -> {
            String group = groupByAccount.get(balance.unsigned("TRANCAT-ACCT-ID"));
            Function<String, List<Object>> key = groupId -> List.of(groupId, balance.text("TRANCAT-TYPE-CD"), balance.unsigned("TRANCAT-CD"));
            return group != null && !rateKeys.contains(key.apply(group)) && !rateKeys.contains(key.apply(DEFAULT_GROUP));
        }).count();
        return new ExtractProfile(
                severalCards,
                balances.stream().map(balance -> balance.text("TRANCAT-TYPE-CD")).collect(Collectors.toSet()),
                mixedTypeCodes,
                extracts.values().stream().mapToLong(DecodedExtract::minusZeroAmounts).sum(),
                extracts.values().stream().mapToLong(DecodedExtract::fSignedAmounts).sum(),
                nonBlankFillers, notActive, withoutRate);
    }

    /** True when TL4 found nothing a mapping gap or an open rule asks about. */
    public boolean isClean() {
        return accountsWithSeveralCards == 0 && accountsWithMixedTypeCodes == 0 && minusZeroAmounts == 0
                && fSignedAmounts == 0 && nonBlankFillers == 0 && accountsNotActive == 0 && balancesWithoutRate == 0;
    }

    private static boolean startsWithDigit(String typeCode) {
        return !typeCode.isEmpty() && typeCode.charAt(0) >= '0' && typeCode.charAt(0) <= '9';
    }

    private static long nonBlankFillers(DecodedExtract extract, byte space) {
        List<FieldLayout> fields = extract.file().layout().fields();
        long count = 0;
        for (RecordValues row : extract.rows()) {
            for (int index = 0; index < fields.size(); index++) {
                if (fields.get(index).kind() == Kind.FILLER && !isBlank(row.filler(index), space)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean isBlank(byte[] filler, byte space) {
        for (byte b : filler) {
            if (b != space) {
                return false;
            }
        }
        return true;
    }
}
