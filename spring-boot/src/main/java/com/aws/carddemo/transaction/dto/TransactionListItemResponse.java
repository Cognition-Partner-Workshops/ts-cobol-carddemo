package com.aws.carddemo.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.aws.carddemo.transaction.TransactionRecord;

public record TransactionListItemResponse(
        Long id,
        String maskedCardNumber,
        LocalDateTime date,
        String description,
        BigDecimal amount,
        String typeCode
) {
    public static TransactionListItemResponse from(TransactionRecord record) {
        String cardNum = record.getCard().getCardNumber();
        String masked = "************" + cardNum.substring(cardNum.length() - 4);
        return new TransactionListItemResponse(
                record.getId(),
                masked,
                record.getTimestamp(),
                record.getDescription(),
                record.getAmount(),
                record.getTransactionType()
        );
    }
}
