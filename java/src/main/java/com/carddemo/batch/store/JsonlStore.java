package com.carddemo.batch.store;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Moves canonical JSONL records in and out of the database. The JSONL files come from the
 * shared loader in spec/harness, so this class never decodes a zoned decimal itself.
 */
@Component
public class JsonlStore {

    private static final TypeReference<Map<String, String>> RECORD = new TypeReference<>() { };

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonlStore(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
        this.jdbc = jdbc;
        this.named = named;
    }

    public int importFile(Datasets dataset, Path file) {
        Map<String, Integer> types = columnTypes(dataset);
        List<String> lines = readLines(file);
        long seq = 0;
        for (String line : lines) {
            Map<String, String> rec = parse(line);
            Map<String, Object> params = new LinkedHashMap<>();
            if (types.containsKey("seq")) {
                params.put("seq", ++seq);
            }
            for (Map.Entry<String, String> e : rec.entrySet()) {
                Integer type = types.get(e.getKey());
                if (type != null) {
                    params.put(e.getKey(), isNumeric(type) ? new BigDecimal(e.getValue()) : e.getValue());
                }
            }
            insert(dataset, params);
        }
        return lines.size();
    }

    public void insert(Datasets dataset, Map<String, Object> params) {
        String cols = String.join(", ", params.keySet());
        String vals = params.keySet().stream().map(k -> ":" + k).collect(Collectors.joining(", "));
        named.update("INSERT INTO " + dataset.table() + " (" + cols + ") VALUES (" + vals + ")", params);
    }

    public int exportFile(Datasets dataset, Path file) {
        List<String> out = new ArrayList<>();
        jdbc.query("SELECT * FROM " + dataset.table() + " ORDER BY " + dataset.orderBy(), rs -> {
            ResultSetMetaData md = rs.getMetaData();
            Map<String, String> rec = new TreeMap<>();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                String name = md.getColumnLabel(i).toLowerCase();
                if (name.equals("seq")) {
                    continue;
                }
                rec.put(name, canonical(rs.getObject(i), md.getColumnType(i), md.getScale(i)));
            }
            out.add(write(rec));
        });
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(file)) {
                for (String line : out) {
                    w.write(line);
                    w.write("\n");
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.size();
    }

    /** Same text form as spec/harness/records.py canonical(): fixed scale, no negative zero, no trailing blanks. */
    static String canonical(Object value, int sqlType, int scale) {
        if (isNumeric(sqlType)) {
            BigDecimal d = value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
            d = d.setScale(Math.max(scale, 0));
            if (d.signum() == 0) {
                d = d.abs();
            }
            return d.toPlainString();
        }
        return value == null ? "" : value.toString().stripTrailing();
    }

    private Map<String, Integer> columnTypes(Datasets dataset) {
        Map<String, Integer> types = new LinkedHashMap<>();
        jdbc.query("SELECT * FROM " + dataset.table() + " WHERE 1 = 0", rs -> {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                types.put(md.getColumnLabel(i).toLowerCase(), md.getColumnType(i));
            }
            return null;
        });
        return types;
    }

    private static boolean isNumeric(int sqlType) {
        return sqlType == Types.NUMERIC || sqlType == Types.DECIMAL || sqlType == Types.INTEGER || sqlType == Types.BIGINT;
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file).stream().filter(l -> !l.isBlank()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, String> parse(String line) {
        try {
            return mapper.readValue(line, RECORD);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String write(Map<String, String> rec) {
        try {
            return mapper.writeValueAsString(rec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
