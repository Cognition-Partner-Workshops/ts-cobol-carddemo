package com.carddemo.api.service;

import com.carddemo.api.dto.ReportRequest;
import com.carddemo.api.dto.TransactionResponse;
import com.carddemo.core.domain.Transaction;
import com.carddemo.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for Report generation.
 * Replaces business logic from CORPT00C (Transaction Reports).
 *
 * Key COBOL logic replaced:
 * - Sequential read of TRANSACT file for date range
 * - Report formatting and output generation
 * - Monthly/custom date range filtering
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final TransactionRepository transactionRepository;

    public List<TransactionResponse> generateTransactionReport(ReportRequest request) {
        List<Transaction> transactions = transactionRepository
                .findByOrigTimestampBetweenOrderByOrigTimestampAsc(
                        request.getStartDate().atStartOfDay(),
                        request.getEndDate().atTime(23, 59, 59));

        return transactions.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TransactionResponse mapToResponse(Transaction txn) {
        return TransactionResponse.builder()
                .transactionId(txn.getTranId())
                .typeCode(txn.getTypeCode())
                .categoryCode(String.valueOf(txn.getCategoryCode()))
                .source(txn.getSource())
                .description(txn.getDescription())
                .amount(txn.getAmount())
                .merchantId(txn.getMerchantId())
                .merchantName(txn.getMerchantName())
                .merchantCity(txn.getMerchantCity())
                .merchantZip(txn.getMerchantZip())
                .cardNumber(txn.getCardNum())
                .originTimestamp(txn.getOrigTimestamp())
                .processTimestamp(txn.getProcTimestamp())
                .build();
    }
}
