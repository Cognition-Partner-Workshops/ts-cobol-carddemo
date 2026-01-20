package com.carddemo.transactiontype.service;

import com.carddemo.transactiontype.dto.TransactionTypeDto;
import com.carddemo.transactiontype.entity.TransactionType;
import com.carddemo.transactiontype.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionTypeService {
    
    private final TransactionTypeRepository typeRepository;
    
    public List<TransactionType> getAllTypes() {
        return typeRepository.findAll();
    }
    
    public List<TransactionType> getActiveTypes() {
        return typeRepository.findActiveTypesOrdered();
    }
    
    public Optional<TransactionType> getTypeByCode(String typeCode) {
        return typeRepository.findById(typeCode);
    }
    
    public List<TransactionType> getTypesByCategory(String categoryCode) {
        return typeRepository.findByCategoryCode(categoryCode);
    }
    
    public List<TransactionType> getDebitTypes() {
        return typeRepository.findByDebitCreditIndicator("D");
    }
    
    public List<TransactionType> getCreditTypes() {
        return typeRepository.findByDebitCreditIndicator("C");
    }
    
    @Transactional
    public TransactionType createType(TransactionTypeDto dto) {
        TransactionType type = new TransactionType();
        mapDtoToEntity(dto, type);
        
        TransactionType saved = typeRepository.save(type);
        log.info("Created transaction type: {}", saved.getTypeCode());
        return saved;
    }
    
    @Transactional
    public Optional<TransactionType> updateType(String typeCode, TransactionTypeDto dto) {
        Optional<TransactionType> typeOpt = typeRepository.findById(typeCode);
        if (typeOpt.isEmpty()) {
            return Optional.empty();
        }
        
        TransactionType type = typeOpt.get();
        mapDtoToEntity(dto, type);
        
        TransactionType saved = typeRepository.save(type);
        log.info("Updated transaction type: {}", saved.getTypeCode());
        return Optional.of(saved);
    }
    
    @Transactional
    public boolean deleteType(String typeCode) {
        Optional<TransactionType> typeOpt = typeRepository.findById(typeCode);
        if (typeOpt.isEmpty()) {
            return false;
        }
        
        typeRepository.delete(typeOpt.get());
        log.info("Deleted transaction type: {}", typeCode);
        return true;
    }
    
    @Transactional
    public Optional<TransactionType> toggleTypeStatus(String typeCode) {
        Optional<TransactionType> typeOpt = typeRepository.findById(typeCode);
        if (typeOpt.isEmpty()) {
            return Optional.empty();
        }
        
        TransactionType type = typeOpt.get();
        type.setActive(!type.getActive());
        
        TransactionType saved = typeRepository.save(type);
        log.info("Toggled transaction type {} to active={}", typeCode, saved.getActive());
        return Optional.of(saved);
    }
    
    private void mapDtoToEntity(TransactionTypeDto dto, TransactionType type) {
        type.setTypeCode(dto.getTypeCode());
        type.setTypeDescription(dto.getTypeDescription());
        type.setDebitCreditIndicator(dto.getDebitCreditIndicator());
        type.setCategoryCode(dto.getCategoryCode());
        type.setAffectsBalance(dto.getAffectsBalance() != null ? dto.getAffectsBalance() : true);
        type.setRequiresApproval(dto.getRequiresApproval() != null ? dto.getRequiresApproval() : false);
        type.setMaxAmount(dto.getMaxAmount());
        type.setMinAmount(dto.getMinAmount());
        type.setFeePercentage(dto.getFeePercentage());
        type.setFlatFee(dto.getFlatFee());
        type.setActive(dto.getActive() != null ? dto.getActive() : true);
        type.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
    }
}
