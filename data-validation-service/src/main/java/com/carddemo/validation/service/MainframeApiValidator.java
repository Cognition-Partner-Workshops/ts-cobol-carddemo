package com.carddemo.validation.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.carddemo.validation.config.ValidationProperties.MainframeApiConfig;
import com.carddemo.validation.model.MainframeApiValidationResult;
import com.carddemo.validation.model.TableValidationResult.FieldDiff;
import com.carddemo.validation.model.TableValidationResult.RecordDiff;
import com.carddemo.validation.config.ValidationProperties;

/**
 * Compares mainframe output files against microservice API responses.
 *
 * <p>The mainframe file is pipe-delimited with a header row. The API
 * response is expected to return a JSON array of objects.</p>
 */
@Component
public class MainframeApiValidator {

    private static final Logger log = LoggerFactory.getLogger(MainframeApiValidator.class);
    private static final String DEFAULT_DELIMITER = "\\|";

    private final RestTemplate restTemplate;
    private final ValidationProperties properties;

    public MainframeApiValidator(ValidationProperties properties) {
        this.restTemplate = new RestTemplate();
        this.properties = properties;
    }

    /**
     * Compare a mainframe output file against the specified API endpoint.
     */
    public MainframeApiValidationResult validate(MainframeApiConfig config) {
        MainframeApiValidationResult result = new MainframeApiValidationResult();
        result.setComparisonName(config.getName());
        result.setMainframeFilePath(config.getMainframeFilePath());
        result.setApiUrl(config.getApiUrl());

        try {
            // Read mainframe file
            List<Map<String, String>> fileRecords = readFlatFile(config.getMainframeFilePath());
            result.setFileRecordCount(fileRecords.size());

            // Call API
            List<Map<String, Object>> apiRecords = callApi(config);
            result.setApiRecordCount(apiRecords.size());

            // Record count comparison
            result.setRecordCountMatch(fileRecords.size() == apiRecords.size());

            // Sample record diff
            List<String> keyFields = config.getKeyFields();
            if (keyFields != null && !keyFields.isEmpty()) {
                int limit = properties.getMaxSampleRecords();
                List<RecordDiff> diffs = compareRecords(
                        fileRecords.subList(0, Math.min(limit, fileRecords.size())),
                        apiRecords.subList(0, Math.min(limit, apiRecords.size())),
                        keyFields);
                result.setSampleDiffs(diffs);
            }

            if (!result.isRecordCountMatch() || !result.getSampleDiffs().isEmpty()) {
                result.markFailed();
            }

            log.info("Mainframe-API comparison '{}': file={} api={} match={}",
                    config.getName(), fileRecords.size(), apiRecords.size(),
                    result.isRecordCountMatch());

        } catch (Exception e) {
            result.addError("Mainframe-API comparison failed: " + e.getMessage());
            log.error("Mainframe-API comparison error for '{}'", config.getName(), e);
        }

        return result;
    }

    // -- Private helpers ------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> callApi(MainframeApiConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpMethod method = "POST".equalsIgnoreCase(config.getHttpMethod())
                ? HttpMethod.POST
                : HttpMethod.GET;

        HttpEntity<String> entity;
        if (config.getRequestBody() != null && !config.getRequestBody().isBlank()) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            entity = new HttpEntity<>(config.getRequestBody(), headers);
        } else {
            entity = new HttpEntity<>(headers);
        }

        ResponseEntity<List> response = restTemplate.exchange(
                config.getApiUrl(), method, entity, List.class);

        List<?> body = response.getBody();
        if (body == null) {
            return List.of();
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (Object item : body) {
            if (item instanceof Map) {
                records.add((Map<String, Object>) item);
            }
        }
        return records;
    }

    private List<Map<String, String>> readFlatFile(String filePath) throws IOException {
        String delimiter = System.getenv("MAINFRAME_FILE_DELIMITER");
        if (delimiter == null || delimiter.isBlank()) {
            delimiter = DEFAULT_DELIMITER;
        }

        List<Map<String, String>> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(
                Paths.get(filePath), StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return records;
            }
            String[] headers = headerLine.split(delimiter);
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim();
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(delimiter, -1);
                Map<String, String> record = new LinkedHashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    record.put(headers[i], values[i].trim());
                }
                records.add(record);
            }
        }
        return records;
    }

    private List<RecordDiff> compareRecords(
            List<Map<String, String>> fileRecords,
            List<Map<String, Object>> apiRecords,
            List<String> keyFields) {

        // Index API records by key
        Map<String, Map<String, Object>> apiIndex = new LinkedHashMap<>();
        for (Map<String, Object> row : apiRecords) {
            String key = buildKey(row, keyFields);
            apiIndex.put(key, row);
        }

        List<RecordDiff> diffs = new ArrayList<>();
        for (Map<String, String> fileRec : fileRecords) {
            String key = buildKeyFromStrings(fileRec, keyFields);
            Map<String, Object> apiRow = apiIndex.get(key);

            if (apiRow == null) {
                RecordDiff diff = new RecordDiff();
                diff.setPrimaryKey(buildPkMap(fileRec, keyFields));
                Map<String, FieldDiff> fieldDiffs = new LinkedHashMap<>();
                fieldDiffs.put("_record", new FieldDiff("EXISTS_IN_FILE", "MISSING_IN_API"));
                diff.setFieldDiffs(fieldDiffs);
                diffs.add(diff);
                continue;
            }

            Map<String, FieldDiff> fieldDiffs = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : fileRec.entrySet()) {
                if (keyFields.stream().anyMatch(k -> k.equalsIgnoreCase(entry.getKey()))) {
                    continue;
                }
                String fileVal = entry.getValue();
                Object apiVal = findCaseInsensitive(apiRow, entry.getKey());
                String apiStr = apiVal == null ? null : apiVal.toString();
                if (!Objects.equals(fileVal, apiStr)) {
                    fieldDiffs.put(entry.getKey(), new FieldDiff(fileVal, apiStr));
                }
            }
            if (!fieldDiffs.isEmpty()) {
                RecordDiff diff = new RecordDiff();
                diff.setPrimaryKey(buildPkMap(fileRec, keyFields));
                diff.setFieldDiffs(fieldDiffs);
                diffs.add(diff);
            }
        }
        return diffs;
    }

    private String buildKey(Map<String, Object> row, List<String> keyFields) {
        StringBuilder sb = new StringBuilder();
        for (String field : keyFields) {
            Object val = findCaseInsensitive(row, field);
            sb.append(val == null ? "NULL" : val.toString()).append("|");
        }
        return sb.toString();
    }

    private String buildKeyFromStrings(Map<String, String> row, List<String> keyFields) {
        StringBuilder sb = new StringBuilder();
        for (String field : keyFields) {
            String val = findCaseInsensitiveStr(row, field);
            sb.append(val == null ? "NULL" : val).append("|");
        }
        return sb.toString();
    }

    private Map<String, Object> buildPkMap(Map<String, String> row, List<String> keyFields) {
        Map<String, Object> pk = new LinkedHashMap<>();
        for (String field : keyFields) {
            pk.put(field, findCaseInsensitiveStr(row, field));
        }
        return pk;
    }

    private Object findCaseInsensitive(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val != null) {
            return val;
        }
        return map.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private String findCaseInsensitiveStr(Map<String, String> map, String key) {
        String val = map.get(key);
        if (val != null) {
            return val;
        }
        return map.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
