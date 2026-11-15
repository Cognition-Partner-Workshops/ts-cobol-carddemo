package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.batch.core.ExitStatus;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.policy.InterestPolicy;
import com.carddemo.interest.policy.InterestPolicy.IdPrefix;
import com.carddemo.interest.policy.InterestPolicy.InactiveAccounts;
import com.carddemo.interest.policy.InterestPolicy.NegativeBalance;
import com.carddemo.interest.policy.InterestPolicy.Rounding;
import com.carddemo.interest.policy.InterestPolicy.TimestampSource;
import com.carddemo.interest.policy.InterestPolicy.ZeroInterest;

/**
 * The switch test of ticket 04: every value of every switch of the calculation, on each of the
 * seven cases, gives exactly the values modernization/rules/interest/expected/&lt;case&gt;.json lists
 * for that option - the option's candidate where the file names one for the rule, the as-coded
 * value everywhere else. The expectations were derived from the rulebook and the inputs, not from
 * this code (standards S7.4); they are read here and never written.
 *
 * <p>Compared per case: the run log, per balance whether a transaction is written and its thirteen
 * fields, and the three amount fields of every account. rateGroup and ratePercent of the
 * expectations are not outputs of the job and are not compared. A balance has no key on the
 * transaction file, so the transactions are matched to the balances in key order (INT-R01).
 */
class PolicySwitchExpectationsTest {

    private static final Path EXPECTATIONS = Path.of("..", "..", "modernization", "rules", "interest", "expected");
    private static final List<String> CASES = List.of("i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped",
            "i04-default-group-fallback", "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding");

    /** One value of one switch, with the rule and option of the register (rules.yaml, options[].set). */
    private record SwitchValue(String rule, String option, String setting, InterestPolicy policy) {
    }

    private static final List<SwitchValue> SWITCH_VALUES = List.of(
            new SwitchValue("INT-Q01", "A", "rounding=TRUNCATE", policy(Rounding.TRUNCATE, null, null, false, null, null, null)),
            new SwitchValue("INT-Q01", "B", "rounding=HALF_UP", policy(Rounding.HALF_UP, null, null, false, null, null, null)),
            new SwitchValue("INT-Q01", "C", "rounding=HALF_EVEN", policy(Rounding.HALF_EVEN, null, null, false, null, null, null)),
            new SwitchValue("INT-Q02", "A", "idPrefix=PARM", policy(null, IdPrefix.PARM, null, false, null, null, null)),
            new SwitchValue("INT-Q02", "B", "idPrefix=RUN_DATE", policy(null, IdPrefix.RUN_DATE, null, false, null, null, null)),
            new SwitchValue("INT-Q03", "A", "timestampSource=RUN_CLOCK",
                    policy(null, null, TimestampSource.RUN_CLOCK, false, null, null, null)),
            new SwitchValue("INT-Q03", "B", "timestampSource=CYCLE_DATE",
                    policy(null, null, TimestampSource.CYCLE_DATE, false, null, null, null)),
            new SwitchValue("INT-Q04", "A", "updateLastAccount=false", policy(null, null, null, false, null, null, null)),
            new SwitchValue("INT-Q04", "B", "updateLastAccount=true", policy(null, null, null, true, null, null, null)),
            new SwitchValue("INT-Q05", "A", "negativeBalance=CHARGE",
                    policy(null, null, null, false, NegativeBalance.CHARGE, null, null)),
            new SwitchValue("INT-Q05", "B", "negativeBalance=SKIP",
                    policy(null, null, null, false, NegativeBalance.SKIP, null, null)),
            new SwitchValue("INT-Q06", "A", "zeroInterest=WRITE", policy(null, null, null, false, null, ZeroInterest.WRITE, null)),
            new SwitchValue("INT-Q06", "B", "zeroInterest=SKIP", policy(null, null, null, false, null, ZeroInterest.SKIP, null)),
            new SwitchValue("INT-H04", "A", "inactiveAccounts=CHARGE",
                    policy(null, null, null, false, null, null, InactiveAccounts.CHARGE)),
            new SwitchValue("INT-H04", "B", "inactiveAccounts=SKIP",
                    policy(null, null, null, false, null, null, InactiveAccounts.SKIP)));

