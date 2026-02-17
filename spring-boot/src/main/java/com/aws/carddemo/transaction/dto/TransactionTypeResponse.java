package com.aws.carddemo.transaction.dto;

import com.aws.carddemo.transaction.TransactionType;

public record TransactionTypeResponse(
        String typeCd,
        String typeDesc
) {
    public static TransactionTypeResponse from(TransactionType type) {
        return new TransactionTypeResponse(type.getTypeCd(), type.getTypeDesc());
    }
}
