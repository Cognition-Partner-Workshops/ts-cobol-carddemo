package com.carddemo.authorization.service;

import com.carddemo.authorization.dto.AuthorizationRuleDto;
import com.carddemo.authorization.entity.AuthorizationRule;
import com.carddemo.authorization.repository.AuthorizationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationRuleService {
    
    private final AuthorizationRuleRepository ruleRepository;
    
    public List<AuthorizationRule> getAllRules() {
        return ruleRepository.findAll();
    }
    
    public List<AuthorizationRule> getActiveRules() {
        return ruleRepository.findActiveRulesOrderByPriority();
    }
    
    public Optional<AuthorizationRule> getRuleByCode(String ruleCode) {
        return ruleRepository.findByRuleCode(ruleCode);
    }
    
    public List<AuthorizationRule> getRulesByType(String ruleType) {
        return ruleRepository.findByRuleType(ruleType);
    }
    
    @Transactional
    public AuthorizationRule createRule(AuthorizationRuleDto dto) {
        AuthorizationRule rule = new AuthorizationRule();
        mapDtoToEntity(dto, rule);
        
        AuthorizationRule saved = ruleRepository.save(rule);
        log.info("Created authorization rule: {}", saved.getRuleCode());
        return saved;
    }
    
    @Transactional
    public Optional<AuthorizationRule> updateRule(String ruleCode, AuthorizationRuleDto dto) {
        Optional<AuthorizationRule> ruleOpt = ruleRepository.findByRuleCode(ruleCode);
        if (ruleOpt.isEmpty()) {
            return Optional.empty();
        }
        
        AuthorizationRule rule = ruleOpt.get();
        mapDtoToEntity(dto, rule);
        
        AuthorizationRule saved = ruleRepository.save(rule);
        log.info("Updated authorization rule: {}", saved.getRuleCode());
        return Optional.of(saved);
    }
    
    @Transactional
    public boolean deleteRule(String ruleCode) {
        Optional<AuthorizationRule> ruleOpt = ruleRepository.findByRuleCode(ruleCode);
        if (ruleOpt.isEmpty()) {
            return false;
        }
        
        ruleRepository.delete(ruleOpt.get());
        log.info("Deleted authorization rule: {}", ruleCode);
        return true;
    }
    
    @Transactional
    public Optional<AuthorizationRule> toggleRuleStatus(String ruleCode) {
        Optional<AuthorizationRule> ruleOpt = ruleRepository.findByRuleCode(ruleCode);
        if (ruleOpt.isEmpty()) {
            return Optional.empty();
        }
        
        AuthorizationRule rule = ruleOpt.get();
        rule.setActive(!rule.getActive());
        
        AuthorizationRule saved = ruleRepository.save(rule);
        log.info("Toggled authorization rule {} to active={}", ruleCode, saved.getActive());
        return Optional.of(saved);
    }
    
    private void mapDtoToEntity(AuthorizationRuleDto dto, AuthorizationRule rule) {
        rule.setRuleCode(dto.getRuleCode());
        rule.setRuleName(dto.getRuleName());
        rule.setRuleType(dto.getRuleType());
        rule.setDescription(dto.getDescription());
        rule.setMerchantCategoryCode(dto.getMerchantCategoryCode());
        rule.setTransactionType(dto.getTransactionType());
        rule.setMinAmount(dto.getMinAmount());
        rule.setMaxAmount(dto.getMaxAmount());
        rule.setDailyLimit(dto.getDailyLimit());
        rule.setMonthlyLimit(dto.getMonthlyLimit());
        rule.setVelocityCount(dto.getVelocityCount());
        rule.setVelocityPeriodMinutes(dto.getVelocityPeriodMinutes());
        rule.setCountryRestriction(dto.getCountryRestriction());
        rule.setAction(dto.getAction());
        rule.setPriority(dto.getPriority());
        rule.setActive(dto.getActive() != null ? dto.getActive() : true);
    }
}
