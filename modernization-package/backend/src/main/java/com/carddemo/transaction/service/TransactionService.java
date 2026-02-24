package com.carddemo.transaction.service;

import com.carddemo.transaction.dto.*;
import com.carddemo.transaction.entity.CardCrossReference;
import com.carddemo.transaction.entity.Transaction;
import com.carddemo.transaction.exception.DuplicateTransactionException;
import com.carddemo.transaction.exception.ResourceNotFoundException;
import com.carddemo.transaction.exception.ValidationException;
import com.carddemo.transaction.repository.CardCrossReferenceRepository;
import com.carddemo.transaction.repository.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service layer for Transaction Processing Module.
 * Implements all 30 business rules with exact logic parity to the legacy COBOL system.
 *
 * Key responsibilities:
 * - CT00 List: Paginated transaction listing with filter (BR-LT-01 through BR-LT-08)
 * - CT01 View: Transaction detail lookup (BR-VT-01 through BR-VT-05)
 * - CT02 Add: 6-phase validation chain + confirmation + ID generation (BR-AT-01 through BR-AT-14)
 * - Cross-reference resolution (BR-AT-04, BR-AT-05)
 */
@Service
public class TransactionService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^[+-]?\\d{1,8}\\.\\d{2}$");
    private static final DateTimeFormatter STRICT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private final TransactionRepository transactionRepository;
    private final CardCrossReferenceRepository xrefRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CardCrossReferenceRepository xrefRepository) {
        this.transactionRepository = transactionRepository;
        this.xrefRepository = xrefRepository;
    }

    // ========================================================================
    // CT00 - List Transactions (BR-LT-01 through BR-LT-08)
    // ========================================================================

    /**
     * List transactions with pagination and optional filter.
     * BR-LT-01: Page size fixed at 10
     * BR-LT-02: Numeric filter validation
     * BR-LT-04: Empty filter browses from start
     * BR-LT-05/06: Boundary detection via first/last flags
     * BR-LT-07: Page state preservation
     */
    public TransactionListResponse listTransactions(int page, int size, String startTransactionId) {
        // BR-LT-02: Validate numeric filter
        if (startTransactionId != null && !startTransactionId.isBlank()) {
            if (!NUMERIC_PATTERN.matcher(startTransactionId).matches()) {
                throw new IllegalArgumentException("Tran ID must be Numeric ...");
            }
        }

        // Enforce page size default (BR-LT-01)
        if (size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage;

        if (startTransactionId != null && !startTransactionId.isBlank()) {
            // BR-LT-04: Filter by starting Transaction ID
            String paddedId = String.format("%-16s", startTransactionId).replace(' ', '0');
            if (startTransactionId.length() <= 16) {
                paddedId = String.format("%016d", Long.parseLong(startTransactionId));
            }
            transactionPage = transactionRepository
                    .findByTransactionIdGreaterThanEqualOrderByTransactionIdAsc(paddedId, pageable);
        } else {
            // BR-LT-04: Empty filter browses from start
            transactionPage = transactionRepository.findAllByOrderByTransactionIdAsc(pageable);
        }

        List<TransactionSummaryDto> content = transactionPage.getContent().stream()
                .map(this::toSummaryDto)
                .toList();

        TransactionListResponse response = new TransactionListResponse();
        response.setContent(content);
        response.setPage(transactionPage.getNumber());
        response.setSize(transactionPage.getSize());
        response.setTotalElements(transactionPage.getTotalElements());
        response.setTotalPages(transactionPage.getTotalPages());
        response.setFirst(transactionPage.isFirst());
        response.setLast(transactionPage.isLast());
        response.setHasNext(transactionPage.hasNext());
        response.setHasPrevious(transactionPage.hasPrevious());

        return response;
    }

    // ========================================================================
    // CT01 - View Transaction (BR-VT-01 through BR-VT-05)
    // ========================================================================

    /**
     * View a single transaction by ID.
     * BR-VT-01: Transaction ID required + not found handling
     * BR-VT-02: Read without lock (standard SELECT)
     * BR-VT-03: All 13 fields returned
     * BR-VT-04: Read-only (GET method)
     */
    public TransactionDetailResponse viewTransaction(String transactionId) {
        // BR-VT-01: Transaction ID required
        if (transactionId == null || transactionId.isBlank()) {
            throw new ResourceNotFoundException(
                    "Tran ID can NOT be empty...", "transactionId", "BR-VT-01");
        }

        // BR-VT-02: Read record
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction ID NOT found...", "transactionId", "BR-VT-01"));

        return toDetailResponse(transaction);
    }

    // ========================================================================
    // CT02 - Add Transaction (BR-AT-01 through BR-AT-14)
    // ========================================================================

    /**
     * Add a new transaction with full 6-phase validation chain.
     * Returns either:
     * - AddTransactionResponse (201) if confirmed and written
     * - ConfirmationRequiredResponse (200) if validation passed but not confirmed
     *
     * @return Object - either AddTransactionResponse or ConfirmationRequiredResponse
     */
    @Transactional
    public Object addTransaction(AddTransactionRequest request) {
        // ====================================================================
        // Phase 1: Key Field Validation (BR-AT-01 through BR-AT-05)
        // ====================================================================
        String resolvedAccountId;
        String resolvedCardNumber;

        String accountId = request.getAccountId();
        String cardNumber = request.getCardNumber();
        boolean hasAccount = accountId != null && !accountId.isBlank();
        boolean hasCard = cardNumber != null && !cardNumber.isBlank();

        // BR-AT-01: Account or Card Required
        if (!hasAccount && !hasCard) {
            throw new ValidationException(
                    "Account or Card Number must be entered...",
                    1, "accountId", "BR-AT-01", 400);
        }

        if (hasAccount) {
            // BR-AT-02: Account ID Numeric
            if (!NUMERIC_PATTERN.matcher(accountId).matches()) {
                throw new ValidationException(
                        "Account ID must be Numeric...",
                        1, "accountId", "BR-AT-02", 400);
            }

            // BR-AT-04: Account Must Exist + BR-AT-05: Cross-Reference Resolution (Path A)
            BigDecimal acctIdDecimal = new BigDecimal(accountId);
            CardCrossReference xref = xrefRepository.findFirstByAccountId(acctIdDecimal)
                    .orElseThrow(() -> new ValidationException(
                            "Account ID NOT found...",
                            1, "accountId", "BR-AT-04", 404));
            resolvedAccountId = String.format("%011d", xref.getAccountId().longValue());
            resolvedCardNumber = xref.getCardNumber();
        } else {
            // BR-AT-03: Card Number Numeric
            if (!NUMERIC_PATTERN.matcher(cardNumber).matches()) {
                throw new ValidationException(
                        "Card Number must be Numeric...",
                        1, "cardNumber", "BR-AT-03", 400);
            }

            // BR-AT-04: Card Must Exist + BR-AT-05: Cross-Reference Resolution (Path B)
            CardCrossReference xref = xrefRepository.findByCardNumber(cardNumber)
                    .orElseThrow(() -> new ValidationException(
                            "Card Number NOT found...",
                            1, "cardNumber", "BR-AT-04", 404));
            resolvedAccountId = String.format("%011d", xref.getAccountId().longValue());
            resolvedCardNumber = xref.getCardNumber();
        }

        // ====================================================================
        // Phase 2: Mandatory Field Checks (BR-AT-06) - 11 fields
        // ====================================================================
        validateMandatoryField(request.getTypeCode(), "typeCode", "Type CD");
        validateMandatoryField(request.getCategoryCode(), "categoryCode", "Category CD");
        validateMandatoryField(request.getSource(), "source", "Source");
        validateMandatoryField(request.getDescription(), "description", "Description");
        validateMandatoryField(request.getAmount(), "amount", "Amount");
        validateMandatoryField(request.getOriginationDate(), "originationDate", "Orig Date");
        validateMandatoryField(request.getProcessingDate(), "processingDate", "Proc Date");
        validateMandatoryField(request.getMerchantId(), "merchantId", "Merchant ID");
        validateMandatoryField(request.getMerchantName(), "merchantName", "Merchant Name");
        validateMandatoryField(request.getMerchantCity(), "merchantCity", "Merchant City");
        validateMandatoryField(request.getMerchantZip(), "merchantZip", "Merchant Zip");

        // ====================================================================
        // Phase 3: Numeric Type Checks (BR-AT-07)
        // ====================================================================
        if (!NUMERIC_PATTERN.matcher(request.getTypeCode()).matches()) {
            throw new ValidationException(
                    "Type CD must be Numeric...",
                    3, "typeCode", "BR-AT-07", 400);
        }
        if (!NUMERIC_PATTERN.matcher(request.getCategoryCode()).matches()) {
            throw new ValidationException(
                    "Category CD must be Numeric...",
                    3, "categoryCode", "BR-AT-07", 400);
        }

        // ====================================================================
        // Phase 4: Amount Format Validation (BR-AT-08)
        // ====================================================================
        if (!AMOUNT_PATTERN.matcher(request.getAmount()).matches()) {
            throw new ValidationException(
                    "Amount should be in format -99999999.99",
                    4, "amount", "BR-AT-08", 400);
        }

        // ====================================================================
        // Phase 5: Date Validation (BR-AT-09, BR-AT-10)
        // ====================================================================
        validateDate(request.getOriginationDate(), "originationDate", "Orig Date");
        validateDate(request.getProcessingDate(), "processingDate", "Proc Date");

        // ====================================================================
        // Phase 6: Merchant ID Numeric Check (BR-AT-11)
        // ====================================================================
        if (!NUMERIC_PATTERN.matcher(request.getMerchantId()).matches()) {
            throw new ValidationException(
                    "Merchant ID must be Numeric...",
                    6, "merchantId", "BR-AT-11", 400);
        }

        // ====================================================================
        // Confirmation Gate (BR-AT-12)
        // ====================================================================
        String confirmation = request.getConfirmation();
        if (confirmation == null || confirmation.isBlank()
                || "N".equalsIgnoreCase(confirmation)) {
            ConfirmationRequiredResponse confirmResponse = new ConfirmationRequiredResponse();
            confirmResponse.setMessage("Confirm to add this transaction...");
            confirmResponse.setResolvedAccountId(resolvedAccountId);
            confirmResponse.setResolvedCardNumber(resolvedCardNumber);
            return confirmResponse;
        }

        if (!"Y".equalsIgnoreCase(confirmation)) {
            ConfirmationRequiredResponse confirmResponse = new ConfirmationRequiredResponse();
            confirmResponse.setMessage("Invalid value. Valid values are (Y/N)...");
            confirmResponse.setResolvedAccountId(resolvedAccountId);
            confirmResponse.setResolvedCardNumber(resolvedCardNumber);
            return confirmResponse;
        }

        // ====================================================================
        // ID Generation + Record Write (BR-AT-13, BR-AT-14)
        // ====================================================================
        String newTransactionId = transactionRepository.generateNextTransactionId();

        // BR-AT-14: Duplicate ID rejection
        if (transactionRepository.existsByTransactionId(newTransactionId)) {
            throw new DuplicateTransactionException("Tran ID already exist...");
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionId(newTransactionId);
        transaction.setCardNumber(resolvedCardNumber);
        transaction.setTypeCode(request.getTypeCode());
        transaction.setCategoryCode(new BigDecimal(request.getCategoryCode()));
        transaction.setSource(request.getSource());
        transaction.setDescription(request.getDescription());
        transaction.setAmount(new BigDecimal(request.getAmount()));
        transaction.setMerchantId(new BigDecimal(request.getMerchantId()));
        transaction.setMerchantName(request.getMerchantName());
        transaction.setMerchantCity(request.getMerchantCity());
        transaction.setMerchantZip(request.getMerchantZip());

        LocalDate origDate = LocalDate.parse(request.getOriginationDate(), STRICT_DATE_FORMAT);
        LocalDate procDate = LocalDate.parse(request.getProcessingDate(), STRICT_DATE_FORMAT);
        transaction.setOriginationTs(origDate.atStartOfDay());
        transaction.setProcessingTs(procDate.atStartOfDay());

        try {
            transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("duplicate")) {
                throw new DuplicateTransactionException("Tran ID already exist...");
            }
            throw new RuntimeException("Unable to Add Transaction...");
        }

        // Build success response
        TransactionDetailResponse detail = toDetailResponse(transaction);
        detail.setAccountId(resolvedAccountId);

        AddTransactionResponse response = new AddTransactionResponse();
        response.setTransactionId(newTransactionId);
        response.setMessage("Transaction added successfully. Your Tran ID is " + newTransactionId + ".");
        response.setTransaction(detail);
        return response;
    }

    // ========================================================================
    // Latest Transaction (PF5 Copy Last)
    // ========================================================================

    /**
     * Get the most recent transaction for Copy Last feature (US-AT-06).
     * Replaces legacy: STARTBR TRANSACT with HIGH-VALUES -> READPREV
     */
    public LatestTransactionResponse getLatestTransaction() {
        Transaction latest = transactionRepository.findFirstByOrderByTransactionIdDesc()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No transactions found", "transactionId", null));

        LatestTransactionResponse response = new LatestTransactionResponse();
        response.setTransactionId(latest.getTransactionId());
        response.setTypeCode(latest.getTypeCode());
        response.setCategoryCode(latest.getCategoryCode().intValue());
        response.setSource(latest.getSource());
        response.setDescription(latest.getDescription());
        response.setAmount(latest.getAmount());
        response.setOriginationDate(latest.getOriginationTs().toLocalDate().toString());
        response.setProcessingDate(latest.getProcessingTs().toLocalDate().toString());
        response.setMerchantId(latest.getMerchantId().longValue());
        response.setMerchantName(latest.getMerchantName());
        response.setMerchantCity(latest.getMerchantCity());
        response.setMerchantZip(latest.getMerchantZip());
        return response;
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Phase 2 helper: Validate a mandatory field (BR-AT-06).
     */
    private void validateMandatoryField(String value, String fieldName, String displayName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(
                    displayName + " can NOT be empty...",
                    2, fieldName, "BR-AT-06", 400);
        }
    }

    /**
     * Phase 5 helper: Validate date format (BR-AT-09) and calendar validity (BR-AT-10).
     */
    private void validateDate(String dateStr, String fieldName, String displayName) {
        // BR-AT-09: Date format validation (YYYY-MM-DD)
        if (!dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new ValidationException(
                    displayName + " - Date format must be YYYY-MM-DD...",
                    5, fieldName, "BR-AT-09", 400);
        }

        // BR-AT-10: Date calendar validity
        try {
            LocalDate.parse(dateStr, STRICT_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ValidationException(
                    displayName + " - Not a valid date...",
                    5, fieldName, "BR-AT-10", 400);
        }
    }

    /**
     * Map Transaction entity to TransactionSummaryDto (CT00 list view).
     */
    private TransactionSummaryDto toSummaryDto(Transaction tx) {
        TransactionSummaryDto dto = new TransactionSummaryDto();
        dto.setTransactionId(tx.getTransactionId());
        dto.setTypeCode(tx.getTypeCode());
        dto.setCategoryCode(tx.getCategoryCode().intValue());
        dto.setSource(tx.getSource());
        dto.setDescription(tx.getDescription());
        dto.setAmount(tx.getAmount());
        dto.setCardNumber(tx.getCardNumber());
        dto.setOriginationTimestamp(tx.getOriginationTs());
        dto.setProcessingTimestamp(tx.getProcessingTs());
        return dto;
    }

    /**
     * Map Transaction entity to TransactionDetailResponse (CT01 detail view).
     * Resolves Account ID from cross-reference for display.
     */
    private TransactionDetailResponse toDetailResponse(Transaction tx) {
        TransactionDetailResponse dto = new TransactionDetailResponse();
        dto.setTransactionId(tx.getTransactionId());
        dto.setCardNumber(tx.getCardNumber());
        dto.setTypeCode(tx.getTypeCode());
        dto.setCategoryCode(tx.getCategoryCode().intValue());
        dto.setSource(tx.getSource());
        dto.setDescription(tx.getDescription());
        dto.setAmount(tx.getAmount());
        dto.setMerchantId(tx.getMerchantId().longValue());
        dto.setMerchantName(tx.getMerchantName());
        dto.setMerchantCity(tx.getMerchantCity());
        dto.setMerchantZip(tx.getMerchantZip());
        dto.setOriginationTimestamp(tx.getOriginationTs());
        dto.setProcessingTimestamp(tx.getProcessingTs());

        // Resolve Account ID from cross-reference
        xrefRepository.findByCardNumber(tx.getCardNumber())
                .ifPresent(xref -> dto.setAccountId(
                        String.format("%011d", xref.getAccountId().longValue())));

        return dto;
    }
}
