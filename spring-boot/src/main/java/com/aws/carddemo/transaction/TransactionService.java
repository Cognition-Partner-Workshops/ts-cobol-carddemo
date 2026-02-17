package com.aws.carddemo.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aws.carddemo.account.Account;
import com.aws.carddemo.account.AccountRepository;
import com.aws.carddemo.billing.CategoryBalance;
import com.aws.carddemo.billing.CategoryBalanceId;
import com.aws.carddemo.billing.CategoryBalanceRepository;
import com.aws.carddemo.card.Card;
import com.aws.carddemo.card.CardRepository;
import com.aws.carddemo.card.CardXref;
import com.aws.carddemo.card.CardXrefRepository;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;
import com.aws.carddemo.transaction.dto.TransactionCreateRequest;
import com.aws.carddemo.transaction.dto.TransactionListItemResponse;
import com.aws.carddemo.transaction.dto.TransactionResponse;

@Service
@Transactional
public class TransactionService {

    private final TransactionRecordRepository transactionRecordRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CategoryBalanceRepository categoryBalanceRepository;

    public TransactionService(TransactionRecordRepository transactionRecordRepository,
                              TransactionTypeRepository transactionTypeRepository,
                              TransactionCategoryRepository transactionCategoryRepository,
                              CardXrefRepository cardXrefRepository,
                              AccountRepository accountRepository,
                              CardRepository cardRepository,
                              CategoryBalanceRepository categoryBalanceRepository) {
        this.transactionRecordRepository = transactionRecordRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionCategoryRepository = transactionCategoryRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.categoryBalanceRepository = categoryBalanceRepository;
    }

    @Transactional(readOnly = true)
    public Page<TransactionListItemResponse> listTransactions(String cardNumber,
                                                              LocalDateTime fromDate,
                                                              LocalDateTime toDate,
                                                              String transactionType,
                                                              BigDecimal minAmount,
                                                              BigDecimal maxAmount,
                                                              Pageable pageable) {
        Pageable sortedPageable = ensureDefaultSort(pageable);

        Specification<TransactionRecord> spec = Specification
                .where(TransactionRecordSpecifications.hasCardNumber(cardNumber));

        if (fromDate != null) {
            spec = spec.and(TransactionRecordSpecifications.timestampAfter(fromDate));
        }
        if (toDate != null) {
            spec = spec.and(TransactionRecordSpecifications.timestampBefore(toDate));
        }
        if (transactionType != null) {
            spec = spec.and(TransactionRecordSpecifications.hasTransactionType(transactionType));
        }
        if (minAmount != null) {
            spec = spec.and(TransactionRecordSpecifications.amountGreaterThanOrEqual(minAmount));
        }
        if (maxAmount != null) {
            spec = spec.and(TransactionRecordSpecifications.amountLessThanOrEqual(maxAmount));
        }

        return transactionRecordRepository.findAll(spec, sortedPageable)
                .map(TransactionListItemResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long id) {
        TransactionRecord record = transactionRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        String typeDescription = transactionTypeRepository.findById(record.getTransactionType())
                .map(TransactionType::getTypeDesc)
                .orElse(null);

        String categoryDescription = record.getTransactionCategory() != null
                ? transactionCategoryRepository.findById(record.getTransactionCategory())
                        .map(TransactionCategory::getCatDesc)
                        .orElse(null)
                : null;

        return TransactionResponse.from(record, typeDescription, categoryDescription);
    }

    public TransactionResponse createTransaction(TransactionCreateRequest request) {
        if (!Boolean.TRUE.equals(request.confirmed())) {
            throw new ValidationException("Transaction must be confirmed");
        }

        CardXref cardXref = cardXrefRepository.findById(request.cardNumber())
                .orElseThrow(() -> new ValidationException(
                        "Card number not found: " + request.cardNumber()));

        Account account = cardXref.getAccount();
        if (!"A".equals(account.getAccountStatus())) {
            throw new ValidationException("Account is not active. Current status: " + account.getAccountStatus());
        }

        TransactionType transactionType = transactionTypeRepository.findById(request.typeCode())
                .orElseThrow(() -> new ValidationException(
                        "Invalid transaction type code: " + request.typeCode()));

        TransactionCategory transactionCategory = transactionCategoryRepository.findById(request.categoryCode())
                .orElseThrow(() -> new ValidationException(
                        "Invalid transaction category code: " + request.categoryCode()));

        Card card = cardRepository.findByCardNumber(request.cardNumber())
                .orElseThrow(() -> new ValidationException(
                        "Card not found: " + request.cardNumber()));

        TransactionRecord record = new TransactionRecord();
        record.setCard(card);
        record.setTransactionType(request.typeCode());
        record.setTransactionCategory(request.categoryCode());
        record.setTransactionSource(request.source());
        record.setDescription(request.description());
        record.setAmount(request.amount());
        record.setTimestamp(request.processingDate() != null ? request.processingDate() : LocalDateTime.now());
        record.setMerchantId(request.merchantId());
        record.setMerchantName(request.merchantName());
        record.setMerchantCity(request.merchantCity());
        record.setMerchantZip(request.merchantZip());

        TransactionRecord saved = transactionRecordRepository.save(record);

        updateCategoryBalance(account, transactionCategory, request.amount());

        return TransactionResponse.from(saved, transactionType.getTypeDesc(), transactionCategory.getCatDesc());
    }

    private void updateCategoryBalance(Account account, TransactionCategory category, BigDecimal amount) {
        CategoryBalanceId balanceId = new CategoryBalanceId(account.getId(), category.getCatCd());
        CategoryBalance balance = categoryBalanceRepository.findById(balanceId)
                .orElseGet(() -> {
                    CategoryBalance newBalance = new CategoryBalance();
                    newBalance.setAccount(account);
                    newBalance.setCategory(category);
                    newBalance.setBalance(BigDecimal.ZERO);
                    return newBalance;
                });
        balance.setBalance(balance.getBalance().add(amount));
        categoryBalanceRepository.save(balance);
    }

    private Pageable ensureDefaultSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "timestamp"));
        }
        return pageable;
    }
}
