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
 * Step definitions for Account Data Transformation scenarios.
 * Maps to CVACT01Y.cpy (ACCOUNT-RECORD, RECLN 300).
 *
 * <p>Common assertions live in {@link SharedStepDefs}; this class only
 * contains account-specific Given/When/Then steps.
 */
public class AccountTransformationStepDefs {

    private final TransformationContext ctx;
    private final TransformationClient client = new TransformationClient();
    private Map<String, String> cobolFields;
    private boolean serviceAvailable;

    public AccountTransformationStepDefs(TransformationContext ctx) {
        this.ctx = ctx;
    }

    @Before("@account")
    public void resetState() {
        cobolFields = new LinkedHashMap<>();
        serviceAvailable = false;
    }

    // ── Background ───────────────────────────────────────────────────

    @Given("the account transformation service is available")
    public void theAccountTransformationServiceIsAvailable() {
        serviceAvailable = true;
    }

    // ── Given: full record via DataTable ─────────────────────────────

    @Given("a mainframe account record with the following COBOL fields:")
    public void aMainframeAccountRecordWithFields(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            cobolFields.put(row.get("field"), row.get("value"));
        }
    }

    // ── Given: single-field shortcuts ────────────────────────────────

    @Given("a mainframe account record with ACCT-ID {string}")
    public void withAcctId(String value) {
        setDefaults();
        cobolFields.put("ACCT-ID", value);
    }

    @Given("a mainframe account record with ACCT-CURR-BAL {string}")
    public void withCurrBal(String value) {
        setDefaults();
        cobolFields.put("ACCT-CURR-BAL", value);
    }

    @Given("a mainframe account record with ACCT-CREDIT-LIMIT {string}")
    public void withCreditLimit(String value) {
        setDefaults();
        cobolFields.put("ACCT-CREDIT-LIMIT", value);
    }

    @Given("a mainframe account record with ACCT-CASH-CREDIT-LIMIT {string}")
    public void withCashCreditLimit(String value) {
        setDefaults();
        cobolFields.put("ACCT-CASH-CREDIT-LIMIT", value);
    }

    @Given("a mainframe account record with ACCT-OPEN-DATE {string}")
    public void withOpenDate(String value) {
        setDefaults();
        cobolFields.put("ACCT-OPEN-DATE", value);
    }

    @Given("a mainframe account record with ACCT-EXPIRAION-DATE {string}")
    public void withExpirationDate(String value) {
        setDefaults();
        cobolFields.put("ACCT-EXPIRAION-DATE", value);
    }

    @Given("a mainframe account record with ACCT-REISSUE-DATE {string}")
    public void withReissueDate(String value) {
        setDefaults();
        cobolFields.put("ACCT-REISSUE-DATE", value);
    }

    @Given("a mainframe account record with ACCT-CURR-CYC-DEBIT {string}")
    public void withCycDebit(String value) {
        setDefaults();
        cobolFields.put("ACCT-CURR-CYC-DEBIT", value);
    }

    @Given("a mainframe account record with ACCT-ACTIVE-STATUS {string}")
    public void withActiveStatus(String value) {
        setDefaults();
        cobolFields.put("ACCT-ACTIVE-STATUS", value);
    }

    @Given("a mainframe account record with ACCT-ADDR-ZIP {string}")
    public void withAddrZip(String value) {
        setDefaults();
        cobolFields.put("ACCT-ADDR-ZIP", value);
    }

    @Given("a mainframe account record with ACCT-GROUP-ID {string}")
    public void withGroupId(String value) {
        setDefaults();
        cobolFields.put("ACCT-GROUP-ID", value);
    }

    @Given("a mainframe account record with {int} bytes of FILLER data {string}")
    public void withFillerData(int fillerLength, String fillerSnippet) {
        setDefaults();
        // FILLER is not part of named fields; validates it is ignored
    }

    // ── When ─────────────────────────────────────────────────────────

    @When("the account record is submitted for transformation")
    public void theAccountRecordIsSubmittedForTransformation() {
        assertThat(serviceAvailable).as("Transformation service must be available").isTrue();
        ObjectNode result = client.transformAccount(cobolFields);
        assertThat(result).as("Transformation result should not be null").isNotNull();
        ctx.setTransformedJson(result);
    }

    // ── Then: DataTable validation (account-specific) ────────────────

    @Then("the transformed account JSON should contain:")
    public void theTransformedAccountJsonShouldContain(DataTable dataTable) {
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
            cobolFields.put("ACCT-ID", "00000000001");
            cobolFields.put("ACCT-ACTIVE-STATUS", "Y");
            cobolFields.put("ACCT-CURR-BAL", "+0000000000.00");
            cobolFields.put("ACCT-CREDIT-LIMIT", "+0000010000.00");
            cobolFields.put("ACCT-CASH-CREDIT-LIMIT", "+0000005000.00");
            cobolFields.put("ACCT-OPEN-DATE", "2020-01-01");
            cobolFields.put("ACCT-EXPIRAION-DATE", "2025-01-01");
            cobolFields.put("ACCT-REISSUE-DATE", "2023-01-01");
            cobolFields.put("ACCT-CURR-CYC-CREDIT", "+0000000000.00");
            cobolFields.put("ACCT-CURR-CYC-DEBIT", "+0000000000.00");
            cobolFields.put("ACCT-ADDR-ZIP", "10001");
            cobolFields.put("ACCT-GROUP-ID", "DEFAULT");
        }
    }

    private void assertFieldValueAndType(String fieldName, JsonNode node,
                                         String expectedValue, String expectedType) {
        switch (expectedType.toLowerCase()) {
            case "long":
                assertThat(node.asLong()).as("'%s' long", fieldName)
                        .isEqualTo(Long.parseLong(expectedValue));
                break;
            case "decimal":
                assertThat(new BigDecimal(node.asText()).compareTo(new BigDecimal(expectedValue)))
                        .as("'%s' decimal", fieldName).isEqualTo(0);
                break;
            case "date":
                assertThat(node.asText()).as("'%s' date", fieldName).isEqualTo(expectedValue);
                break;
            case "string":
                assertThat(node.asText()).as("'%s' string", fieldName).isEqualTo(expectedValue);
                break;
            default:
                assertThat(node.asText()).as("'%s' value", fieldName).isEqualTo(expectedValue);
        }
    }
}
