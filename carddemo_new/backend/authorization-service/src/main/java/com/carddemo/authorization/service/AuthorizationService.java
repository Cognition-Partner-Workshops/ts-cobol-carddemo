package com.carddemo.authorization.service;

import com.carddemo.authorization.dto.AuthorizationRequestDto;
import com.carddemo.authorization.dto.AuthorizationResponseDto;
import com.carddemo.authorization.entity.*;
import com.carddemo.authorization.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {
    
    private final AuthorizationRequestRepository authRequestRepository;
    private final AuthorizationRuleRepository ruleRepository;
    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    
    // Response codes
    private static final String APPROVED = "00";
    private static final String DECLINED_INSUFFICIENT_FUNDS = "51";
    private static final String DECLINED_CARD_EXPIRED = "54";
    private static final String DECLINED_CARD_INACTIVE = "62";
    private static final String DECLINED_ACCOUNT_INACTIVE = "78";
    private static final String DECLINED_VELOCITY_EXCEEDED = "61";
    private static final String DECLINED_AMOUNT_LIMIT = "65";
    private static final String DECLINED_COUNTRY_RESTRICTION = "57";
    private static final String DECLINED_INVALID_CARD = "14";
    
    @Transactional
    public AuthorizationResponseDto processAuthorization(AuthorizationRequestDto request) {
        log.info("Processing authorization request for card ending in {}", 
            maskCardNumber(request.getCardNumber()));
        
        LocalDateTime requestTime = LocalDateTime.now();
        String authId = generateAuthId();
        
        // Create authorization request record
        AuthorizationRequest authRequest = new AuthorizationRequest();
        authRequest.setAuthId(authId);
        authRequest.setCardNumber(request.getCardNumber());
        authRequest.setMerchantId(request.getMerchantId());
        authRequest.setMerchantName(request.getMerchantName());
        authRequest.setMerchantCategoryCode(request.getMerchantCategoryCode());
        authRequest.setMerchantCity(request.getMerchantCity());
        authRequest.setMerchantState(request.getMerchantState());
        authRequest.setMerchantCountry(request.getMerchantCountry());
        authRequest.setAmount(request.getAmount());
        authRequest.setCurrencyCode(request.getCurrencyCode());
        authRequest.setTransactionType(request.getTransactionType());
        authRequest.setPosEntryMode(request.getPosEntryMode());
        authRequest.setRequestTimestamp(requestTime);
        
        // Validate card
        Optional<Card> cardOpt = cardRepository.findById(request.getCardNumber());
        if (cardOpt.isEmpty()) {
            return declineAuthorization(authRequest, DECLINED_INVALID_CARD, "Card not found");
        }
        
        Card card = cardOpt.get();
        authRequest.setAccountId(card.getAccountId());
        
        // Check card status
        if (!"Y".equals(card.getCardStatus())) {
            return declineAuthorization(authRequest, DECLINED_CARD_INACTIVE, "Card is not active");
        }
        
        // Check card expiration
        if (card.getExpirationDate() != null && card.getExpirationDate().isBefore(LocalDate.now())) {
            return declineAuthorization(authRequest, DECLINED_CARD_EXPIRED, "Card has expired");
        }
        
        // Validate account
        Optional<Account> accountOpt = accountRepository.findById(card.getAccountId());
        if (accountOpt.isEmpty()) {
            return declineAuthorization(authRequest, DECLINED_ACCOUNT_INACTIVE, "Account not found");
        }
        
        Account account = accountOpt.get();
        
        // Check account status
        if (!"Y".equals(account.getActiveStatus())) {
            return declineAuthorization(authRequest, DECLINED_ACCOUNT_INACTIVE, "Account is not active");
        }
        
        // Calculate available credit
        BigDecimal availableCredit = account.getCreditLimit().subtract(account.getCurrentBalance());
        authRequest.setAvailableCreditBefore(availableCredit);
        
        // Check sufficient credit
        if (request.getAmount().compareTo(availableCredit) > 0) {
            return declineAuthorization(authRequest, DECLINED_INSUFFICIENT_FUNDS, 
                "Insufficient available credit");
        }
        
        // Apply authorization rules
        List<AuthorizationRule> rules = ruleRepository.findApplicableRules(request.getMerchantCategoryCode());
        for (AuthorizationRule rule : rules) {
            String ruleResult = evaluateRule(rule, request, card);
            if (ruleResult != null) {
                return declineAuthorization(authRequest, ruleResult, 
                    "Declined by rule: " + rule.getRuleName());
            }
        }
        
        // Approve authorization
        return approveAuthorization(authRequest, account, request.getAmount());
    }
    
    private String evaluateRule(AuthorizationRule rule, AuthorizationRequestDto request, Card card) {
        // Check amount limits
        if (rule.getMinAmount() != null && request.getAmount().compareTo(rule.getMinAmount()) < 0) {
            return null; // Rule doesn't apply
        }
        if (rule.getMaxAmount() != null && request.getAmount().compareTo(rule.getMaxAmount()) > 0) {
            if ("DECLINE".equals(rule.getAction())) {
                return DECLINED_AMOUNT_LIMIT;
            }
        }
        
        // Check country restriction
        if (rule.getCountryRestriction() != null && !rule.getCountryRestriction().isEmpty()) {
            String[] restrictedCountries = rule.getCountryRestriction().split(",");
            for (String country : restrictedCountries) {
                if (country.trim().equals(request.getMerchantCountry())) {
                    if ("DECLINE".equals(rule.getAction())) {
                        return DECLINED_COUNTRY_RESTRICTION;
                    }
                }
            }
        }
        
        // Check velocity (transaction count in time period)
        if (rule.getVelocityCount() != null && rule.getVelocityPeriodMinutes() != null) {
            LocalDateTime since = LocalDateTime.now().minusMinutes(rule.getVelocityPeriodMinutes());
            Long count = authRequestRepository.countApprovedSince(card.getCardNumber(), since);
            if (count >= rule.getVelocityCount()) {
                if ("DECLINE".equals(rule.getAction())) {
                    return DECLINED_VELOCITY_EXCEEDED;
                }
            }
        }
        
        // Check daily limit
        if (rule.getDailyLimit() != null) {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            BigDecimal dailyTotal = authRequestRepository.sumApprovedAmountSince(card.getCardNumber(), startOfDay);
            if (dailyTotal.add(request.getAmount()).compareTo(rule.getDailyLimit()) > 0) {
                if ("DECLINE".equals(rule.getAction())) {
                    return DECLINED_AMOUNT_LIMIT;
                }
            }
        }
        
        return null; // Rule passed
    }
    
    private AuthorizationResponseDto approveAuthorization(AuthorizationRequest authRequest, 
            Account account, BigDecimal amount) {
        LocalDateTime responseTime = LocalDateTime.now();
        
        // Update account balance (hold the amount)
        BigDecimal newBalance = account.getCurrentBalance().add(amount);
        account.setCurrentBalance(newBalance);
        accountRepository.save(account);
        
        BigDecimal availableCreditAfter = account.getCreditLimit().subtract(newBalance);
        
        // Update authorization request
        authRequest.setResponseTimestamp(responseTime);
        authRequest.setStatus("APPROVED");
        authRequest.setResponseCode(APPROVED);
        authRequest.setAvailableCreditAfter(availableCreditAfter);
        authRequestRepository.save(authRequest);
        
        log.info("Authorization {} approved for amount {}", authRequest.getAuthId(), amount);
        
        return AuthorizationResponseDto.builder()
            .authId(authRequest.getAuthId())
            .status("APPROVED")
            .responseCode(APPROVED)
            .authorizedAmount(amount)
            .availableCredit(availableCreditAfter)
            .responseTimestamp(responseTime)
            .build();
    }
    
    private AuthorizationResponseDto declineAuthorization(AuthorizationRequest authRequest, 
            String responseCode, String reason) {
        LocalDateTime responseTime = LocalDateTime.now();
        
        authRequest.setResponseTimestamp(responseTime);
        authRequest.setStatus("DECLINED");
        authRequest.setResponseCode(responseCode);
        authRequest.setDeclineReason(reason);
        authRequestRepository.save(authRequest);
        
        log.info("Authorization {} declined: {} - {}", authRequest.getAuthId(), responseCode, reason);
        
        return AuthorizationResponseDto.builder()
            .authId(authRequest.getAuthId())
            .status("DECLINED")
            .responseCode(responseCode)
            .declineReason(reason)
            .responseTimestamp(responseTime)
            .build();
    }
    
    public List<AuthorizationRequest> getAuthorizationHistory(String cardNumber) {
        return authRequestRepository.findByCardNumber(cardNumber);
    }
    
    public List<AuthorizationRequest> getAuthorizationsByAccount(String accountId) {
        return authRequestRepository.findByAccountId(accountId);
    }
    
    public Optional<AuthorizationRequest> getAuthorization(String authId) {
        return authRequestRepository.findByAuthId(authId);
    }
    
    private String generateAuthId() {
        return "AUTH" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
    
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }
}
