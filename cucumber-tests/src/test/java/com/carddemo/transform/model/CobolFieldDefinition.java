package com.carddemo.transform.model;

/**
 * Represents a single COBOL field definition from a copybook.
 * Captures the PIC clause details needed for transformation validation.
 */
public class CobolFieldDefinition {

    public enum PicType {
        /** PIC 9(n) - unsigned numeric display */
        NUMERIC_DISPLAY,
        /** PIC S9(n)V99 - signed numeric with implied decimal */
        SIGNED_DECIMAL,
        /** PIC X(n) - alphanumeric */
        ALPHANUMERIC
    }

    private final String cobolName;
    private final PicType picType;
    private final int totalLength;
    private final int decimalPlaces;

    public CobolFieldDefinition(String cobolName, PicType picType, int totalLength, int decimalPlaces) {
        this.cobolName = cobolName;
        this.picType = picType;
        this.totalLength = totalLength;
        this.decimalPlaces = decimalPlaces;
    }

    public CobolFieldDefinition(String cobolName, PicType picType, int totalLength) {
        this(cobolName, picType, totalLength, 0);
    }

    public String getCobolName() {
        return cobolName;
    }

    public PicType getPicType() {
        return picType;
    }

    public int getTotalLength() {
        return totalLength;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    @Override
    public String toString() {
        return "CobolFieldDefinition{" +
                "cobolName='" + cobolName + '\'' +
                ", picType=" + picType +
                ", totalLength=" + totalLength +
                ", decimalPlaces=" + decimalPlaces +
                '}';
    }
}
