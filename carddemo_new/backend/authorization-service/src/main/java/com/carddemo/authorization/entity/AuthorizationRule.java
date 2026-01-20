package com.carddemo.authorization.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "authorization_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", length = 10, unique = true)
    private String ruleCode;

    @Column(name = "rule_name", length = 50)
    private String ruleName;

    @Column(name = "rule_type", length = 20)
    private String ruleType;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "merchant_category_code", length = 4)
    private String merchantCategoryCode;

    @Column(name = "transaction_type", length = 10)
    private String transactionType;

    @Column(name = "min_amount", precision = 12, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 12, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "daily_limit", precision = 12, scale = 2)
    private BigDecimal dailyLimit;

    @Column(name = "monthly_limit", precision = 12, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(name = "velocity_count")
    private Integer velocityCount;

    @Column(name = "velocity_period_minutes")
    private Integer velocityPeriodMinutes;

    @Column(name = "country_restriction", length = 100)
    private String countryRestriction;

    @Column(name = "action", length = 20)
    private String action;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "active")
    private Boolean active;
}
