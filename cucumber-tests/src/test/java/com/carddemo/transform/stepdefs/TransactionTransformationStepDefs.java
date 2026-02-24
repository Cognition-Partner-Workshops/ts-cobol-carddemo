package com.carddemo.transform.stepdefs;

import com.carddemo.transform.util.TransformationClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for Transaction Data Transformation scenarios.
 * Maps to CVTRA05Y.cpy (TRAN-RECORD, RECLN 350).
 *
 * <p>Common assertions live in {@link SharedStepDefs}; this class only
 * contains transaction-specific Given/When/Then steps.
 */
public class TransactionTransformationStepDefs {

    private final TransformationContext ctx;
    private final TransformationClient client = new TransformationClient();
    private Map<String, String> cobolFields;
    private boolean serviceAvailable;

    public TransactionTransformationStepDefs(TransformationContext ctx) {
        this.ctx = ctx;
    }

    @Before("@transaction")
    public void resetState() {
        cobolFields = new LinkedHashMap<>();
        serviceAvailable = false;
    }

    // ── Background ───────────────────────────────────────────────────

    @Given("the transaction transformation service is available")
    public void theTransactionTransformationServiceIsAvailable() {
        serviceAvailable = true;
    }

    // ── Given: full record via DataTable ─────────────────────────────

