package com.aws.carddemo.service.transactiontype;

import com.aws.carddemo.domain.entity.TransactionType;
import com.aws.carddemo.domain.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Transaction Type Service - migrated from COTRTUPC (CTTU)
 * Handles transaction type CRUD operations
 * 
 * Original implementation used:
 * - DB2 with static embedded SQL
 * - Cursor processing for pagination
 * 
 * Migrated to:
 * - JPA with Pageable/Slice for pagination
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionTypeService {

    private final TransactionTypeRepository transactionTypeRepository;

    @Transactional(readOnly = true)
    public Page<TransactionTypeDTO> listTransactionTypes(Pageable pageable) {
        log.info("Listing transaction types with pagination");
        return transactionTypeRepository.findAll(pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public List<TransactionTypeDTO> listActiveTransactionTypes() {
        log.info("Listing active transaction types");
        return transactionTypeRepository.findByActiveTrue().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TransactionTypeDTO> getTransactionType(String typeCode) {
        log.info("Fetching transaction type: {}", typeCode);
        return transactionTypeRepository.findById(typeCode)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public List<TransactionTypeDTO> getByCategory(Integer categoryCode) {
        log.info("Fetching transaction types by category: {}", categoryCode);
        return transactionTypeRepository.findByCategoryCode(categoryCode).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public TransactionTypeDTO createTransactionType(TransactionTypeCreateRequest request) {
        log.info("Creating transaction type: {}", request.getTypeCode());

        if (transactionTypeRepository.existsById(request.getTypeCode())) {
            throw new TransactionTypeAlreadyExistsException("Transaction type already exists: " + request.getTypeCode());
        }

        TransactionType transactionType = TransactionType.builder()
                .typeCode(request.getTypeCode())
                .typeDescription(request.getTypeDescription())
                .categoryCode(request.getCategoryCode())
                .categoryDescription(request.getCategoryDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        TransactionType saved = transactionTypeRepository.save(transactionType);
        log.info("Transaction type created successfully: {}", request.getTypeCode());
        
        return mapToDTO(saved);
    }

    @Transactional
    public TransactionTypeDTO updateTransactionType(String typeCode, TransactionTypeUpdateRequest request) {
        log.info("Updating transaction type: {}", typeCode);

        TransactionType transactionType = transactionTypeRepository.findById(typeCode)
                .orElseThrow(() -> new TransactionTypeNotFoundException("Transaction type not found: " + typeCode));

        if (request.getTypeDescription() != null) {
            transactionType.setTypeDescription(request.getTypeDescription());
        }
        if (request.getCategoryCode() != null) {
            transactionType.setCategoryCode(request.getCategoryCode());
        }
        if (request.getCategoryDescription() != null) {
            transactionType.setCategoryDescription(request.getCategoryDescription());
        }
        if (request.getActive() != null) {
            transactionType.setActive(request.getActive());
        }

        TransactionType saved = transactionTypeRepository.save(transactionType);
        log.info("Transaction type updated successfully: {}", typeCode);
        
        return mapToDTO(saved);
    }

    @Transactional
    public void deleteTransactionType(String typeCode) {
        log.info("Deleting transaction type: {}", typeCode);

        if (!transactionTypeRepository.existsById(typeCode)) {
            throw new TransactionTypeNotFoundException("Transaction type not found: " + typeCode);
        }

        transactionTypeRepository.deleteById(typeCode);
        log.info("Transaction type deleted successfully: {}", typeCode);
    }

    @Transactional
    public void deactivateTransactionType(String typeCode) {
        log.info("Deactivating transaction type: {}", typeCode);

        TransactionType transactionType = transactionTypeRepository.findById(typeCode)
                .orElseThrow(() -> new TransactionTypeNotFoundException("Transaction type not found: " + typeCode));

        transactionType.setActive(false);
        transactionTypeRepository.save(transactionType);
        log.info("Transaction type deactivated successfully: {}", typeCode);
    }

    @Transactional(readOnly = true)
    public Page<TransactionTypeDTO> searchByDescription(String keyword, Pageable pageable) {
        log.info("Searching transaction types by description: {}", keyword);
        return transactionTypeRepository.searchByDescription(keyword, pageable)
                .map(this::mapToDTO);
    }

    private TransactionTypeDTO mapToDTO(TransactionType transactionType) {
        return TransactionTypeDTO.builder()
                .typeCode(transactionType.getTypeCode())
                .typeDescription(transactionType.getTypeDescription())
                .categoryCode(transactionType.getCategoryCode())
                .categoryDescription(transactionType.getCategoryDescription())
                .active(transactionType.getActive())
                .build();
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class TransactionTypeDTO {
        private String typeCode;
        private String typeDescription;
        private Integer categoryCode;
        private String categoryDescription;
        private Boolean active;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class TransactionTypeCreateRequest {
        private String typeCode;
        private String typeDescription;
        private Integer categoryCode;
        private String categoryDescription;
        private Boolean active;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class TransactionTypeUpdateRequest {
        private String typeDescription;
        private Integer categoryCode;
        private String categoryDescription;
        private Boolean active;
    }

    public static class TransactionTypeNotFoundException extends RuntimeException {
        public TransactionTypeNotFoundException(String message) {
            super(message);
        }
    }

    public static class TransactionTypeAlreadyExistsException extends RuntimeException {
        public TransactionTypeAlreadyExistsException(String message) {
            super(message);
        }
    }
}
