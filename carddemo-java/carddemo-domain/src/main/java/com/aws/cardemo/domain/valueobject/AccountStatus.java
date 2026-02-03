package com.aws.cardemo.domain.valueobject;

public enum AccountStatus {
    ACTIVE("A"),
    CLOSED("C"),
    SUSPENDED("S");

    private final String code;

    AccountStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AccountStatus fromCode(String code) {
        for (AccountStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown account status code: " + code);
    }
}
