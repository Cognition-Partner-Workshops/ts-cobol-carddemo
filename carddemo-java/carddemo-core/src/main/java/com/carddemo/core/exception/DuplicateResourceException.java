package com.carddemo.core.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * Replaces CICS RESP(DUPREC) and VSAM duplicate-key conditions.
 */
public class DuplicateResourceException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    public DuplicateResourceException(String resourceType, String resourceId) {
        super(String.format("%s already exists with ID: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}
