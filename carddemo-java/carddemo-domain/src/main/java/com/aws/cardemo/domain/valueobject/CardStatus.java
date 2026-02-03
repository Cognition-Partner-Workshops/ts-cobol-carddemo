package com.aws.cardemo.domain.valueobject;

public enum CardStatus {
    ACTIVE("Y"),
    INACTIVE("N");

    private final String code;

    CardStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CardStatus fromCode(String code) {
        for (CardStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown card status code: " + code);
    }
}
