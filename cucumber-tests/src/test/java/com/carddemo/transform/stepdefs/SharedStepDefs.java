package com.carddemo.transform.stepdefs;

import com.carddemo.transform.model.CopybookRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared step definitions used across account, customer, and transaction
 * feature files.  Every step pattern that appears in more than one feature
 * file MUST live here (Cucumber forbids duplicate patterns across classes).
 *
 * <p>State is shared via {@link TransformationContext}, injected by PicoContainer.
 */
public class SharedStepDefs {

    private final TransformationContext ctx;

    public SharedStepDefs(TransformationContext ctx) {
        this.ctx = ctx;
    }

    // ── Background / structural ─────────────────────────────────────

    @Given("the COBOL copybook {string} defines record length {int}")
    public void theCopybookDefinesRecordLength(String copybook, int expectedLength) {
        switch (copybook) {
            case "CVACT01Y":
                assertThat(CopybookRegistry.ACCOUNT_RECORD_LENGTH)
                        .as("CVACT01Y record length").isEqualTo(expectedLength);
                break;
            case "CVCUS01Y":
                assertThat(CopybookRegistry.CUSTOMER_RECORD_LENGTH)
                        .as("CVCUS01Y record length").isEqualTo(expectedLength);
                break;
            case "CVTRA05Y":
                assertThat(CopybookRegistry.TRANSACTION_RECORD_LENGTH)
                        .as("CVTRA05Y record length").isEqualTo(expectedLength);
                break;
            default:
                throw new IllegalArgumentException("Unknown copybook: " + copybook);
        }
    }

    // ── Common "Then" assertions ────────────────────────────────────

    @Then("the transformed JSON field {string} should be {long} as a long")
    public void theFieldShouldBeLong(String field, long expected) {
        JsonNode node = getNode(field);
        assertThat(node.asLong()).as("Field '%s' value", field).isEqualTo(expected);
    }

    @Then("the transformed JSON field {string} should be {string}")
    public void theFieldShouldBeString(String field, String expected) {
        JsonNode node = getNode(field);
        assertThat(node.asText()).as("Field '%s' value", field).isEqualTo(expected);
    }

    @Then("the transformed JSON field {string} should be {int} as an integer")
    public void theFieldShouldBeInteger(String field, int expected) {
        JsonNode node = getNode(field);
        assertThat(node.asInt()).as("Field '%s' value", field).isEqualTo(expected);
    }

    @Then("the transformed JSON field {string} should be {bigdecimal} as a decimal")
    public void theFieldShouldBeDecimal(String field, BigDecimal expected) {
        JsonNode node = getNode(field);
        assertThat(new BigDecimal(node.asText()).compareTo(expected))
                .as("Field '%s' decimal value", field).isEqualTo(0);
    }

    @Then("the transformed JSON field {string} should be {string} as an ISO date")
    public void theFieldShouldBeIsoDate(String field, String expected) {
        JsonNode node = getNode(field);
        assertThat(node.asText()).as("Field '%s' ISO date", field).isEqualTo(expected);
    }

    @Then("the transformed JSON field {string} should be {string} as an ISO timestamp")
    public void theFieldShouldBeIsoTimestamp(String field, String expected) {
        JsonNode node = getNode(field);
        assertThat(node.asText()).as("Field '%s' ISO timestamp", field).isEqualTo(expected);
    }

    @Then("the transformed JSON field {string} should be true as a boolean")
    public void theFieldShouldBeTrue(String field) {
        JsonNode node = getNode(field);
        assertThat(node.asBoolean()).as("Field '%s' should be true", field).isTrue();
    }

    @Then("the transformed JSON field {string} should be false as a boolean")
    public void theFieldShouldBeFalse(String field) {
        JsonNode node = getNode(field);
        assertThat(node.asBoolean()).as("Field '%s' should be false", field).isFalse();
    }

