package com.aws.carddemo.account;

import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aws.carddemo.account.dto.AccountResponse;
import com.aws.carddemo.account.dto.AccountUpdateRequest;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;

@Service
@Transactional
public class AccountService {

    private static final Map<String, Set<String>> VALID_STATUS_TRANSITIONS = Map.of(
            "A", Set.of("S", "C"),
            "S", Set.of("A", "C"),
            "C", Set.of()
    );

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long accountId) {
        Account account = accountRepository.findByIdWithCustomer(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> listAccountsByCustomer(Long customerId, Pageable pageable) {
        return accountRepository.findByCustomerId(customerId, pageable)
                .map(AccountResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> listAll(Pageable pageable) {
        return accountRepository.findAll(pageable)
                .map(AccountResponse::from);
    }

    public AccountResponse updateAccount(Long accountId, AccountUpdateRequest request) {
        Account account = accountRepository.findByIdWithCustomer(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        if (request.accountStatus() != null) {
            validateStatusTransition(account.getAccountStatus(), request.accountStatus());
            account.setAccountStatus(request.accountStatus());
        }

        if (request.creditLimit() != null) {
            validateCreditLimit(request.creditLimit(), account.getCurrentBalance());
            account.setCreditLimit(request.creditLimit());
        }

        if (request.cashCreditLimit() != null) {
            java.math.BigDecimal effectiveCreditLimit = request.creditLimit() != null
                    ? request.creditLimit() : account.getCreditLimit();
            if (request.cashCreditLimit().compareTo(effectiveCreditLimit) > 0) {
                throw new ValidationException("Cash credit limit cannot exceed credit limit");
            }
            account.setCashCreditLimit(request.cashCreditLimit());
        }

        if (request.expirationDate() != null) {
            if (request.expirationDate().isBefore(java.time.LocalDate.now())) {
                throw new ValidationException("Expiration date cannot be in the past");
            }
            account.setExpirationDate(request.expirationDate());
        }

        if (request.reissueDate() != null) {
            account.setReissueDate(request.reissueDate());
        }

        if (request.groupId() != null) {
            account.setGroupId(request.groupId());
        }

        Account saved = accountRepository.save(account);
        return AccountResponse.from(saved);
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus.equals(newStatus)) {
            return;
        }
        Set<String> allowedTransitions = VALID_STATUS_TRANSITIONS.get(currentStatus);
        if (allowedTransitions == null || !allowedTransitions.contains(newStatus)) {
            throw new ValidationException(
                    "Invalid status transition from '" + currentStatus + "' to '" + newStatus + "'");
        }
    }

    private void validateCreditLimit(java.math.BigDecimal newLimit, java.math.BigDecimal currentBalance) {
        if (currentBalance != null && newLimit.compareTo(currentBalance) < 0) {
            throw new ValidationException("Credit limit cannot be less than current balance");
        }
    }
}
