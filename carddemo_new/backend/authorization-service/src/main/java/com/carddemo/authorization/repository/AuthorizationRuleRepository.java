package com.carddemo.authorization.repository;

import com.carddemo.authorization.entity.AuthorizationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorizationRuleRepository extends JpaRepository<AuthorizationRule, Long> {
    Optional<AuthorizationRule> findByRuleCode(String ruleCode);
    
    List<AuthorizationRule> findByRuleType(String ruleType);
    
    List<AuthorizationRule> findByActiveTrue();
    
    @Query("SELECT r FROM AuthorizationRule r WHERE r.active = true ORDER BY r.priority ASC")
    List<AuthorizationRule> findActiveRulesOrderByPriority();
    
    @Query("SELECT r FROM AuthorizationRule r WHERE r.active = true AND (r.merchantCategoryCode = :mcc OR r.merchantCategoryCode IS NULL) ORDER BY r.priority ASC")
    List<AuthorizationRule> findApplicableRules(@Param("mcc") String merchantCategoryCode);
}
