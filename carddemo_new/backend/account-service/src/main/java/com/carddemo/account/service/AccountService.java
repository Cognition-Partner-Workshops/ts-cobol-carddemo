package com.carddemo.account.service;

import com.carddemo.account.dto.AccountDto;
import com.carddemo.account.dto.AccountUpdateRequest;
import com.carddemo.account.dto.CustomerDto;
import com.carddemo.account.entity.Account;
import com.carddemo.account.entity.Customer;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.account.repository.CustomerRepository;
import com.carddemo.common.dto.PageResponse;
import com.carddemo.common.exception.BusinessException;
import com.carddemo.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountDto getAccountById(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", accountId));

        CustomerDto customerDto = null;
        if (account.getCustomerId() != null) {
            customerDto = customerRepository.findById(account.getCustomerId())
                    .map(this::mapToCustomerDto)
                    .orElse(null);
        }

        return mapToAccountDto(account, customerDto);
    }

    public PageResponse<AccountDto> getAllAccounts(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<Account> accountPage = accountRepository.findAll(pageable);

        List<AccountDto> accounts = accountPage.getContent().stream()
                .map(account -> mapToAccountDto(account, null))
                .collect(Collectors.toList());

        return PageResponse.<AccountDto>builder()
                .content(accounts)
                .pageNumber(accountPage.getNumber())
                .pageSize(accountPage.getSize())
                .totalElements(accountPage.getTotalElements())
                .totalPages(accountPage.getTotalPages())
                .first(accountPage.isFirst())
                .last(accountPage.isLast())
                .build();
    }

    public List<AccountDto> getAccountsByCustomerId(String customerId) {
        List<Account> accounts = accountRepository.findByCustomerId(customerId);
        return accounts.stream()
                .map(account -> mapToAccountDto(account, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountDto updateAccount(String accountId, AccountUpdateRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", accountId));

        if (request.getActiveStatus() != null) {
            if (!request.getActiveStatus().equals("Y") && !request.getActiveStatus().equals("N")) {
                throw new BusinessException("Active status must be Y or N", "INVALID_STATUS");
            }
            account.setActiveStatus(request.getActiveStatus());
        }

        if (request.getCreditLimit() != null) {
            if (request.getCreditLimit().compareTo(account.getCurrentBalance()) < 0) {
                throw new BusinessException("Credit limit cannot be less than current balance", "INVALID_CREDIT_LIMIT");
            }
            account.setCreditLimit(request.getCreditLimit());
        }

        if (request.getCashCreditLimit() != null) {
            account.setCashCreditLimit(request.getCashCreditLimit());
        }

        if (request.getExpirationDate() != null) {
            if (request.getExpirationDate().isBefore(account.getOpenDate())) {
                throw new BusinessException("Expiration date must be after open date", "INVALID_EXPIRATION_DATE");
            }
            account.setExpirationDate(request.getExpirationDate());
        }

        if (request.getGroupId() != null) {
            account.setGroupId(request.getGroupId());
        }

        Account savedAccount = accountRepository.save(account);
        return mapToAccountDto(savedAccount, null);
    }

    private AccountDto mapToAccountDto(Account account, CustomerDto customerDto) {
        return AccountDto.builder()
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
                .groupId(account.getGroupId())
                .customerId(account.getCustomerId())
                .availableCredit(account.getAvailableCredit())
                .availableCash(account.getAvailableCash())
                .customer(customerDto)
                .build();
    }

    private CustomerDto mapToCustomerDto(Customer customer) {
        String fullName = String.format("%s %s %s",
                customer.getFirstName() != null ? customer.getFirstName() : "",
                customer.getMiddleName() != null ? customer.getMiddleName() : "",
                customer.getLastName() != null ? customer.getLastName() : "").trim();

        return CustomerDto.builder()
                .customerId(customer.getCustomerId())
                .firstName(customer.getFirstName())
                .middleName(customer.getMiddleName())
                .lastName(customer.getLastName())
                .fullName(fullName)
                .addressLine1(customer.getAddressLine1())
                .addressLine2(customer.getAddressLine2())
                .addressLine3(customer.getAddressLine3())
                .stateCode(customer.getStateCode())
                .countryCode(customer.getCountryCode())
                .zipCode(customer.getZipCode())
                .phoneNumber1(customer.getPhoneNumber1())
                .phoneNumber2(customer.getPhoneNumber2())
                .dateOfBirth(customer.getDateOfBirth())
                .ficoScore(customer.getFicoScore())
                .build();
    }
}