    @Given("a mainframe transaction record with the following COBOL fields:")
    public void aMainframeTransactionRecordWithFields(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            cobolFields.put(row.get("field"), row.get("value"));
        }
    }

    // ── Given: single-field shortcuts ────────────────────────────────

    @Given("a mainframe transaction record with TRAN-ID {string}")
    public void withTranId(String value) {
        setDefaults();
        cobolFields.put("TRAN-ID", value);
    }

    @Given("a mainframe transaction record with TRAN-AMT {string}")
    public void withTranAmt(String value) {
        setDefaults();
        cobolFields.put("TRAN-AMT", value);
    }

    @Given("a mainframe transaction record with TRAN-CAT-CD {string}")
    public void withCatCd(String value) {
        setDefaults();
        cobolFields.put("TRAN-CAT-CD", value);
    }

    @Given("a mainframe transaction record with TRAN-MERCHANT-ID {string}")
    public void withMerchantId(String value) {
        setDefaults();
        cobolFields.put("TRAN-MERCHANT-ID", value);
    }

    @Given("a mainframe transaction record with TRAN-MERCHANT-NAME {string}")
    public void withMerchantName(String value) {
        setDefaults();
        cobolFields.put("TRAN-MERCHANT-NAME", value);
    }

    @Given("a mainframe transaction record with TRAN-ORIG-TS {string}")
    public void withOrigTs(String value) {
        setDefaults();
        cobolFields.put("TRAN-ORIG-TS", value);
    }

    @Given("a mainframe transaction record with TRAN-PROC-TS {string}")
    public void withProcTs(String value) {
        setDefaults();
        cobolFields.put("TRAN-PROC-TS", value);
    }

    @Given("a mainframe transaction record with TRAN-CARD-NUM {string}")
    public void withCardNum(String value) {
        setDefaults();
        cobolFields.put("TRAN-CARD-NUM", value);
    }

    @Given("a mainframe transaction record with TRAN-SOURCE {string}")
    public void withSource(String value) {
        setDefaults();
        cobolFields.put("TRAN-SOURCE", value);
    }

    @Given("a mainframe transaction record with TRAN-DESC all spaces")
    public void withDescAllSpaces() {
        setDefaults();
        cobolFields.put("TRAN-DESC", " ".repeat(100));
    }

    @Given("a mainframe transaction record with TRAN-MERCHANT-NAME all spaces")
    public void withMerchantNameAllSpaces() {
        setDefaults();
        cobolFields.put("TRAN-MERCHANT-NAME", " ".repeat(50));
    }

    @Given("a mainframe transaction record with TRAN-MERCHANT-CITY all spaces")
    public void withMerchantCityAllSpaces() {
        setDefaults();
        cobolFields.put("TRAN-MERCHANT-CITY", " ".repeat(50));
    }

    @Given("a mainframe transaction record with TRAN-DESC {string} repeated to fill {int} characters")
    public void withDescRepeated(String pattern, int totalLength) {
        setDefaults();
        StringBuilder sb = new StringBuilder();
        while (sb.length() < totalLength) {
            sb.append(pattern);
        }
        cobolFields.put("TRAN-DESC", sb.substring(0, totalLength));
    }

    @Given("a mainframe transaction record with {int} bytes of FILLER data")
    public void withTransactionFillerData(int fillerLength) {
        setDefaults();
        // FILLER is not mapped; validates structural awareness
    }

    // ── When ─────────────────────────────────────────────────────────

    @When("the transaction record is submitted for transformation")
    public void theTransactionRecordIsSubmittedForTransformation() {
        assertThat(serviceAvailable).as("Transformation service must be available").isTrue();
        ObjectNode result = client.transformTransaction(cobolFields);
        assertThat(result).as("Transformation result should not be null").isNotNull();
        ctx.setTransformedJson(result);
    }

    // ── Then: DataTable validation (transaction-specific) ────────────

    @Then("the transformed transaction JSON should contain:")
    public void theTransformedTransactionJsonShouldContain(DataTable dataTable) {
        ObjectNode json = ctx.getTransformedJson();
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            String jsonField = row.get("jsonField");
            String expectedValue = row.get("expectedValue");
            String expectedType = row.get("expectedType");

            JsonNode node = json.get(jsonField);
            assertThat(node).as("JSON field '%s' should be present", jsonField).isNotNull();
            assertFieldValueAndType(jsonField, node, expectedValue, expectedType);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void setDefaults() {
        if (cobolFields.isEmpty()) {
            cobolFields.put("TRAN-ID", "0000000000000001");
            cobolFields.put("TRAN-TYPE-CD", "SA");
            cobolFields.put("TRAN-CAT-CD", "5001");
            cobolFields.put("TRAN-SOURCE", "ONLINE");
            cobolFields.put("TRAN-DESC", "DEFAULT TRANSACTION");
            cobolFields.put("TRAN-AMT", "+000000100.00");
            cobolFields.put("TRAN-MERCHANT-ID", "000012345");
            cobolFields.put("TRAN-MERCHANT-NAME", "DEFAULT MERCHANT");
            cobolFields.put("TRAN-MERCHANT-CITY", "NEW YORK");
            cobolFields.put("TRAN-MERCHANT-ZIP", "10001");
            cobolFields.put("TRAN-CARD-NUM", "4111111111111111");
            cobolFields.put("TRAN-ORIG-TS", "2024-01-01-00.00.00.000000");
            cobolFields.put("TRAN-PROC-TS", "2024-01-01-00.00.01.000000");
        }
    }

    private void assertFieldValueAndType(String fieldName, JsonNode node,
                                         String expectedValue, String expectedType) {
        switch (expectedType.toLowerCase()) {
            case "long":
                assertThat(node.asLong()).as("'%s' long", fieldName)
                        .isEqualTo(Long.parseLong(expectedValue));
                break;
            case "integer":
                assertThat(node.asInt()).as("'%s' int", fieldName)
                        .isEqualTo(Integer.parseInt(expectedValue));
                break;
            case "decimal":
                assertThat(new BigDecimal(node.asText()).compareTo(new BigDecimal(expectedValue)))
                        .as("'%s' decimal", fieldName).isEqualTo(0);
                break;
            case "string":
                assertThat(node.asText()).as("'%s' string", fieldName).isEqualTo(expectedValue);
                break;
            case "date":
                assertThat(node.asText()).as("'%s' date", fieldName).isEqualTo(expectedValue);
                break;
            case "timestamp":
                assertThat(node.asText()).as("'%s' timestamp", fieldName).isEqualTo(expectedValue);
                break;
            default:
                assertThat(node.asText()).as("'%s' value", fieldName).isEqualTo(expectedValue);
        }
    }
}
