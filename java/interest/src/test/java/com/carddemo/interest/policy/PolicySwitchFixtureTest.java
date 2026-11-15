package com.carddemo.interest.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.domain.Account;
import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.AccountInterest;
import com.carddemo.interest.domain.InterestCalculator;
import com.carddemo.interest.domain.TransactionCounter;
import com.carddemo.interest.file.FileRunWorkspace;
import com.carddemo.interest.policy.InterestPolicy.IdPrefix;
import com.carddemo.interest.policy.InterestPolicy.InactiveAccounts;
import com.carddemo.interest.policy.InterestPolicy.NegativeBalance;
import com.carddemo.interest.policy.InterestPolicy.Rounding;
import com.carddemo.interest.policy.InterestPolicy.TimestampSource;
import com.carddemo.interest.policy.InterestPolicy.ZeroInterest;
import com.carddemo.interest.port.AccountStore;

/**
 * The switch tests of ticket 04: each of the seven cases is run through the calculation and the
 * file adapters with one switch on a non-legacy value, and every value of the output is compared
 * with what modernization/rules/interest/expected/&lt;case&gt;.json lists for that option. Those files
 * were derived from the rules and the inputs only, not from the code (standards S7.4). A field the
 * option does not list must keep its as-coded value, so the switch changes exactly the listed values.
 * The output files are read here by the offsets of the copybooks, not through the codec under test.
 */
class PolicySwitchFixtureTest {

    private static final Path EXPECTED = Path.of("..", "..", "modernization", "rules", "interest", "expected");
    private static final Clock FROZEN = Clock.fixed(Instant.parse("2022-07-05T10:00:00Z"), ZoneOffset.UTC);
    private static final String TRANSACTIONS = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt";
    private static final String ACCOUNTS = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt";
    private static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";
    private static final String ACCOUNTS_INPUT = "AWS.M2.CARDDEMO.ACCTDATA.PS.txt";

    /** One field of a layout: offset, length and, for a signed amount, its scale (app/cpy/CVTRA05Y.cpy, CVACT01Y.cpy). */
    private record Slice(int offset, int length, boolean signed) {
    }

    private static final Map<String, Slice> TRANSACTION_FIELDS = new LinkedHashMap<>();
    private static final Map<String, Slice> ACCOUNT_FIELDS = new LinkedHashMap<>();

    static {
        TRANSACTION_FIELDS.put("TRAN-ID", new Slice(0, 16, false));
        TRANSACTION_FIELDS.put("TRAN-TYPE-CD", new Slice(16, 2, false));
        TRANSACTION_FIELDS.put("TRAN-CAT-CD", new Slice(18, 4, false));
        TRANSACTION_FIELDS.put("TRAN-SOURCE", new Slice(22, 10, false));
        TRANSACTION_FIELDS.put("TRAN-DESC", new Slice(32, 100, false));
        TRANSACTION_FIELDS.put("TRAN-AMT", new Slice(132, 11, true));
        TRANSACTION_FIELDS.put("TRAN-MERCHANT-ID", new Slice(143, 9, false));
        TRANSACTION_FIELDS.put("TRAN-MERCHANT-NAME", new Slice(152, 50, false));
        TRANSACTION_FIELDS.put("TRAN-MERCHANT-CITY", new Slice(202, 50, false));
        TRANSACTION_FIELDS.put("TRAN-MERCHANT-ZIP", new Slice(252, 10, false));
        TRANSACTION_FIELDS.put("TRAN-CARD-NUM", new Slice(262, 16, false));
        TRANSACTION_FIELDS.put("TRAN-ORIG-TS", new Slice(278, 26, false));
        TRANSACTION_FIELDS.put("TRAN-PROC-TS", new Slice(304, 26, false));
        ACCOUNT_FIELDS.put("ACCT-ID", new Slice(0, 11, false));
        ACCOUNT_FIELDS.put("ACCT-CURR-BAL", new Slice(12, 12, true));
        ACCOUNT_FIELDS.put("ACCT-CURR-CYC-CREDIT", new Slice(78, 12, true));
        ACCOUNT_FIELDS.put("ACCT-CURR-CYC-DEBIT", new Slice(90, 12, true));
    }

    @TempDir
    Path temp;

