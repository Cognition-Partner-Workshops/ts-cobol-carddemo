package com.aws.carddemo.service;

import com.aws.carddemo.dto.TransactionTypeDto;
import com.aws.carddemo.entity.TransactionType;
import com.aws.carddemo.entity.TransactionTypeCategory;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.repository.TransactionTypeCategoryRepository;
import com.aws.carddemo.repository.TransactionTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionTypeService {

    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionTypeCategoryRepository categoryRepository;

    public TransactionTypeService(TransactionTypeRepository transactionTypeRepository,
                                   TransactionTypeCategoryRepository categoryRepository) {
        this.transactionTypeRepository = transactionTypeRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public TransactionTypeDto getTransactionType(String typeCode) {
        TransactionType type = transactionTypeRepository.findByTypeCodeWithCategory(typeCode)
                .orElseThrow(() -> new ResourceNotFoundException("TransactionType", "typeCode", typeCode));
        return toDto(type);
    }

    @Transactional(readOnly = true)
    public List<TransactionTypeDto> getAllTransactionTypes() {
        return transactionTypeRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionTypeDto> getActiveTransactionTypes() {
        return transactionTypeRepository.findAllActiveTypes().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionTypeDto> getTransactionTypesByCategory(Integer categoryId) {
        return transactionTypeRepository.findByCategoryCategoryId(categoryId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionTypeDto createTransactionType(TransactionTypeDto dto) {
        TransactionType type = new TransactionType();
        type.setTypeCode(dto.getTypeCode());
        type.setTypeName(dto.getTypeName());
        type.setTypeDesc(dto.getTypeDesc());
        type.setActive(dto.getActive() != null ? dto.getActive() : true);

        if (dto.getCategoryId() != null) {
            TransactionTypeCategory category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", dto.getCategoryId()));
            type.setCategory(category);
        }

        type = transactionTypeRepository.save(type);
        return toDto(type);
    }

    @Transactional
    public TransactionTypeDto updateTransactionType(String typeCode, TransactionTypeDto dto) {
        TransactionType type = transactionTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new ResourceNotFoundException("TransactionType", "typeCode", typeCode));

        type.setTypeName(dto.getTypeName());
        type.setTypeDesc(dto.getTypeDesc());
        if (dto.getActive() != null) {
            type.setActive(dto.getActive());
        }

        if (dto.getCategoryId() != null) {
            TransactionTypeCategory category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", dto.getCategoryId()));
            type.setCategory(category);
        }

        type = transactionTypeRepository.save(type);
        return toDto(type);
    }

    @Transactional
    public void deactivateTransactionType(String typeCode) {
        TransactionType type = transactionTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new ResourceNotFoundException("TransactionType", "typeCode", typeCode));
        type.setActive(false);
        transactionTypeRepository.save(type);
    }

    private TransactionTypeDto toDto(TransactionType type) {
        return TransactionTypeDto.builder()
                .typeId(type.getTypeId())
                .typeCode(type.getTypeCode())
                .typeName(type.getTypeName())
                .typeDesc(type.getTypeDesc())
                .categoryId(type.getCategory() != null ? type.getCategory().getCategoryId() : null)
                .categoryName(type.getCategory() != null ? type.getCategory().getCategoryName() : null)
                .active(type.getActive())
                .createdAt(type.getCreatedAt())
                .updatedAt(type.getUpdatedAt())
                .build();
    }
}
