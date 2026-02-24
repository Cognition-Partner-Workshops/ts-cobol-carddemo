package com.carddemo.transform.runner;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.FEATURES_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * JUnit 5 Platform Suite entry point for Cucumber tests.
 * Discovers and runs all .feature files under src/test/resources/features.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("com.carddemo.transform")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/resources/features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.carddemo.transform.stepdefs")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-reports/cucumber.html, json:target/cucumber-reports/cucumber.json")
public class RunCucumberTest {
    // This class is intentionally empty.
    // It serves as the entry point for the JUnit Platform Suite.
}
