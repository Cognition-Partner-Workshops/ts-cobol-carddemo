package com.carddemo.interest.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.carddemo.interest.FileCaseRun;
import com.carddemo.interest.Fixtures;
import com.carddemo.interest.PolicySwitches;
import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.DecodedRecord;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.LayoutField;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.codec.TransactionRecord;
import com.carddemo.interest.policy.InterestPolicy;
import com.carddemo.interest.policy.InterestPolicy.IdPrefix;
import com.carddemo.interest.policy.InterestPolicy.NegativeBalance;
import com.carddemo.interest.policy.InterestPolicy.Rounding;
import com.carddemo.interest.policy.InterestPolicy.TimestampSource;

/**
 * Ticket 04, switch test: each switch changes exactly the values that
 * modernization/rules/interest/expected/&lt;case&gt;.json lists for that option. The expected side is
 * that file (derived from rules.yaml and the case's input, not from this code): for one rule and
 * one option, a field holds the option's value where the file lists one and the as-coded or
 * determined value everywhere else. Every field the file names is compared, on every case; what
 * the file does not name (the other account fields and the run log) must be as on the input and
 * as determined.
 */
class PolicySwitchExpectationsTest {

    private static final Path EXPECTATIONS = Path.of("..", "..", "modernization", "rules", "interest", "expected");
    private static final List<String> CASES = List.of("i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped",
            "i04-default-group-fallback", "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding");
    private static final RecordSource SOURCE = RecordSource.built("test");

    @TempDir
    Path temp;

    /** A rule, an option of it in rules.yaml, and the policy that selects it. */
    private record Option(String rule, String option, InterestPolicy policy) {
    }

    private static final List<Option> OPTIONS = List.of(
            new Option("INT-Q01", "A", InterestPolicy.legacy()),
            new Option("INT-Q01", "B", PolicySwitches.rounding(Rounding.HALF_UP)),
            new Option("INT-Q01", "C", PolicySwitches.rounding(Rounding.HALF_EVEN)),
            new Option("INT-Q02", "B", PolicySwitches.idPrefix(IdPrefix.RUN_DATE)),
            new Option("INT-Q03", "B", PolicySwitches.timestampSource(TimestampSource.CYCLE_DATE)),
            new Option("INT-Q04", "B", PolicySwitches.updateLastAccount(true)),
            new Option("INT-Q05", "B", PolicySwitches.negativeBalance(NegativeBalance.SKIP)));

    static Stream<Arguments> caseAndOption() {
        return CASES.stream().flatMap(caseName -> OPTIONS.stream()
                .map(option -> Arguments.of(caseName, option.rule(), option.option(), option.policy())));
    }

