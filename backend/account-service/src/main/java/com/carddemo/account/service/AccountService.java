package com.carddemo.account.service;

import com.carddemo.account.dto.AccountSummaryDto;
import com.carddemo.account.dto.CreateAccountRequest;
import com.carddemo.account.dto.UpdateAccountRequest;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.account.repository.CardAccountXrefRepository;
import com.carddemo.common.dto.AccountDto;
import com.carddemo.common.dto.PagedResponse;
import com.carddemo.common.entity.Account;
import com.carddemo.common.entity.CardAccountXref;
import com.carddemo.common.exception.AccountInactiveException;
import com.carddemo.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CardAccountXrefRepository cardAccountXrefRepository;

    public AccountService(AccountRepository accountRepository, CardAccountXrefRepository cardAccountXrefRepository) {
        this.accountRepository = accountRepository;
        this.cardAccountXrefRepository = cardAccountXrefRepository;
    }

    public PagedResponse<AccountDto> getAllAccounts(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Account> accountPage = accountRepository.findAll(pageable);

        List<AccountDto> accounts = accountPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<AccountDto>builder()
                .content(accounts)
                .page(accountPage.getNumber())
                .size(accountPage.getSize())
                .totalElements(accountPage.getTotalElements())
                .totalPages(accountPage.getTotalPages())
                .first(accountPage.isFirst())
                .last(accountPage.isLast())
                .build();
    }

    public AccountDto getAccountById(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", accountId));
        return mapToDto(account);
    }

    public List<AccountDto> getAccountsByCustomerId(Long customerId) {
        List<CardAccountXref> xrefs = cardAccountXrefRepository.findByCustomerId(customerId);
        return xrefs.stream()
                .map(xref -> accountRepository.findById(xref.getAccountId())
                        .map(this::mapToDto)
                        .orElse(null))
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountDto createAccount(CreateAccountRequest request) {
        Account account = Account.builder()
                .accountId(generateAccountId())
                .activeStatus("Y")
                .currentBalance(BigDecimal.ZERO)
                .creditLimit(request.getCreditLimit())
                .cashCreditLimit(request.getCashCreditLimit())
                .openDate(LocalDate.now())
                .expirationDate(request.getExpirationDate())
                .currentCycleCredit(BigDecimal.ZERO)
                .currentCycleDebit(BigDecimal.ZERO)
                .addressZip(request.getAddressZip())
                .groupId(request.getGroupId())
                .build();

        account = accountRepository.save(account);

        CardAccountXref xref = CardAccountXref.builder()
                .customerId(request.getCustomerId())
                .accountId(account.getAccountId())
                .cardNumber("")
                .build();
        cardAccountXrefRepository.save(xref);

        return mapToDto(account);
    }

    @Transactional
    public AccountDto updateAccount(Long accountId, UpdateAccountRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", accountId));

        if (request.getActiveStatus() != null) account.setActiveStatus(request.getActiveStatus());
        if (request.getCreditLimit() != null) account.setCreditLimit(request.getCreditLimit());
        if (request.getCashCreditLimit() != null) account.setCashCreditLimit(request.getCashCreditLimit());
        if (request.getExpirationDate() != null) account.setExpirationDate(request.getExpirationDate());
        if (request.getReissueDate() != null) account.setReissueDate(request.getReissueDate());
        if (request.getAddressZip() != null) account.setAddressZip(request.getAddressZip());
        if (request.getGroupId() != null) account.setGroupId(request.getGroupId());

        account = accountRepository.save(account);
        return mapToDto(account);
    }

    @Transactional
    public AccountDto activateAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", accountId));
        account.setActiveStatus("Y");
        account = accountRepository.save(account);
        return mapToDto(account);
    }

    @Transactional
    public AccountDto deactivateAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", accountId));
        account.setActiveStatus("N");
        account = accountRepository.save(account);
        return mapToDto(account);
    }

    public List<AccountDto> getActiveAccounts() {
        return accountRepository.findByActiveStatus("Y").stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<AccountDto> getOverLimitAccounts() {
        return accountRepository.findOverLimitAccounts().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<AccountDto> getExpiringAccounts(int daysAhead) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(daysAhead);
        return accountRepository.findAccountsExpiringBetween(startDate, endDate).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public AccountSummaryDto getAccountSummary() {
        List<Account> allAccounts = accountRepository.findAll();
        List<Account> activeAccounts = accountRepository.findByActiveStatus("Y");
        List<Account> overLimitAccounts = accountRepository.findOverLimitAccounts();

        BigDecimal totalBalance = allAccounts.stream()
                .map(Account::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCreditLimit = allAccounts.stream()
                .map(Account::getCreditLimit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageBalance = allAccounts.isEmpty() ? BigDecimal.ZERO
                : totalBalance.divide(BigDecimal.valueOf(allAccounts.size()), 2, RoundingMode.HALF_UP);

        return AccountSummaryDto.builder()
                .totalAccounts((long) allAccounts.size())
                .activeAccounts((long) activeAccounts.size())
                .inactiveAccounts((long) (allAccounts.size() - activeAccounts.size()))
                .totalBalance(totalBalance)
                .totalCreditLimit(totalCreditLimit)
                .averageBalance(averageBalance)
                .overLimitAccounts((long) overLimitAccounts.size())
                .build();
    }

    public void validateAccountActive(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", accountId));
        if (!account.isActive()) {
            throw new AccountInactiveException(accountId);
        }
    }

    private Long generateAccountId() {
        return ThreadLocalRandom.current().nextLong(10000000000L, 99999999999L);
    }

    private AccountDto mapToDto(Account account) {
        return AccountDto.builder()
                .accountId(account.getAccountId())
                .activeStatus(account.getActiveStatus())
                .currentBalance(account.getCurrentBalance())
                .creditLimit(account.getCreditLimit())
                .cashCreditLimit(account.getCashCreditLimit())
                .availableCredit(account.getAvailableCredit())
                .openDate(account.getOpenDate())
                .expirationDate(account.getExpirationDate())
                .reissueDate(account.getReissueDate())
                .currentCycleCredit(account.getCurrentCycleCredit())
                .currentCycleDebit(account.getCurrentCycleDebit())
                .addressZip(account.getAddressZip())
                .groupId(account.getGroupId())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
