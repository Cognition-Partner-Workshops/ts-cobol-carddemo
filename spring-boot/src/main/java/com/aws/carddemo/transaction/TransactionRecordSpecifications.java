package com.aws.carddemo.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

public final class TransactionRecordSpecifications {

    private TransactionRecordSpecifications() {
    }

    public static Specification<TransactionRecord> hasCardNumber(String cardNumber) {
        return (root, query, cb) -> cb.equal(root.get("card").get("cardNumber"), cardNumber);
    }

    public static Specification<TransactionRecord> timestampAfter(LocalDateTime fromDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), fromDate);
    }

    public static Specification<TransactionRecord> timestampBefore(LocalDateTime toDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), toDate);
    }

    public static Specification<TransactionRecord> hasTransactionType(String typeCode) {
        return (root, query, cb) -> cb.equal(root.get("transactionType"), typeCode);
    }

    public static Specification<TransactionRecord> amountGreaterThanOrEqual(BigDecimal minAmount) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), minAmount);
    }

    public static Specification<TransactionRecord> amountLessThanOrEqual(BigDecimal maxAmount) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), maxAmount);
    }
}