    @ParameterizedTest(name = "{0} {1} option {2}")
    @MethodSource("caseAndOption")
    void switch_changesExactlyTheListedValues(String caseName, String rule, String option, InterestPolicy policy)
            throws IOException, JSONException {
        JSONObject expected = new JSONObject(Files.readString(EXPECTATIONS.resolve(caseName + ".json")));

        Path output = FileCaseRun.run(Fixtures.input(caseName), temp.resolve("actual"), policy);

        // transactions: one per balance the file says is written, in the order of the balances
        List<DecodedRecord> transactions = decodeAll(output.resolve(FileRunWorkspace.TRANSACTIONS_DATASET), TransactionRecord.LAYOUT);
        int written = 0;
        JSONArray balances = expected.getJSONArray("balances");
        for (int b = 0; b < balances.length(); b++) {
            JSONObject balance = balances.getJSONObject(b);
            String key = balance.getString("key");
            if (!Boolean.TRUE.equals(valueUnder(balance.getJSONObject("interestTransactionWritten"), rule, option))) {
                continue;
            }
            assertTrue(written < transactions.size(), key + ": a transaction is expected");
            DecodedRecord transaction = transactions.get(written++);
            for (LayoutField field : TransactionRecord.LAYOUT.fields()) {
                if (balance.has(field.name())) {
                    assertEquals(valueUnder(balance.getJSONObject(field.name()), rule, option), shown(transaction, field),
                            key + " " + field.name());
                }
            }
        }
        assertEquals(written, transactions.size(), "no transaction beyond those the file lists as written");

        // accounts: the fields the file names; every other byte of the record as on the input
        List<String> accountLines = Fixtures.lines(output.resolve(FileRunWorkspace.ACCOUNTS_DATASET));
        List<String> inputLines = Fixtures.lines(Fixtures.input(caseName).resolve(FileRunWorkspace.ACCOUNTS_FILE));
        JSONArray accounts = expected.getJSONArray("accounts");
        assertEquals(accounts.length(), accountLines.size());
        for (int i = 0; i < accountLines.size(); i++) {
            JSONObject account = accounts.getJSONObject(i);
            String image = RecordImage.fromLine(accountLines.get(i), AccountRecord.LAYOUT, SOURCE);
            String inputImage = RecordImage.fromLine(inputLines.get(i), AccountRecord.LAYOUT, SOURCE);
            DecodedRecord decoded = AccountRecord.LAYOUT.decode(RecordImage.bytes(image), CodePage.FIXTURE, SOURCE);
            for (LayoutField field : AccountRecord.LAYOUT.fields()) {
                if (!account.has(field.name())) {
                    assertEquals(inputImage.substring(field.offset(), field.end()), image.substring(field.offset(), field.end()),
                            "account " + i + " " + field.name() + " is not listed and must be as on the input");
                } else if (account.get(field.name()) instanceof String id) {
                    assertEquals(id, shown(decoded, field));
                } else {
                    assertEquals(valueUnder(account.getJSONObject(field.name()), rule, option), shown(decoded, field),
                            "account " + i + " " + field.name());
                }
            }
        }

        // run log and end of the run: determined, no option changes them
        List<String> sysout = new ArrayList<>();
        JSONArray lines = expected.getJSONObject("run").getJSONObject("sysout").getJSONArray("value");
        for (int i = 0; i < lines.length(); i++) {
            sysout.add(lines.getString(i));
        }
        assertEquals(sysout, Fixtures.lines(output.resolve(FileRunWorkspace.RUN_LOG_FILE)).stream()
                .map(String::stripTrailing).toList());
        assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve(FileRunWorkspace.RETURN_CODES_FILE)),
                Fixtures.text(output.resolve(FileRunWorkspace.RETURN_CODES_FILE)));
    }

    /**
     * The value of one entry of the file for this rule and option: the option's candidate where the
     * entry lists one for the rule, else the as-coded candidate, else the determined value.
     */
    private static Object valueUnder(JSONObject entry, String rule, String option) throws JSONException {
        if (!entry.has("candidates")) {
            return entry.get("value");
        }
        Object asCoded = null;
        JSONArray candidates = entry.getJSONArray("candidates");
        for (int i = 0; i < candidates.length(); i++) {
            JSONObject candidate = candidates.getJSONObject(i);
            if (candidate.getString("rule").equals(rule) && candidate.getString("option").equals(option)) {
                return candidate.get("value");
            }
            if (candidate.optBoolean("asCoded")) {
                asCoded = candidate.get("value");
            }
        }
        return asCoded;
    }

    /** A decoded field the way the file writes it: text trimmed, amounts as 12.35, unsigned numbers with all their digits. */
    private static String shown(DecodedRecord record, LayoutField field) {
        if (field.kind() == FieldKind.TEXT) {
            return record.text(field.name());
        }
        BigDecimal number = record.number(field.name());
        return field.scale() > 0 ? number.toPlainString()
                : String.format("%0" + field.length() + "d", number.longValueExact());
    }

    private static List<DecodedRecord> decodeAll(Path file, RecordLayout layout) {
        try {
            return Fixtures.lines(file).stream()
                    .map(line -> layout.decode(RecordImage.bytes(RecordImage.fromLine(line, layout, SOURCE)), CodePage.FIXTURE, SOURCE))
                    .toList();
        } catch (UncheckedIOException e) {
            throw new AssertionError("cannot read " + file, e);
        }
    }
}
