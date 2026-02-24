package com.carddemo.transform.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Client that simulates sending mainframe-format payloads to the transformation
 * service and captures the transformed output.
 * <p>
 * In integration mode this would call the actual REST endpoint; for unit-level
 * validation we apply the expected transformation rules locally so the feature
 * files can be run without a live service.
 * <p>
 * To switch to live mode, set the system property {@code transform.base.url}
 * (e.g. {@code -Dtransform.base.url=http://localhost:8080/api/transform}).
 */
public class TransformationClient {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final String baseUrl;

    public TransformationClient() {
        this.baseUrl = System.getProperty("transform.base.url", "");
    }

    /**
     * Transforms a mainframe account record payload into a JSON response.
     *
     * @param cobolFields map of COBOL field names to raw string values
     * @return the transformed JSON as a Jackson ObjectNode
     */
    public ObjectNode transformAccount(Map<String, String> cobolFields) {
        if (!baseUrl.isEmpty()) {
            return postToService(baseUrl + "/account", cobolFields);
        }
        return applyAccountTransformRules(cobolFields);
    }

    /**
     * Transforms a mainframe customer record payload into a JSON response.
     *
     * @param cobolFields map of COBOL field names to raw string values
     * @return the transformed JSON as a Jackson ObjectNode
     */
    public ObjectNode transformCustomer(Map<String, String> cobolFields) {
        if (!baseUrl.isEmpty()) {
            return postToService(baseUrl + "/customer", cobolFields);
        }
        return applyCustomerTransformRules(cobolFields);
    }

    /**
     * Transforms a mainframe transaction record payload into a JSON response.
     *
     * @param cobolFields map of COBOL field names to raw string values
     * @return the transformed JSON as a Jackson ObjectNode
     */
    public ObjectNode transformTransaction(Map<String, String> cobolFields) {
        if (!baseUrl.isEmpty()) {
            return postToService(baseUrl + "/transaction", cobolFields);
        }
        return applyTransactionTransformRules(cobolFields);
    }

    // ── Local transformation rules (mirrors expected service behaviour) ──

    private ObjectNode applyAccountTransformRules(Map<String, String> fields) {
        ObjectNode json = MAPPER.createObjectNode();

        putNumericLong(json, "accountId", fields.get("ACCT-ID"));
        putTrimmedOrNull(json, "activeStatus", fields.get("ACCT-ACTIVE-STATUS"));
        putSignedDecimal(json, "currentBalance", fields.get("ACCT-CURR-BAL"));
        putSignedDecimal(json, "creditLimit", fields.get("ACCT-CREDIT-LIMIT"));
        putSignedDecimal(json, "cashCreditLimit", fields.get("ACCT-CASH-CREDIT-LIMIT"));
        putDateOrNull(json, "openDate", fields.get("ACCT-OPEN-DATE"));
        putDateOrNull(json, "expirationDate", fields.get("ACCT-EXPIRAION-DATE"));
        putDateOrNull(json, "reissueDate", fields.get("ACCT-REISSUE-DATE"));
        putSignedDecimal(json, "cycleCredit", fields.get("ACCT-CURR-CYC-CREDIT"));
        putSignedDecimal(json, "cycleDebit", fields.get("ACCT-CURR-CYC-DEBIT"));
        putTrimmedOrNull(json, "addressZip", fields.get("ACCT-ADDR-ZIP"));
        putTrimmedOrNull(json, "groupId", fields.get("ACCT-GROUP-ID"));

        return json;
    }

    private ObjectNode applyCustomerTransformRules(Map<String, String> fields) {
        ObjectNode json = MAPPER.createObjectNode();

        putNumericLong(json, "customerId", fields.get("CUST-ID"));
        putTrimmedOrNull(json, "firstName", fields.get("CUST-FIRST-NAME"));
        putTrimmedOrNull(json, "middleName", fields.get("CUST-MIDDLE-NAME"));
        putTrimmedOrNull(json, "lastName", fields.get("CUST-LAST-NAME"));
        putTrimmedOrNull(json, "addressLine1", fields.get("CUST-ADDR-LINE-1"));
        putTrimmedOrNull(json, "addressLine2", fields.get("CUST-ADDR-LINE-2"));
        putTrimmedOrNull(json, "addressLine3", fields.get("CUST-ADDR-LINE-3"));
        putTrimmedOrNull(json, "stateCode", fields.get("CUST-ADDR-STATE-CD"));
        putTrimmedOrNull(json, "countryCode", fields.get("CUST-ADDR-COUNTRY-CD"));
        putTrimmedOrNull(json, "addressZip", fields.get("CUST-ADDR-ZIP"));
        putTrimmedOrNull(json, "phoneNumber1", fields.get("CUST-PHONE-NUM-1"));
        putTrimmedOrNull(json, "phoneNumber2", fields.get("CUST-PHONE-NUM-2"));
        putSsnString(json, "ssn", fields.get("CUST-SSN"));
        putTrimmedOrNull(json, "govtIssuedId", fields.get("CUST-GOVT-ISSUED-ID"));
        putDateOrNull(json, "dateOfBirth", fields.get("CUST-DOB-YYYY-MM-DD"));
        putTrimmedOrNull(json, "eftAccountId", fields.get("CUST-EFT-ACCOUNT-ID"));
        putCardHolderBoolean(json, "primaryCardHolder", fields.get("CUST-PRI-CARD-HOLDER-IND"));
        putNumericInt(json, "ficoCreditScore", fields.get("CUST-FICO-CREDIT-SCORE"));

        return json;
    }

    private ObjectNode applyTransactionTransformRules(Map<String, String> fields) {
        ObjectNode json = MAPPER.createObjectNode();

        putTrimmedOrNull(json, "transactionId", fields.get("TRAN-ID"));
        putTrimmedOrNull(json, "typeCode", fields.get("TRAN-TYPE-CD"));
        putNumericInt(json, "categoryCode", fields.get("TRAN-CAT-CD"));
        putTrimmedOrNull(json, "source", fields.get("TRAN-SOURCE"));
        putTrimmedOrNull(json, "description", fields.get("TRAN-DESC"));
        putSignedDecimal(json, "amount", fields.get("TRAN-AMT"));
        putNumericLong(json, "merchantId", fields.get("TRAN-MERCHANT-ID"));
        putTrimmedOrNull(json, "merchantName", fields.get("TRAN-MERCHANT-NAME"));
        putTrimmedOrNull(json, "merchantCity", fields.get("TRAN-MERCHANT-CITY"));
        putTrimmedOrNull(json, "merchantZip", fields.get("TRAN-MERCHANT-ZIP"));
        putTrimmedOrNull(json, "cardNumber", fields.get("TRAN-CARD-NUM"));
        putTimestampOrNull(json, "originTimestamp", fields.get("TRAN-ORIG-TS"));
        putTimestampOrNull(json, "processTimestamp", fields.get("TRAN-PROC-TS"));

        return json;
    }

    // ── Field-level helpers ──────────────────────────────────────────

    private void putNumericLong(ObjectNode node, String field, String value) {
        if (value == null || value.isBlank()) {
            node.putNull(field);
            return;
        }
        try {
            node.put(field, CobolDataFormatter.parseNumericDisplay(value));
        } catch (NumberFormatException e) {
            node.put("_error_" + field, "Invalid numeric: " + value);
        }
    }

    private void putNumericInt(ObjectNode node, String field, String value) {
        if (value == null || value.isBlank()) {
            node.putNull(field);
            return;
        }
        try {
            node.put(field, CobolDataFormatter.parseNumericDisplayInt(value));
        } catch (NumberFormatException e) {
            node.put("_error_" + field, "Invalid numeric: " + value);
        }
    }

    private void putSignedDecimal(ObjectNode node, String field, String value) {
        if (value == null || value.isBlank()) {
            node.putNull(field);
            return;
        }
        try {
            BigDecimal bd = CobolDataFormatter.parseSignedDecimal(value);
            if (bd != null) {
                node.put(field, bd);
            } else {
                node.putNull(field);
            }
        } catch (NumberFormatException e) {
            node.put("_error_" + field, "Invalid decimal: " + value);
        }
    }

    private void putTrimmedOrNull(ObjectNode node, String field, String value) {
        String trimmed = CobolDataFormatter.trimAlphanumeric(value);
        if (trimmed == null) {
            node.putNull(field);
        } else {
            node.put(field, trimmed);
        }
    }

    private void putDateOrNull(ObjectNode node, String field, String value) {
        if (CobolDataFormatter.isCobolBlank(value)) {
            node.putNull(field);
            return;
        }
        LocalDate date = CobolDataFormatter.parseDate(value);
        if (date != null) {
            node.put(field, date.toString());
        } else {
            node.putNull(field);
        }
    }

    private void putTimestampOrNull(ObjectNode node, String field, String value) {
        if (CobolDataFormatter.isCobolBlank(value)) {
            node.putNull(field);
            return;
        }
        String isoTs = CobolDataFormatter.convertTimestampToIso(value);
        if (isoTs != null) {
            node.put(field, isoTs);
        } else {
            node.putNull(field);
        }
    }

    private void putSsnString(ObjectNode node, String field, String value) {
        if (value == null || value.isBlank()) {
            node.putNull(field);
            return;
        }
        String trimmed = value.trim();
        // Validate all digits
        if (!trimmed.matches("\\d+")) {
            node.put("_error_" + field, "Invalid SSN: " + value);
            return;
        }
        // Preserve leading zeros by storing as string
        node.put(field, trimmed);
    }

    private void putCardHolderBoolean(ObjectNode node, String field, String value) {
        if (value == null || value.isBlank()) {
            node.putNull(field);
            return;
        }
        node.put(field, CobolDataFormatter.parseCardHolderIndicator(value));
    }

    // ── Live service call (for integration mode) ─────────────────────

    private ObjectNode postToService(String url, Map<String, String> payload) {
        try {
            String jsonPayload = MAPPER.writeValueAsString(payload);
            String response = io.restassured.RestAssured.given()
                    .contentType("application/json")
                    .body(jsonPayload)
                    .when()
                    .post(url)
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();
            return (ObjectNode) MAPPER.readTree(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call transformation service at " + url, e);
        }
    }
}
