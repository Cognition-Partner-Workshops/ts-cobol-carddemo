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
 * Step definitions for Customer Data Transformation scenarios.
 * Maps to CVCUS01Y.cpy (CUSTOMER-RECORD, RECLN 500).
 *
 * <p>Common assertions live in {@link SharedStepDefs}; this class only
 * contains customer-specific Given/When/Then steps.
 */
public class CustomerTransformationStepDefs {

    private final TransformationContext ctx;
    private final TransformationClient client = new TransformationClient();
    private Map<String, String> cobolFields;
    private boolean serviceAvailable;

    public CustomerTransformationStepDefs(TransformationContext ctx) {
        this.ctx = ctx;
    }

    @Before("@customer")
    public void resetState() {
        cobolFields = new LinkedHashMap<>();
        serviceAvailable = false;
    }

    // ── Background ───────────────────────────────────────────────────

    @Given("the customer transformation service is available")
    public void theCustomerTransformationServiceIsAvailable() {
        serviceAvailable = true;
    }

    // ── Given: full record via DataTable ─────────────────────────────

    @Given("a mainframe customer record with the following COBOL fields:")
    public void aMainframeCustomerRecordWithFields(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            cobolFields.put(row.get("field"), row.get("value"));
        }
    }

    // ── Given: single-field shortcuts ────────────────────────────────

    @Given("a mainframe customer record with CUST-ID {string}")
    public void withCustId(String value) {
        setDefaults();
        cobolFields.put("CUST-ID", value);
    }

    @Given("a mainframe customer record with CUST-FIRST-NAME {string}")
    public void withFirstName(String value) {
        setDefaults();
        cobolFields.put("CUST-FIRST-NAME", value);
    }

    @Given("a mainframe customer record with CUST-MIDDLE-NAME {string}")
    public void withMiddleName(String value) {
        setDefaults();
        cobolFields.put("CUST-MIDDLE-NAME", value);
    }

    @Given("a mainframe customer record with CUST-LAST-NAME {string}")
    public void withLastName(String value) {
        setDefaults();
        cobolFields.put("CUST-LAST-NAME", value);
    }

    @Given("a mainframe customer record with CUST-ADDR-LINE-1 {string}")
    public void withAddrLine1(String value) {
        setDefaults();
        cobolFields.put("CUST-ADDR-LINE-1", value);
    }

    @Given("a mainframe customer record with CUST-ADDR-LINE-2 {string}")
    public void withAddrLine2(String value) {
        setDefaults();
        cobolFields.put("CUST-ADDR-LINE-2", value);
    }

    @Given("a mainframe customer record with CUST-ADDR-LINE-2 all spaces")
    public void withAddrLine2AllSpaces() {
        setDefaults();
        cobolFields.put("CUST-ADDR-LINE-2", " ".repeat(50));
    }

    @Given("a mainframe customer record with CUST-ADDR-LINE-3 all spaces")
    public void withAddrLine3AllSpaces() {
        setDefaults();
        cobolFields.put("CUST-ADDR-LINE-3", " ".repeat(50));
    }

    @Given("a mainframe customer record with CUST-ADDR-STATE-CD {string}")
    public void withStateCode(String value) {
        setDefaults();
        cobolFields.put("CUST-ADDR-STATE-CD", value);
    }

    @Given("a mainframe customer record with CUST-SSN {string}")
    public void withSsn(String value) {
        setDefaults();
        cobolFields.put("CUST-SSN", value);
    }

    @Given("a mainframe customer record with CUST-DOB-YYYY-MM-DD {string}")
    public void withDob(String value) {
        setDefaults();
        cobolFields.put("CUST-DOB-YYYY-MM-DD", value);
    }

    @Given("a mainframe customer record with CUST-EFT-ACCOUNT-ID {string}")
    public void withEftAccountId(String value) {
        setDefaults();
        cobolFields.put("CUST-EFT-ACCOUNT-ID", value);
    }

    @Given("a mainframe customer record with CUST-PRI-CARD-HOLDER-IND {string}")
    public void withPriCardHolderInd(String value) {
        setDefaults();
        cobolFields.put("CUST-PRI-CARD-HOLDER-IND", value);
    }

    @Given("a mainframe customer record with CUST-FICO-CREDIT-SCORE {string}")
    public void withFicoScore(String value) {
        setDefaults();
        cobolFields.put("CUST-FICO-CREDIT-SCORE", value);
    }

    @Given("a mainframe customer record with CUST-PHONE-NUM-2 {string}")
    public void withPhoneNum2(String value) {
        setDefaults();
        cobolFields.put("CUST-PHONE-NUM-2", value);
    }

    @Given("a mainframe customer record with {int} bytes of FILLER data")
    public void withCustomerFillerData(int fillerLength) {
        setDefaults();
        // FILLER is not mapped; validates structural awareness
    }

    // ── When ─────────────────────────────────────────────────────────

    @When("the customer record is submitted for transformation")
    public void theCustomerRecordIsSubmittedForTransformation() {
        assertThat(serviceAvailable).as("Transformation service must be available").isTrue();
        ObjectNode result = client.transformCustomer(cobolFields);
        assertThat(result).as("Transformation result should not be null").isNotNull();
        ctx.setTransformedJson(result);
    }

    // ── Then: DataTable validation (customer-specific) ───────────────

    @Then("the transformed customer JSON should contain:")
    public void theTransformedCustomerJsonShouldContain(DataTable dataTable) {
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
            cobolFields.put("CUST-ID", "000000001");
            cobolFields.put("CUST-FIRST-NAME", "JOHN");
            cobolFields.put("CUST-MIDDLE-NAME", "M");
            cobolFields.put("CUST-LAST-NAME", "DOE");
            cobolFields.put("CUST-ADDR-LINE-1", "123 MAIN ST");
            cobolFields.put("CUST-ADDR-LINE-2", "");
            cobolFields.put("CUST-ADDR-LINE-3", "");
            cobolFields.put("CUST-ADDR-STATE-CD", "NY");
            cobolFields.put("CUST-ADDR-COUNTRY-CD", "USA");
            cobolFields.put("CUST-ADDR-ZIP", "10001");
            cobolFields.put("CUST-PHONE-NUM-1", "212-555-0100");
            cobolFields.put("CUST-PHONE-NUM-2", "");
            cobolFields.put("CUST-SSN", "123456789");
            cobolFields.put("CUST-GOVT-ISSUED-ID", "DL123456789");
            cobolFields.put("CUST-DOB-YYYY-MM-DD", "1985-01-15");
            cobolFields.put("CUST-EFT-ACCOUNT-ID", "EFT0000001");
            cobolFields.put("CUST-PRI-CARD-HOLDER-IND", "Y");
            cobolFields.put("CUST-FICO-CREDIT-SCORE", "750");
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
            case "boolean":
                assertThat(node.asBoolean()).as("'%s' boolean", fieldName)
                        .isEqualTo(Boolean.parseBoolean(expectedValue));
                break;
            case "date":
                assertThat(node.asText()).as("'%s' date", fieldName).isEqualTo(expectedValue);
                break;
            case "string":
                String actualText = node.isNull() ? null : node.asText();
                if (expectedValue == null || expectedValue.isEmpty()) {
                    assertThat(actualText == null || actualText.isEmpty())
                            .as("'%s' should be null or empty", fieldName).isTrue();
                } else {
                    assertThat(actualText).as("'%s' string", fieldName).isEqualTo(expectedValue);
                }
                break;
            default:
                assertThat(node.asText()).as("'%s' value", fieldName).isEqualTo(expectedValue);
        }
    }
}
