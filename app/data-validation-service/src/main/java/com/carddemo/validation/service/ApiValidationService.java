package com.carddemo.validation.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.carddemo.validation.config.ValidationProperties;
import com.carddemo.validation.config.ValidationProperties.TablePairConfig;
import com.carddemo.validation.model.TableValidationResult;
import com.carddemo.validation.model.ValidationStatus;

/**
 * Validates mainframe output files against the modernized microservice API.
 *
 * <p>Calls the configured API endpoint for each table pair and compares
 * the API response record count and content against the mainframe flat file.
 */
@Service
public class ApiValidationService {

    private static final Logger log = LoggerFactory.getLogger(ApiValidationService.class);

    private final ValidationProperties properties;
    private final RestTemplate restTemplate;

    public ApiValidationService(ValidationProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Validate mainframe output file against the microservice API response.
     */
    public void validateFileToApi(TablePairConfig pair, TableValidationResult result) {
        String mainframeFile = pair.getMainframeFile();
        String apiEndpoint = pair.getApiEndpoint();

        if (mainframeFile == null || mainframeFile.isBlank()) {
            result.setFileToApiStatus(ValidationStatus.SKIPPED);
            result.setFileToApiDetail("No mainframe file configured");
            return;
        }

        if (apiEndpoint == null || apiEndpoint.isBlank()) {
            result.setFileToApiStatus(ValidationStatus.SKIPPED);
            result.setFileToApiDetail("No API endpoint configured");
            return;
        }

        Path filePath = Paths.get(properties.getMainframeOutputDir(), mainframeFile);
        if (!Files.exists(filePath)) {
            result.setFileToApiStatus(ValidationStatus.SKIPPED);
            result.setFileToApiDetail("Mainframe file not found: " + filePath);
            log.warn("FILE_TO_API [{}]: file not found – {}", pair.getName(), filePath);
            return;
        }

        try {
            // Count records in the mainframe file
            long fileRowCount = countFileRecords(filePath);

            // Call the microservice API
            String apiUrl = properties.getApiBaseUrl() + apiEndpoint;
            List<?> apiRecords = fetchApiRecords(apiUrl);
            long apiRecordCount = apiRecords.size();

            result.setFileRowCount(fileRowCount);
            result.setApiRecordCount(apiRecordCount);

            boolean countMatch = fileRowCount == apiRecordCount;

            if (countMatch) {
                // Perform content-level spot check
                List<String> fileLines = readSampleLines(filePath, properties.getMaxSampleRecords());
                int discrepancies = compareFileToApiContent(fileLines, apiRecords);

                if (discrepancies == 0) {
                    result.setFileToApiStatus(ValidationStatus.PASS);
                    result.setFileToApiDetail(String.format(
                            "File records: %d, API records: %d – counts and sample content match",
                            fileRowCount, apiRecordCount));
                } else {
                    result.setFileToApiStatus(ValidationStatus.FAIL);
                    result.setFileToApiDetail(String.format(
                            "File records: %d, API records: %d – counts match but %d sample discrepancies",
                            fileRowCount, apiRecordCount, discrepancies));
                }
            } else {
                result.setFileToApiStatus(ValidationStatus.FAIL);
                result.setFileToApiDetail(String.format(
                        "File records: %d, API records: %d – count mismatch",
                        fileRowCount, apiRecordCount));
            }

            log.info("FILE_TO_API [{}]: fileRows={} apiRows={} status={}",
                    pair.getName(), fileRowCount, apiRecordCount, result.getFileToApiStatus());

        } catch (IOException e) {
            log.error("FILE_TO_API [{}]: I/O error – {}", pair.getName(), e.getMessage(), e);
            result.setFileToApiStatus(ValidationStatus.ERROR);
            result.addError("File-to-API I/O error: " + e.getMessage());
        } catch (RestClientException e) {
            log.error("FILE_TO_API [{}]: API error – {}", pair.getName(), e.getMessage(), e);
            result.setFileToApiStatus(ValidationStatus.ERROR);
            result.addError("File-to-API REST error: " + e.getMessage());
        }
    }

    /**
     * Fetch all records from the given API endpoint.
     */
    private List<?> fetchApiRecords(String apiUrl) {
        ResponseEntity<List> response = restTemplate.getForEntity(apiUrl, List.class);
        List<?> body = response.getBody();
        return body != null ? body : List.of();
    }

    /**
     * Compare sample file lines against API response content.
     *
     * @return number of discrepancies found
     */
    private int compareFileToApiContent(List<String> fileLines, List<?> apiRecords) {
        int discrepancies = 0;
        int compareCount = Math.min(fileLines.size(), apiRecords.size());

        for (int i = 0; i < compareCount; i++) {
            String fileLine = normalizeForComparison(fileLines.get(i));
            String apiRecord = normalizeForComparison(apiRecords.get(i).toString());

            if (!fileLine.equals(apiRecord)) {
                discrepancies++;
            }
        }
        return discrepancies;
    }

    private long countFileRecords(Path filePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .count();
        }
    }

    private List<String> readSampleLines(Path filePath, int maxLines) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null && lines.size() < maxLines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
        }
        return lines;
    }

    private String normalizeForComparison(String value) {
        return value.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
