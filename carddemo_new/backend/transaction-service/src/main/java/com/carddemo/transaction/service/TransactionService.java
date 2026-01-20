package com.carddemo.transaction.service;

import com.carddemo.common.dto.PageResponse;
import com.carddemo.common.exception.BusinessException;
import com.carddemo.common.exception.ResourceNotFoundException;
import com.carddemo.transaction.dto.TransactionCreateRequest;
import com.carddemo.transaction.dto.TransactionDto;
import com.carddemo.transaction.entity.Transaction;
import com.carddemo.transaction.entity.TransactionCategory;
import com.carddemo.transaction.entity.TransactionType;
import com.carddemo.transaction.repository.TransactionCategoryRepository;
import com.carddemo.transaction.repository.TransactionRepository;
import com.carddemo.transaction.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;

    public TransactionDto getTransactionById(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "transactionId", transactionId));
        return mapToTransactionDto(transaction);
    }

    public PageResponse<TransactionDto> getTransactionsByAccount(String accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("originalTimestamp").descending());
        Page<Transaction> transactionPage = transactionRepository.findByAccountId(accountId, pageable);
        return buildPageResponse(transactionPage);
    }

    public PageResponse<TransactionDto> getTransactionsByCard(String cardNumber, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("originalTimestamp").descending());
        Page<Transaction> transactionPage = transactionRepository.findByCardNumber(cardNumber, pageable);
        return buildPageResponse(transactionPage);
    }

    public PageResponse<TransactionDto> getTransactionsByAccountAndDateRange(
            String accountId, LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("originalTimestamp").descending());
        Page<Transaction> transactionPage = transactionRepository.findByAccountIdAndDateRange(
                accountId, startDate, endDate, pageable);
        return buildPageResponse(transactionPage);
    }

    public PageResponse<TransactionDto> getAllTransactions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("originalTimestamp").descending());
        Page<Transaction> transactionPage = transactionRepository.findAll(pageable);
        return buildPageResponse(transactionPage);
    }

    @Transactional
    public TransactionDto createTransaction(TransactionCreateRequest request) {
        TransactionType type = transactionTypeRepository.findById(request.getTypeCode())
                .orElseThrow(() -> new BusinessException("Invalid transaction type code: " + request.getTypeCode(), "INVALID_TYPE_CODE"));

        TransactionCategory category = transactionCategoryRepository.findById(request.getCategoryCode())
                .orElseThrow(() -> new BusinessException("Invalid category code: " + request.getCategoryCode(), "INVALID_CATEGORY_CODE"));

        String transactionId = generateTransactionId();

        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .cardNumber(request.getCardNumber())
                .accountId(extractAccountIdFromCard(request.getCardNumber()))
                .typeCode(request.getTypeCode())
                .categoryCode(request.getCategoryCode())
                .amount(request.getAmount())
                .description(request.getDescription())
                .merchantId(request.getMerchantId())
                .merchantName(request.getMerchantName())
                .merchantCity(request.getMerchantCity())
                .merchantZip(request.getMerchantZip())
                .source("ONLINE")
                .status("PENDING")
                .originalTimestamp(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        return mapToTransactionDto(savedTransaction);
    }

    public List<TransactionType> getAllTransactionTypes() {
        return transactionTypeRepository.findAll();
    }

    public List<TransactionCategory> getAllTransactionCategories() {
        return transactionCategoryRepository.findAll();
    }

    private PageResponse<TransactionDto> buildPageResponse(Page<Transaction> transactionPage) {
        List<TransactionDto> transactions = transactionPage.getContent().stream()
                .map(this::mapToTransactionDto)
                .collect(Collectors.toList());

        return PageResponse.<TransactionDto>builder()
                .content(transactions)
                .pageNumber(transactionPage.getNumber())
                .pageSize(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .first(transactionPage.isFirst())
                .last(transactionPage.isLast())
                .build();
    }

    private TransactionDto mapToTransactionDto(Transaction transaction) {
        String typeDescription = transactionTypeRepository.findById(transaction.getTypeCode())
                .map(TransactionType::getDescription)
                .orElse(null);

        String categoryDescription = transactionCategoryRepository.findById(transaction.getCategoryCode())
                .map(TransactionCategory::getDescription)
                .orElse(null);

        return TransactionDto.builder()
                .transactionId(transaction.getTransactionId())
                .typeCode(transaction.getTypeCode())
                .typeDescription(typeDescription)
                .categoryCode(transaction.getCategoryCode())
                .categoryDescription(categoryDescription)
                .source(transaction.getSource())
                .description(transaction.getDescription())
                .amount(transaction.getAmount())
                .merchantId(transaction.getMerchantId())
                .merchantName(transaction.getMerchantName())
                .merchantCity(transaction.getMerchantCity())
                .merchantZip(transaction.getMerchantZip())
                .cardNumber(transaction.getCardNumber())
                .maskedCardNumber(maskCardNumber(transaction.getCardNumber()))
                .accountId(transaction.getAccountId())
                .originalTimestamp(transaction.getOriginalTimestamp())
                .processedTimestamp(transaction.getProcessedTimestamp())
                .status(transaction.getStatus())
                .build();
    }

    private String generateTransactionId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 2).toUpperCase();
        return timestamp + random;
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    private String extractAccountIdFromCard(String cardNumber) {
        return "00000000001";
    }
}