    private static final List<String> TRANSACTION_FIELDS = List.of("TRAN-ID", "TRAN-TYPE-CD", "TRAN-CAT-CD",
            "TRAN-SOURCE", "TRAN-DESC", "TRAN-AMT", "TRAN-MERCHANT-ID", "TRAN-MERCHANT-NAME", "TRAN-MERCHANT-CITY",
            "TRAN-MERCHANT-ZIP", "TRAN-CARD-NUM", "TRAN-ORIG-TS", "TRAN-PROC-TS");
    private static final List<String> ACCOUNT_FIELDS = List.of("ACCT-CURR-BAL", "ACCT-CURR-CYC-CREDIT", "ACCT-CURR-CYC-DEBIT");

    @TempDir
    Path temp;

    private static InterestPolicy policy(Rounding rounding, IdPrefix idPrefix, TimestampSource timestampSource,
            boolean updateLastAccount, NegativeBalance negativeBalance, ZeroInterest zeroInterest,
            InactiveAccounts inactiveAccounts) {
        return new InterestPolicy(rounding, idPrefix, timestampSource, updateLastAccount, negativeBalance, zeroInterest,
                inactiveAccounts);
    }

    static Stream<Arguments> everySwitchValueOnEveryCase() {
        return CASES.stream().flatMap(caseName -> SWITCH_VALUES.stream().map(value -> Arguments.of(caseName, value)));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("everySwitchValueOnEveryCase")
    void switchValue_changesExactlyTheValuesTheExpectationLists(String caseName, SwitchValue value) throws JSONException {
        JSONObject expectation = new JSONObject(Fixtures.text(EXPECTATIONS.resolve(caseName + ".json")));
        Map<String, String> expected = expectedValues(expectation, value.rule(), value.option());
        Map<String, String> asCoded = expectedValues(expectation, "", "");

        Path output = temp.resolve("actual");
        assertEquals(ExitStatus.COMPLETED, FixtureRun.run(Fixtures.input(caseName), output, value.policy()));
        Map<String, String> actual = actualValues(expectation, output);

        List<String> listed = expected.keySet().stream().filter(path -> !expected.get(path).equals(asCoded.get(path))).toList();
        List<String> changed = actual.keySet().stream().filter(path -> !actual.get(path).equals(asCoded.get(path))).toList();
        // the line the evidence quotes: which values the option lists and which the job changed
        System.out.printf("switch-test %s %s option %s %s: %d values compared, expectation lists %d changed, job changed %d%s%n",
                caseName, value.rule(), value.option(), value.setting(), expected.size(), listed.size(), changed.size(),
                listed.isEmpty() ? "" : " " + listed);
        assertEquals(listed, changed, "values that differ from the as-coded outcome");
        assertEquals(expected, actual);
        assertTrue(expected.size() >= 1 + 14 + 3, "run log, one balance and one account at least");
    }

    /** The value the expectation gives each compared path for one option of one rule. */
    private static Map<String, String> expectedValues(JSONObject expectation, String rule, String option) throws JSONException {
        Map<String, String> values = new LinkedHashMap<>();
        JSONArray sysout = expectation.getJSONObject("run").getJSONObject("sysout").getJSONArray("value");
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < sysout.length(); i++) {
            lines.add(sysout.getString(i));
        }
        values.put("run/sysout", String.join("\n", lines));
        JSONArray balances = expectation.getJSONArray("balances");
        for (int i = 0; i < balances.length(); i++) {
            JSONObject balance = balances.getJSONObject(i);
            String path = "balance/" + balance.getString("key") + "/";
            values.put(path + "interestTransactionWritten", chosen(balance.getJSONObject("interestTransactionWritten"), rule, option));
            for (String field : TRANSACTION_FIELDS) {
                values.put(path + field, chosen(balance.getJSONObject(field), rule, option));
            }
        }
        JSONArray accounts = expectation.getJSONArray("accounts");
        for (int i = 0; i < accounts.length(); i++) {
            JSONObject account = accounts.getJSONObject(i);
            for (String field : ACCOUNT_FIELDS) {
                values.put("account/" + account.getString("ACCT-ID") + "/" + field, chosen(account.getJSONObject(field), rule, option));
            }
        }
        return values;
    }

    /** A determined value, or of an open one the candidate of the option, or the as-coded candidate. */
    private static String chosen(JSONObject node, String rule, String option) throws JSONException {
        if (node.has("value")) {
            return String.valueOf(node.get("value"));
        }
        JSONArray candidates = node.getJSONArray("candidates");
        String asCoded = null;
        for (int i = 0; i < candidates.length(); i++) {
            JSONObject candidate = candidates.getJSONObject(i);
            if (candidate.getString("rule").equals(rule) && candidate.getString("option").equals(option)) {
                return String.valueOf(candidate.get("value"));
            }
            if (candidate.getBoolean("asCoded")) {
                asCoded = String.valueOf(candidate.get("value"));
            }
        }
        return asCoded;
    }

