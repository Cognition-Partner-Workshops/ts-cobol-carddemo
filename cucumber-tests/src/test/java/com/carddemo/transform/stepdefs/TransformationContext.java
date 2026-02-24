package com.carddemo.transform.stepdefs;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Shared mutable context injected by Cucumber's PicoContainer DI into all
 * step definition classes within a single scenario execution.
 * Holds the transformed JSON result so that entity-specific "When" steps
 * can populate it and shared "Then" steps can assert against it.
 */
public class TransformationContext {

    private ObjectNode transformedJson;

    public ObjectNode getTransformedJson() {
        return transformedJson;
    }

    public void setTransformedJson(ObjectNode json) {
        this.transformedJson = json;
    }
}