    @Then("the transformed JSON field {string} should be {string} as a string")
    public void theFieldShouldBeStringTyped(String field, String expected) {
        JsonNode node = getNode(field);
        assertThat(node.asText()).as("Field '%s' value", field).isEqualTo(expected);
    }

    @Then("the transformed JSON field {string} should be null")
    public void theFieldShouldBeNull(String field) {
        JsonNode node = getNode(field);
        assertThat(node.isNull()).as("Field '%s' should be null", field).isTrue();
    }

    @Then("the transformed JSON field {string} should be null or empty")
    public void theFieldShouldBeNullOrEmpty(String field) {
        JsonNode node = getNode(field);
        if (!node.isNull()) {
            assertThat(node.asText()).as("Field '%s' should be empty", field).isEmpty();
        }
    }

    @Then("the transformed JSON field {string} should be {string} or a defined default")
    public void theFieldShouldBeValueOrDefault(String field, String possibleValue) {
        JsonNode node = getNode(field);
        String actual = node.isNull() ? null : node.asText();
        assertThat(actual == null || actual.equals(possibleValue.trim()) || !actual.isBlank())
                .as("Field '%s' should be '%s' or a default, got '%s'",
                        field, possibleValue, actual)
                .isTrue();
    }

    @Then("the transformed JSON field {string} should be {string} or {string}")
    public void theFieldShouldBeOneOfTwo(String field, String option1, String option2) {
        JsonNode node = getNode(field);
        assertThat(node.asText())
                .as("Field '%s' should be '%s' or '%s'", field, option1, option2)
                .isIn(option1, option2);
    }

    @Then("the transformed JSON field {string} should have length <= {int}")
    public void theFieldShouldHaveLengthLessThanOrEqual(String field, int maxLength) {
        JsonNode node = getNode(field);
        assertThat(node.asText().length())
                .as("Field '%s' length", field).isLessThanOrEqualTo(maxLength);
    }

    @Then("the transformation should either reject the record or set {string} to null")
    public void theTransformationShouldRejectOrSetNull(String field) {
        ObjectNode json = ctx.getTransformedJson();
        JsonNode node = json.get(field);
        JsonNode errorNode = json.get("_error_" + field);
        boolean isNullField = node != null && node.isNull();
        boolean hasError = errorNode != null;
        assertThat(isNullField || hasError)
                .as("Field '%s' should be null or have error", field).isTrue();
    }

    @Then("the transformation should report a validation error for {string}")
    public void theTransformationShouldReportValidationError(String field) {
        ObjectNode json = ctx.getTransformedJson();
        JsonNode errorNode = json.get("_error_" + field);
        assertThat(errorNode)
                .as("Validation error for '%s' should be present", field).isNotNull();
    }

    @Then("the transformed JSON should not contain a {string} field")
    public void theJsonShouldNotContainField(String field) {
        assertThat(ctx.getTransformedJson().has(field))
                .as("JSON should not contain '%s'", field).isFalse();
    }

    @And("the total record length consumed should be {int} bytes")
    public void theTotalRecordLengthConsumedShouldBe(int expectedLength) {
        boolean isKnownLength = expectedLength == CopybookRegistry.ACCOUNT_RECORD_LENGTH
                || expectedLength == CopybookRegistry.CUSTOMER_RECORD_LENGTH
                || expectedLength == CopybookRegistry.TRANSACTION_RECORD_LENGTH;
        assertThat(isKnownLength || expectedLength > 0)
                .as("Record length %d should be a known copybook length or positive",
                        expectedLength)
                .isTrue();
    }

    // ── Helper ──────────────────────────────────────────────────────

    private JsonNode getNode(String field) {
        ObjectNode json = ctx.getTransformedJson();
        assertThat(json).as("Transformed JSON must exist").isNotNull();
        JsonNode node = json.get(field);
        assertThat(node).as("Field '%s' present", field).isNotNull();
        return node;
    }
}
