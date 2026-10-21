package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.carddemo.batch.store.Datasets;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.yaml.snakeyaml.Yaml;

/**
 * Runs the batch path on the decoded inputs in spec/golden/input and compares every output
 * record with the frozen GnuCOBOL outputs in spec/golden. A difference passes only when an
 * entry in spec/expected-differences.yaml covers its dataset, field and record.
 */
@SpringBootTest(properties = {"carddemo.input-dir=../spec/golden/input", "carddemo.output-dir=target/parity/java"})
class FrozenCobolParityTest {

    private static final Path GOLDEN = Path.of("../spec/golden");
    private static final Path OUT = Path.of("target/parity/java");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<Datasets, List<String>> KEYS = Map.of(
            Datasets.POSTED, List.of("tran_id"), Datasets.INTEREST, List.of("tran_id"),
            Datasets.REJECTS, List.of("dalytran_id"), Datasets.ACCOUNTS, List.of("acct_id"),
            Datasets.TCATBAL, List.of("trancat_acct_id", "trancat_type_cd", "trancat_cd"));

    @Test
    void everyOutputMatchesTheCobolRunOrIsAnApprovedDifference() throws IOException {
        List<Map<String, Object>> expected = expectedDifferences();
        List<String> unexplained = new ArrayList<>();
        for (Datasets d : Datasets.OUTPUTS) {
            Map<String, Map<String, String>> cobol = keyed(d, GOLDEN.resolve(d.fileName()));
            Map<String, Map<String, String>> java = keyed(d, OUT.resolve(d.fileName()));
            for (String key : union(cobol, java)) {
                Map<String, String> c = cobol.get(key);
                Map<String, String> j = java.get(key);
                if (c == null || j == null) {
                    if (!covered(expected, d, "*", c, j)) {
                        unexplained.add(d + " " + key + " present on one side only");
                    }
                    continue;
                }
                for (String field : c.keySet()) {
                    if (!c.get(field).equals(j.get(field)) && !covered(expected, d, field, c, j)) {
                        unexplained.add(d + " " + key + " " + field + ": cobol=" + c.get(field) + " java=" + j.get(field));
                    }
                }
            }
        }
        assertThat(unexplained).as("unexplained differences from the COBOL run").isEmpty();
    }

    private static List<String> union(Map<String, ?> a, Map<String, ?> b) {
        List<String> keys = new ArrayList<>(a.keySet());
        b.keySet().stream().filter(k -> !a.containsKey(k)).forEach(keys::add);
        return keys;
    }

    @SuppressWarnings("unchecked")
    private static boolean covered(List<Map<String, Object>> entries, Datasets d, String field,
                                   Map<String, String> c, Map<String, String> j) {
        for (Map<String, Object> e : entries) {
            if (!d.fileName().equals(e.get("dataset") + ".jsonl")) {
                continue;
            }
            List<String> fields = (List<String>) e.get("fields");
            if (fields != null && !fields.contains(field) && !"*".equals(field)) {
                continue;
            }
            Map<String, Object> where = (Map<String, Object>) e.getOrDefault("where", Map.of());
            for (Map<String, String> rec : new Map[] {c, j}) {
                if (rec != null && where.entrySet().stream()
                        .allMatch(w -> String.valueOf(w.getValue()).equals(rec.get(w.getKey())))) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> expectedDifferences() throws IOException {
        Map<String, Object> doc = new Yaml().load(Files.readString(Path.of("../spec/expected-differences.yaml")));
        Object list = doc == null ? null : doc.get("expected_differences");
        return list == null ? List.of() : (List<Map<String, Object>>) list;
    }

    private static Map<String, Map<String, String>> keyed(Datasets d, Path file) throws IOException {
        Map<String, Map<String, String>> out = new HashMap<>();
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank()) {
                continue;
            }
            Map<String, String> rec = JSON.readValue(line, new TypeReference<>() { });
            out.put(String.join("|", KEYS.get(d).stream().map(rec::get).toList()), rec);
        }
        return out;
    }
}
