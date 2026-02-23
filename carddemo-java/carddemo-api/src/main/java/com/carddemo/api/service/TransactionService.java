package com.carddemo.api.service;

import com.carddemo.api.dto.PageResponse;
import com.carddemo.api.dto.TransactionCreateRequest;
import com.carddemo.api.dto.TransactionResponse;
import com.carddemo.core.domain.Transaction;
import com.carddemo.core.exception.ResourceNotFoundException;
import com.carddemo.core.repository.CardRepository;
import com.carddemo.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service layer for Transaction operations.
 * Replaces business logic from COTRN00C (Transaction List), COTRN01C (Transaction View),
 * and COTRN02C (Transaction Add).
 *
 * Key COBOL logic replaced:
 * - VSAM READ on TRANSACT file → JPA findById
 * - VSAM WRITE on TRANSACT file → JPA save (new record)
 * - VSAM STARTBR/READNEXT browse → JPA paginated queries
 * - Transaction ID generation (COTRN02C uses timestamp-based ID)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;

    public PageResponse<TransactionResponse> listTransactions(
            String cardNumber, Long accountId,
            LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable) {

        Page<Transaction> page;
        if (cardNumber != null && startDate != null && endDate != null) {
            page = transactionRepository.findByCardNumAndDateRange(cardNumber, startDate, endDate, pageable);
        } else if (accountId != null) {
            page = transactionRepository.findByAccountId(accountId, pageable);
        } else if (cardNumber != null) {
            page = transactionRepository.findByCardNum(cardNumber, pageable);
        } else {
            page = transactionRepository.findAll(pageable);
        }
        return buildPageResponse(page);
    }

    public TransactionResponse getTransaction(String transactionId) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));
        return mapToResponse(txn);
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionCreateRequest request) {
        // Validate card exists
        cardRepository.findById(request.getCardNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Card", request.getCardNumber()));

        Transaction txn = Transaction.builder()
                .tranId(generateTransactionId())
                .typeCode(request.getTypeCode())
                .categoryCode(request.getCategoryCode())
                .source(request.getSource())
                .description(request.getDescription())
                .amount(request.getAmount())
                .merchantId(request.getMerchantId())
                .merchantName(request.getMerchantName())
                .merchantCity(request.getMerchantCity())
                .merchantZip(request.getMerchantZip())
                .cardNum(request.getCardNumber())
                .origTimestamp(LocalDateTime.now())
                .procTimestamp(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(txn);
        return mapToResponse(saved);
    }

    /**
     * Generate a unique transaction ID.
     * COBOL COTRN02C uses a timestamp-based ID format.
     * Java equivalent uses UUID truncated to 16 characters.
     */
    private String generateTransactionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
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

    private PageResponse<TransactionResponse> buildPageResponse(Page<Transaction> page) {
        return PageResponse.<TransactionResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
