package com.aws.carddemo.service;

import com.aws.carddemo.dto.AccountDto;
import com.aws.carddemo.entity.Account;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.mapper.AccountMapper;
import com.aws.carddemo.repository.AccountRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountService(AccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "accounts", key = "#acctId")
    public AccountDto getAccount(Long acctId) {
        Account account = accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "acctId", acctId));
        return accountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountWithCards(Long acctId) {
        Account account = accountRepository.findByIdWithCards(acctId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "acctId", acctId));
        return accountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public Page<AccountDto> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable).map(accountMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AccountDto> getActiveAccounts(Pageable pageable) {
        return accountRepository.findByAcctActiveStatus("Y", pageable).map(accountMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsByGroupId(String groupId) {
        return accountMapper.toDtoList(accountRepository.findByAcctGroupId(groupId));
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getExpiredAccounts() {
        return accountMapper.toDtoList(accountRepository.findExpiredAccounts(LocalDate.now()));
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsExpiringBetween(LocalDate startDate, LocalDate endDate) {
        return accountMapper.toDtoList(accountRepository.findAccountsExpiringBetween(startDate, endDate));
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getOverlimitAccounts() {
        return accountMapper.toDtoList(accountRepository.findOverlimitAccounts());
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#dto.acctId")
    public AccountDto createAccount(AccountDto dto) {
        Account account = accountMapper.toEntity(dto);
        account = accountRepository.save(account);
        return accountMapper.toDto(account);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#acctId")
    public AccountDto updateAccount(Long acctId, AccountDto dto) {
        Account account = accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "acctId", acctId));
        accountMapper.updateEntity(dto, account);
        account = accountRepository.save(account);
        return accountMapper.toDto(account);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#acctId")
    public void updateAccountBalance(Long acctId, BigDecimal amount) {
        Account account = accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "acctId", acctId));
        
        account.setAcctCurrBal(account.getAcctCurrBal().add(amount));
        
        if (amount.compareTo(BigDecimal.ZERO) >= 0) {
            account.setAcctCurrCycCredit(account.getAcctCurrCycCredit().add(amount));
        } else {
            account.setAcctCurrCycDebit(account.getAcctCurrCycDebit().add(amount));
        }
        
        accountRepository.save(account);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#acctId")
    public void deactivateAccount(Long acctId) {
        Account account = accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "acctId", acctId));
        account.setAcctActiveStatus("N");
        accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public long countActiveAccounts() {
        return accountRepository.countByStatus("Y");
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long acctId) {
        return accountRepository.existsById(acctId);
    }
}