    private static InterestPolicy policy(Rounding rounding, IdPrefix idPrefix, TimestampSource timestampSource,
            boolean updateLastAccount, NegativeBalance negativeBalance, ZeroInterest zeroInterest,
            InactiveAccounts inactiveAccounts) {
        return new InterestPolicy(rounding, idPrefix, timestampSource, updateLastAccount,
                negativeBalance, zeroInterest, inactiveAccounts);
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void allSwitches_defaultGivesTheAsCodedValuesAndTheRecordedFiles(String caseName) {
        Path output = run(caseName, InterestPolicy.legacy());

        assertMatchesExpectation(caseName, output, "none", "A");
        for (String file : List.of(TRANSACTIONS, ACCOUNTS, RUN_LOG)) {
            assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve(file)), Fixtures.text(output.resolve(file)),
                    caseName + " " + file);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intQ01_halfUpChangesExactlyTheListedValues(String caseName) {
        assertMatchesExpectation(caseName,
                run(caseName, policy(Rounding.HALF_UP, null, null, false, null, null, null)), "INT-Q01", "B");
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intQ01_halfEvenChangesExactlyTheListedValues(String caseName) {
        Path output = run(caseName, policy(Rounding.HALF_EVEN, null, null, false, null, null, null));

        assertMatchesExpectation(caseName, output, "INT-Q01", "C");
        // the ticket's switch test: only i02-tie-half-cent leaves the recorded run, on TRAN-AMT (12.36 for 12.35)
        String recorded = Fixtures.text(Fixtures.expected(caseName).resolve(TRANSACTIONS));
        if (caseName.equals("i02-tie-half-cent")) {
            assertNotEquals(recorded, Fixtures.text(output.resolve(TRANSACTIONS)));
            assertEquals("12.36", transactionsOf(output).get(0).get("TRAN-AMT"));
            assertEquals("12.35", transactionsOf(Fixtures.expected(caseName)).get(0).get("TRAN-AMT"));
        } else {
            assertEquals(recorded, Fixtures.text(output.resolve(TRANSACTIONS)), caseName);
            assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve(ACCOUNTS)),
                    Fixtures.text(output.resolve(ACCOUNTS)), caseName);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intQ02_runDatePrefixChangesExactlyTheListedValues(String caseName) {
        assertMatchesExpectation(caseName,
                run(caseName, policy(null, IdPrefix.RUN_DATE, null, false, null, null, null)), "INT-Q02", "B");
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intQ03_cycleDateChangesExactlyTheListedValues(String caseName) {
        assertMatchesExpectation(caseName,
                run(caseName, policy(null, null, TimestampSource.CYCLE_DATE, false, null, null, null)), "INT-Q03", "B");
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intQ04_updateLastAccountChangesExactlyTheListedValues(String caseName) {
        assertMatchesExpectation(caseName,
                run(caseName, policy(null, null, null, true, null, null, null)), "INT-Q04", "B");
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intQ05_skipNegativeBalanceChangesExactlyTheListedValues(String caseName) {
        assertMatchesExpectation(caseName,
                run(caseName, policy(null, null, null, false, NegativeBalance.SKIP, null, null)), "INT-Q05", "B");
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intQ06_skipZeroInterestChangesNothingInTheCases(String caseName) {
        // no case has an interest of 0.00 at a non-zero rate, so the expectation lists no value for option B
        assertMatchesExpectation(caseName,
                run(caseName, policy(null, null, null, false, null, ZeroInterest.SKIP, null)), "INT-Q06", "B");
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intH04_skipInactiveAccountsChangesNothingInTheCases(String caseName) {
        // every account of the cases has ACCT-ACTIVE-STATUS = Y, so the expectation lists no value for option B
        assertMatchesExpectation(caseName,
                run(caseName, policy(null, null, null, false, null, null, InactiveAccounts.SKIP)), "INT-H04", "B");
    }

    /**
     * One run on the file adapters, as calcInterestStep does it: read an account, calculate, write its
     * transactions, post its update. The active flag (byte 12 of the account record, CVACT01Y.cpy:6)
     * comes from the file adapter (codec/AccountRecord.decode).
     */
    private Path run(String caseName, InterestPolicy policy) {
        Path output = temp.resolve(caseName).resolve("actual");
        FileRunWorkspace workspace = FileRunWorkspace.open(Fixtures.input(caseName), output, "switch-test");
        AccountStore accounts = workspace.accountStore();
        InterestCalculator calculator = new InterestCalculator(accounts, workspace.cardLookup(), workspace.rateLookup(),
                workspace.runLog(), policy, FROZEN, new TransactionCounter(), Fixtures.PARM_DATE);

        workspace.runLog().line("START OF EXECUTION OF PROGRAM CBACT04C");
        Optional<AccountBalances> item = workspace.balanceSource().nextAccount();
        while (item.isPresent()) {
            AccountInterest result = calculator.calculate(item.get());
            result.transactions().forEach(workspace.transactionSink()::write);
            result.newBalance().ifPresent(balance -> accounts.postInterest(result.accountId(), balance));
            item = workspace.balanceSource().nextAccount();
        }
        workspace.runLog().line("END OF EXECUTION OF PROGRAM CBACT04C");
        workspace.commit(0);
        return output;
    }

    @SuppressWarnings("unchecked")
    private void assertMatchesExpectation(String caseName, Path output, String rule, String option) {
        Map<String, Object> expectation = JsonPath.parse(Fixtures.text(EXPECTED.resolve(caseName + ".json"))).read("$");

        List<Map<String, String>> expectedTransactions = new ArrayList<>();
        for (Map<String, Object> balance : (List<Map<String, Object>>) expectation.get("balances")) {
            if (Boolean.TRUE.equals(valueFor(balance.get("interestTransactionWritten"), rule, option))) {
                Map<String, String> transaction = new LinkedHashMap<>();
                TRANSACTION_FIELDS.keySet().forEach(field ->
                        transaction.put(field, (String) valueFor(balance.get(field), rule, option)));
                expectedTransactions.add(transaction);
            }
        }
        assertEquals(expectedTransactions, transactionsOf(output), caseName + " transactions, " + rule + " option " + option);

        List<Map<String, String>> expectedAccounts = new ArrayList<>();
        for (Map<String, Object> account : (List<Map<String, Object>>) expectation.get("accounts")) {
            Map<String, String> fields = new LinkedHashMap<>();
            ACCOUNT_FIELDS.keySet().forEach(field -> fields.put(field, (String) valueFor(account.get(field), rule, option)));
            expectedAccounts.add(fields);
        }
        assertEquals(expectedAccounts, records(output.resolve(ACCOUNTS), ACCOUNT_FIELDS),
                caseName + " accounts, " + rule + " option " + option);

        Map<String, Object> sysout = (Map<String, Object>) ((Map<String, Object>) expectation.get("run")).get("sysout");
        // the expectation gives each line without the trailing spaces of the 50-byte record echo
        assertEquals(List.copyOf((List<String>) sysout.get("value")),
                Fixtures.lines(output.resolve(RUN_LOG)).stream().map(String::stripTrailing).toList(), caseName + " run log");
    }

    /**
     * The value the expectation gives a field under one option of one rule: the candidate of that
     * option when the field lists the rule, otherwise the as-coded (or determined) value.
     */
    @SuppressWarnings("unchecked")
    private static Object valueFor(Object entry, String rule, String option) {
        if (!(entry instanceof Map)) {
            return entry;
        }
        Map<String, Object> field = (Map<String, Object>) entry;
        if (!field.containsKey("candidates")) {
            return field.get("value");
        }
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) field.get("candidates");
        return candidates.stream()
                .filter(candidate -> rule.equals(candidate.get("rule")) && option.equals(candidate.get("option")))
                .findFirst()
                .or(() -> candidates.stream().filter(candidate -> Boolean.TRUE.equals(candidate.get("asCoded"))).findFirst())
                .orElseThrow()
                .get("value");
    }

    private static List<Map<String, String>> transactionsOf(Path output) {
        return records(output.resolve(TRANSACTIONS), TRANSACTION_FIELDS);
    }

    private static List<Map<String, String>> records(Path file, Map<String, Slice> layout) {
        List<Map<String, String>> records = new ArrayList<>();
        for (String line : Fixtures.lines(file)) {
            String image = String.format("%-350s", line);
            Map<String, String> fields = new LinkedHashMap<>();
            layout.forEach((name, slice) -> {
                String text = image.substring(slice.offset(), slice.offset() + slice.length());
                fields.put(name, slice.signed() ? amount(text) : text.stripTrailing());
            });
            records.add(fields);
        }
        return records;
    }

    /** A zoned amount with two decimals and an overpunched sign in its last byte: { A-I plus, } J-R minus. */
    private static String amount(String zoned) {
        char last = zoned.charAt(zoned.length() - 1);
        int plus = "{ABCDEFGHI".indexOf(last);
        int minus = "}JKLMNOPQR".indexOf(last);
        if (plus < 0 && minus < 0) {
            throw new AssertionError("no overpunched sign in " + zoned);
        }
        BigDecimal value = new BigDecimal(zoned.substring(0, zoned.length() - 1) + Math.max(plus, minus)).movePointLeft(2);
        return (minus >= 0 ? value.negate() : value).toPlainString();
    }
}
