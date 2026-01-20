package com.carddemo.transactiontype.service;

import com.carddemo.transactiontype.dto.TransactionCategoryDto;
import com.carddemo.transactiontype.entity.TransactionCategory;
import com.carddemo.transactiontype.repository.TransactionCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionCategoryService {
    
    private final TransactionCategoryRepository categoryRepository;
    
    public List<TransactionCategory> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    public List<TransactionCategory> getActiveCategories() {
        return categoryRepository.findActiveCategoriesOrdered();
    }
    
    public Optional<TransactionCategory> getCategoryByCode(String categoryCode) {
        return categoryRepository.findById(categoryCode);
    }
    
    public List<TransactionCategory> getRootCategories() {
        return categoryRepository.findRootCategories();
    }
    
    public List<TransactionCategory> getChildCategories(String parentCode) {
        return categoryRepository.findByParentCategoryCode(parentCode);
    }
    
    public List<TransactionCategory> getCategoriesByType(String categoryType) {
        return categoryRepository.findByCategoryType(categoryType);
    }
    
    public List<TransactionCategory> getCategoriesByReportingGroup(String reportingGroup) {
        return categoryRepository.findByReportingGroup(reportingGroup);
    }
    
    @Transactional
    public TransactionCategory createCategory(TransactionCategoryDto dto) {
        TransactionCategory category = new TransactionCategory();
        mapDtoToEntity(dto, category);
        
        TransactionCategory saved = categoryRepository.save(category);
        log.info("Created transaction category: {}", saved.getCategoryCode());
        return saved;
    }
    
    @Transactional
    public Optional<TransactionCategory> updateCategory(String categoryCode, TransactionCategoryDto dto) {
        Optional<TransactionCategory> categoryOpt = categoryRepository.findById(categoryCode);
        if (categoryOpt.isEmpty()) {
            return Optional.empty();
        }
        
        TransactionCategory category = categoryOpt.get();
        mapDtoToEntity(dto, category);
        
        TransactionCategory saved = categoryRepository.save(category);
        log.info("Updated transaction category: {}", saved.getCategoryCode());
        return Optional.of(saved);
    }
    
    @Transactional
    public boolean deleteCategory(String categoryCode) {
        Optional<TransactionCategory> categoryOpt = categoryRepository.findById(categoryCode);
        if (categoryOpt.isEmpty()) {
            return false;
        }
        
        categoryRepository.delete(categoryOpt.get());
        log.info("Deleted transaction category: {}", categoryCode);
        return true;
    }
    
    @Transactional
    public Optional<TransactionCategory> toggleCategoryStatus(String categoryCode) {
        Optional<TransactionCategory> categoryOpt = categoryRepository.findById(categoryCode);
        if (categoryOpt.isEmpty()) {
            return Optional.empty();
        }
        
        TransactionCategory category = categoryOpt.get();
        category.setActive(!category.getActive());
        
        TransactionCategory saved = categoryRepository.save(category);
        log.info("Toggled transaction category {} to active={}", categoryCode, saved.getActive());
        return Optional.of(saved);
    }
    
    private void mapDtoToEntity(TransactionCategoryDto dto, TransactionCategory category) {
        category.setCategoryCode(dto.getCategoryCode());
        category.setCategoryDescription(dto.getCategoryDescription());
        category.setParentCategoryCode(dto.getParentCategoryCode());
        category.setCategoryType(dto.getCategoryType());
        category.setMerchantCategoryCode(dto.getMerchantCategoryCode());
        category.setReportingGroup(dto.getReportingGroup());
        category.setActive(dto.getActive() != null ? dto.getActive() : true);
        category.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
    }
}
