package com.carddemo.transaction.service;

import com.carddemo.common.dto.PagedResponse;
import com.carddemo.common.dto.TransactionDto;
import com.carddemo.common.entity.Transaction;
import com.carddemo.common.entity.TransactionCategory;
import com.carddemo.common.entity.TransactionType;
import com.carddemo.common.exception.ResourceNotFoundException;
import com.carddemo.transaction.dto.CreateTransactionRequest;
import com.carddemo.transaction.dto.TransactionSummaryDto;
import com.carddemo.transaction.repository.TransactionCategoryRepository;
import com.carddemo.transaction.repository.TransactionRepository;
import com.carddemo.transaction.repository.TransactionTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              TransactionTypeRepository transactionTypeRepository,
                              TransactionCategoryRepository transactionCategoryRepository) {
        this.transactionRepository = transactionRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionCategoryRepository = transactionCategoryRepository;
    }

    public PagedResponse<TransactionDto> getAllTransactions(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Transaction> transactionPage = transactionRepository.findAll(pageable);

        List<TransactionDto> transactions = transactionPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<TransactionDto>builder()
                .content(transactions)
                .page(transactionPage.getNumber())
                .size(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .first(transactionPage.isFirst())
                .last(transactionPage.isLast())
                .build();
    }

    public TransactionDto getTransactionById(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "transactionId", transactionId));
        return mapToDto(transaction);
    }

    public PagedResponse<TransactionDto> getTransactionsByCardNumber(String cardNumber, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("originationTimestamp").descending());
        Page<Transaction> transactionPage = transactionRepository.findByCardNumber(cardNumber, pageable);

        List<TransactionDto> transactions = transactionPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<TransactionDto>builder()
                .content(transactions)
                .page(transactionPage.getNumber())
                .size(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .first(transactionPage.isFirst())
                .last(transactionPage.isLast())
                .build();
    }

    public List<TransactionDto> getTransactionsByCardAndDateRange(String cardNumber, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        return transactionRepository.findByCardNumberAndDateRange(cardNumber, startDateTime, endDateTime).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public PagedResponse<TransactionDto> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(page, size, Sort.by("originationTimestamp").descending());
        Page<Transaction> transactionPage = transactionRepository.findByDateRange(startDateTime, endDateTime, pageable);

        List<TransactionDto> transactions = transactionPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<TransactionDto>builder()
                .content(transactions)
                .page(transactionPage.getNumber())
                .size(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .first(transactionPage.isFirst())
                .last(transactionPage.isLast())
                .build();
    }

    @Transactional
    public TransactionDto createTransaction(CreateTransactionRequest request) {
        Transaction transaction = Transaction.builder()
                .transactionId(generateTransactionId())
                .transactionTypeCode(request.getTransactionTypeCode())
                .transactionCategoryCode(request.getTransactionCategoryCode())
                .transactionSource(request.getTransactionSource())
                .description(request.getDescription())
                .amount(request.getAmount())
                .merchantId(request.getMerchantId())
                .merchantName(request.getMerchantName())
                .merchantCity(request.getMerchantCity())
                .merchantZip(request.getMerchantZip())
                .cardNumber(request.getCardNumber())
                .originationTimestamp(LocalDateTime.now())
                .processingTimestamp(LocalDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);
        return mapToDto(transaction);
    }

    public List<TransactionDto> getTransactionsByType(String transactionTypeCode) {
        return transactionRepository.findByTransactionTypeCode(transactionTypeCode).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<TransactionDto> getTransactionsByMerchant(Long merchantId) {
        return transactionRepository.findByMerchantId(merchantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public TransactionSummaryDto getTransactionSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.toLocalDate().minusDays(7).atStartOfDay();
        LocalDateTime startOfMonth = now.toLocalDate().minusDays(30).atStartOfDay();

        Long totalTransactions = transactionRepository.count();
        Long transactionsToday = transactionRepository.countTransactionsSince(startOfDay);
        Long transactionsThisWeek = transactionRepository.countTransactionsSince(startOfWeek);
        Long transactionsThisMonth = transactionRepository.countTransactionsSince(startOfMonth);

        BigDecimal totalDebits = transactionRepository.sumDebitsSince(startOfMonth);
        BigDecimal totalCredits = transactionRepository.sumCreditsSince(startOfMonth);

        if (totalDebits == null) totalDebits = BigDecimal.ZERO;
        if (totalCredits == null) totalCredits = BigDecimal.ZERO;

        return TransactionSummaryDto.builder()
                .totalTransactions(totalTransactions)
                .transactionsToday(transactionsToday != null ? transactionsToday : 0L)
                .transactionsThisWeek(transactionsThisWeek != null ? transactionsThisWeek : 0L)
                .transactionsThisMonth(transactionsThisMonth != null ? transactionsThisMonth : 0L)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .netAmount(totalDebits.subtract(totalCredits))
                .build();
    }

    public List<TransactionType> getAllTransactionTypes() {
        return transactionTypeRepository.findAll();
    }

    public List<TransactionCategory> getTransactionCategories(String transactionTypeCode) {
        return transactionCategoryRepository.findByTransactionTypeCode(transactionTypeCode);
    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private TransactionDto mapToDto(Transaction transaction) {
        String typeDescription = transactionTypeRepository.findById(transaction.getTransactionTypeCode())
                .map(TransactionType::getDescription)
                .orElse(null);

        TransactionCategory.TransactionCategoryId categoryId = new TransactionCategory.TransactionCategoryId(
                transaction.getTransactionTypeCode(), transaction.getTransactionCategoryCode());
        String categoryDescription = transactionCategoryRepository.findById(categoryId)
                .map(TransactionCategory::getDescription)
                .orElse(null);

        String maskedCardNumber = transaction.getCardNumber() != null && transaction.getCardNumber().length() >= 4
                ? "**** **** **** " + transaction.getCardNumber().substring(transaction.getCardNumber().length() - 4)
                : null;

        return TransactionDto.builder()
                .transactionId(transaction.getTransactionId())
                .transactionTypeCode(transaction.getTransactionTypeCode())
                .transactionTypeDescription(typeDescription)
                .transactionCategoryCode(transaction.getTransactionCategoryCode())
                .transactionCategoryDescription(categoryDescription)
                .transactionSource(transaction.getTransactionSource())
                .description(transaction.getDescription())
                .amount(transaction.getAmount())
                .merchantId(transaction.getMerchantId())
                .merchantName(transaction.getMerchantName())
                .merchantCity(transaction.getMerchantCity())
                .merchantZip(transaction.getMerchantZip())
                .cardNumber(transaction.getCardNumber())
                .maskedCardNumber(maskedCardNumber)
                .originationTimestamp(transaction.getOriginationTimestamp())
                .processingTimestamp(transaction.getProcessingTimestamp())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
