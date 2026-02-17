package com.aws.carddemo.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.aws.carddemo.transaction.TransactionRecord;

public record TransactionResponse(
        Long id,
        String cardNumber,
        String transactionType,
        String typeDescription,
        String transactionCategory,
        String categoryDescription,
        String transactionSource,
        String description,
        BigDecimal amount,
        String merchantId,
        String merchantName,
        String merchantCity,
        String merchantZip,
        LocalDateTime timestamp,
        String originalCurrency,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(TransactionRecord record,
                                           String typeDescription,
                                           String categoryDescription) {
        return new TransactionResponse(
                record.getId(),
                record.getCard().getCardNumber(),
                record.getTransactionType(),
                typeDescription,
                record.getTransactionCategory(),
                categoryDescription,
                record.getTransactionSource(),
                record.getDescription(),
                record.getAmount(),
                record.getMerchantId(),
                record.getMerchantName(),
                record.getMerchantCity(),
                record.getMerchantZip(),
                record.getTimestamp(),
                record.getOriginalCurrency(),
                record.getCreatedAt()
        );
    }
}
