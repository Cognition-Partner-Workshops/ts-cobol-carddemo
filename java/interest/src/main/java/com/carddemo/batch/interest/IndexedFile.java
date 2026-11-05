package com.carddemo.batch.interest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * In-memory stand-in for a KSDS loaded by the setup jobs (IDCAMS REPRO through the harness's
 * sequential-access load), with an optional non-unique alternate index built afterwards.
 *
 * <p>The load writes records in input order with ACCESS SEQUENTIAL, so any record whose primary
 * key is not strictly greater than the previous accepted key (duplicate or out of sequence) is
 * skipped, exactly as the legacy setup does ("IDC3302I DUPLICATE KEYS SKIPPED").
 */
final class IndexedFile {
    private final int keyOffset;
    private final int keyLength;
    private final TreeMap<String, String> records = new TreeMap<>();
    private final Map<String, String> alternate = new HashMap<>();
    private int skipped;

    IndexedFile(int keyOffset, int keyLength) {
        this.keyOffset = keyOffset;
        this.keyLength = keyLength;
    }

    static IndexedFile load(List<String> recordsInInputOrder, int keyOffset, int keyLength) {
        IndexedFile file = new IndexedFile(keyOffset, keyLength);
        String last = null;
        for (String record : recordsInInputOrder) {
            String key = file.keyOf(record);
            if (last != null && key.compareTo(last) <= 0) {
                file.skipped++;
                continue;
            }
            file.records.put(key, record);
            last = key;
        }
        return file;
    }

    /** BLDINDEX for a NONUNIQUEKEY alternate index: duplicates resolve to the first record in key order. */
    IndexedFile withAlternateIndex(int altOffset, int altLength) {
        for (String record : records.values()) {
            alternate.putIfAbsent(record.substring(altOffset, altOffset + altLength), record);
        }
        return this;
    }

    String keyOf(String record) {
        return record.substring(keyOffset, keyOffset + keyLength);
    }

    String read(String key) {
        return records.get(key);
    }

    String readAlternate(String altKey) {
        return alternate.get(altKey);
    }

    void rewrite(String record) {
        records.put(keyOf(record), record);
    }

    Collection<String> inKeyOrder() {
        return new ArrayList<>(records.values());
    }

    int skipped() {
        return skipped;
    }
}
