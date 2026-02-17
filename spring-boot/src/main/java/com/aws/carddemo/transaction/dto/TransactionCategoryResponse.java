package com.aws.carddemo.transaction.dto;

import com.aws.carddemo.transaction.TransactionCategory;

public record TransactionCategoryResponse(
        String catCd,
        String catTypeCd,
        String catDesc
) {
    public static TransactionCategoryResponse from(TransactionCategory category) {
        return new TransactionCategoryResponse(
                category.getCatCd(),
                category.getTransactionType().getTypeCd(),
                category.getCatDesc()
        );
    }
}