    /** The same paths read from the files the job wrote, read by the offsets of CVTRA05Y and CVACT01Y. */
    private static Map<String, String> actualValues(JSONObject expectation, Path output) throws JSONException {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("run/sysout", String.join("\n", Fixtures.lines(output.resolve("sysout/INTCALC.STEP15.txt")).stream()
                .map(String::stripTrailing).toList()));

        List<String> transactions = Fixtures.lines(output.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt")).stream()
                .map(line -> padded(line, 350)).toList();
        int next = 0;
        JSONArray balances = expectation.getJSONArray("balances");
        for (int i = 0; i < balances.length(); i++) {
            String key = balances.getJSONObject(i).getString("key");
            String path = "balance/" + key + "/";
            // key is account/type/category: a transaction goes to the first balance of the account it names
            // that has none yet; a wrong assignment inside an account shows as a TRAN-AMT difference
            boolean written = next < transactions.size()
                    && transactions.get(next).substring(32, 56).equals("Int. for a/c " + key.substring(0, 11));
            values.put(path + "interestTransactionWritten", String.valueOf(written));
            String image = written ? transactions.get(next++) : null;
            // app/cpy/CVTRA05Y.cpy:5-17
            values.put(path + "TRAN-ID", text(image, 0, 16));
            values.put(path + "TRAN-TYPE-CD", text(image, 16, 2));
            values.put(path + "TRAN-CAT-CD", text(image, 18, 4));
            values.put(path + "TRAN-SOURCE", text(image, 22, 10));
            values.put(path + "TRAN-DESC", text(image, 32, 100));
            values.put(path + "TRAN-AMT", image == null ? "null" : amount(image, 132, 11));
            values.put(path + "TRAN-MERCHANT-ID", text(image, 143, 9));
            values.put(path + "TRAN-MERCHANT-NAME", text(image, 152, 50));
            values.put(path + "TRAN-MERCHANT-CITY", text(image, 202, 50));
            values.put(path + "TRAN-MERCHANT-ZIP", text(image, 252, 10));
            values.put(path + "TRAN-CARD-NUM", text(image, 262, 16));
            values.put(path + "TRAN-ORIG-TS", text(image, 278, 26));
            values.put(path + "TRAN-PROC-TS", text(image, 304, 26));
        }
        assertEquals(transactions.size(), next, "every transaction written belongs to a balance of the expectation");

        Map<String, String> accountImages = new LinkedHashMap<>();
        for (String line : Fixtures.lines(output.resolve("datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt"))) {
            String image = padded(line, 300);
            accountImages.put(image.substring(0, 11), image);
        }
        JSONArray accounts = expectation.getJSONArray("accounts");
        for (int i = 0; i < accounts.length(); i++) {
            String accountId = accounts.getJSONObject(i).getString("ACCT-ID");
            String image = accountImages.get(accountId);
            // app/cpy/CVACT01Y.cpy:7, 13, 14
            values.put("account/" + accountId + "/ACCT-CURR-BAL", amount(image, 12, 12));
            values.put("account/" + accountId + "/ACCT-CURR-CYC-CREDIT", amount(image, 78, 12));
            values.put("account/" + accountId + "/ACCT-CURR-CYC-DEBIT", amount(image, 90, 12));
        }
        return values;
    }

    private static String text(String image, int offset, int length) {
        return image == null ? "null" : image.substring(offset, offset + length).stripTrailing();
    }

    /** A fixture line with the trailing spaces of its record put back (tools/parity/record.py:51). */
    private static String padded(String line, int recordLength) {
        return line + " ".repeat(Math.max(0, recordLength - line.length()));
    }

    /**
     * A zoned amount with two decimals, read by hand and not with the codec under test: the last
     * character carries digit and sign, "{ABCDEFGHI" plus 0-9 and "}JKLMNOPQR" minus 0-9 (data-map rule E2).
     */
    private static String amount(String image, int offset, int length) {
        String zoned = image.substring(offset, offset + length);
        char last = zoned.charAt(length - 1);
        int plus = "{ABCDEFGHI".indexOf(last);
        int minus = "}JKLMNOPQR".indexOf(last);
        assertTrue(plus >= 0 || minus >= 0, "overpunch sign of " + zoned);
        BigDecimal value = new BigDecimal(zoned.substring(0, length - 1) + Math.max(plus, minus)).movePointLeft(2);
        return (minus >= 0 ? value.negate() : value).toPlainString();
    }
}
