package com.carddemo.authorization.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationRuleDto {
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private String description;
    private String merchantCategoryCode;
    private String transactionType;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private Integer velocityCount;
    private Integer velocityPeriodMinutes;
    private String countryRestriction;
    private String action;
    private Integer priority;
    private Boolean active;
}
