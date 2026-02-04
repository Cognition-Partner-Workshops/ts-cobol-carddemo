package com.aws.carddemo.service.account;

import com.aws.carddemo.domain.entity.Account;
import com.aws.carddemo.domain.entity.CardCrossReference;
import com.aws.carddemo.domain.entity.Customer;
import com.aws.carddemo.domain.repository.AccountRepository;
import com.aws.carddemo.domain.repository.CardCrossReferenceRepository;
import com.aws.carddemo.domain.repository.CustomerRepository;
import com.aws.carddemo.service.dto.AccountDTO;
import com.aws.carddemo.service.dto.CustomerDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Account Service - migrated from COACTVWC.cbl and COACTUPC.cbl
 * Handles account view and update operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;

    /**
     * Get account by ID - migrated from 9000-READ-ACCT in COACTVWC.cbl
     */
    @Transactional(readOnly = true)
    public Optional<AccountDTO> getAccount(Long accountId) {
        log.info("Fetching account: {}", accountId);
        return accountRepository.findById(accountId)
                .map(this::mapToDTO);
    }

    /**
     * Get account with customer details - migrated from COACTVWC.cbl
     */
    @Transactional(readOnly = true)
    public Optional<AccountWithCustomerDTO> getAccountWithCustomer(Long accountId) {
        log.info("Fetching account with customer details: {}", accountId);
        
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            log.warn("Account not found: {}", accountId);
            return Optional.empty();
        }

        Account account = accountOpt.get();
        List<CardCrossReference> xrefs = cardCrossReferenceRepository.findByAccountAccountId(accountId);
        
        Customer customer = null;
        if (!xrefs.isEmpty()) {
            customer = xrefs.get(0).getCustomer();
        }

        return Optional.of(AccountWithCustomerDTO.builder()
                .account(mapToDTO(account))
                .customer(customer != null ? mapCustomerToDTO(customer) : null)
                .build());
    }

    /**
     * Update account - migrated from COACTUPC.cbl
     */
    @Transactional
    public AccountDTO updateAccount(Long accountId, AccountUpdateRequest request) {
        log.info("Updating account: {}", accountId);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        if (request.getActiveStatus() != null) {
            account.setActiveStatus(request.getActiveStatus());
        }
        if (request.getCreditLimit() != null) {
            account.setCreditLimit(request.getCreditLimit());
        }
        if (request.getCashCreditLimit() != null) {
            account.setCashCreditLimit(request.getCashCreditLimit());
        }
        if (request.getExpirationDate() != null) {
            account.setExpirationDate(request.getExpirationDate());
        }
        if (request.getReissueDate() != null) {
            account.setReissueDate(request.getReissueDate());
        }
        if (request.getGroupId() != null) {
            account.setGroupId(request.getGroupId());
        }

        Account savedAccount = accountRepository.save(account);
        log.info("Account updated successfully: {}", accountId);
        
        return mapToDTO(savedAccount);
    }

    /**
     * List all accounts with pagination
     */
    @Transactional(readOnly = true)
    public Page<AccountDTO> listAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable)
                .map(this::mapToDTO);
    }

    /**
     * List active accounts
     */
    @Transactional(readOnly = true)
    public Page<AccountDTO> listActiveAccounts(Pageable pageable) {
        return accountRepository.findByActiveStatus("Y", pageable)
                .map(this::mapToDTO);
    }

    /**
     * Find accounts by group
     */
    @Transactional(readOnly = true)
    public Page<AccountDTO> findByGroup(String groupId, Pageable pageable) {
        return accountRepository.findByGroupId(groupId, pageable)
                .map(this::mapToDTO);
    }

    /**
     * Find over-limit accounts
     */
    @Transactional(readOnly = true)
    public List<AccountDTO> findOverLimitAccounts() {
        return accountRepository.findOverLimitAccounts().stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Find expiring accounts
     */
    @Transactional(readOnly = true)
    public List<AccountDTO> findExpiringAccounts(LocalDate beforeDate) {
        return accountRepository.findExpiringAccounts(beforeDate).stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Get account statistics
     */
    @Transactional(readOnly = true)
    public AccountStatistics getStatistics() {
        return AccountStatistics.builder()
                .totalActiveAccounts(accountRepository.countActiveAccounts())
                .totalActiveBalance(accountRepository.getTotalActiveBalance())
                .totalCreditLimit(accountRepository.getTotalCreditLimit())
                .build();
    }

    private AccountDTO mapToDTO(Account account) {
        return AccountDTO.builder()
                .accountId(account.getAccountId())
                .activeStatus(account.getActiveStatus())
                .currentBalance(account.getCurrentBalance())
                .creditLimit(account.getCreditLimit())
                .cashCreditLimit(account.getCashCreditLimit())
                .openDate(account.getOpenDate())
                .expirationDate(account.getExpirationDate())
                .reissueDate(account.getReissueDate())
                .currentCycleCredit(account.getCurrentCycleCredit())
                .currentCycleDebit(account.getCurrentCycleDebit())
                .zipCode(account.getZipCode())
                .groupId(account.getGroupId())
                .availableCredit(account.getAvailableCredit())
                .overLimit(account.isOverLimit())
                .build();
    }

    private CustomerDTO mapCustomerToDTO(Customer customer) {
        return CustomerDTO.builder()
                .customerId(customer.getCustomerId())
                .firstName(customer.getFirstName())
                .middleName(customer.getMiddleName())
                .lastName(customer.getLastName())
                .fullName(customer.getFullName())
                .addressLine1(customer.getAddressLine1())
                .addressLine2(customer.getAddressLine2())
                .addressLine3(customer.getAddressLine3())
                .stateCode(customer.getStateCode())
                .countryCode(customer.getCountryCode())
                .zipCode(customer.getZipCode())
                .phoneNumber1(customer.getPhoneNumber1())
                .phoneNumber2(customer.getPhoneNumber2())
                .govtIssuedId(customer.getGovtIssuedId())
                .dateOfBirth(customer.getDateOfBirth())
                .eftAccountId(customer.getEftAccountId())
                .primaryCardHolder(customer.getPrimaryCardHolder())
                .ficoCreditScore(customer.getFicoCreditScore())
                .build();
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AccountWithCustomerDTO {
        private AccountDTO account;
        private CustomerDTO customer;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AccountUpdateRequest {
        private String activeStatus;
        private BigDecimal creditLimit;
        private BigDecimal cashCreditLimit;
        private LocalDate expirationDate;
        private LocalDate reissueDate;
        private String groupId;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AccountStatistics {
        private long totalActiveAccounts;
        private BigDecimal totalActiveBalance;
        private BigDecimal totalCreditLimit;
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String message) {
            super(message);
        }
    }
}
