package com.carddemo.api.service;

import com.carddemo.api.dto.AccountResponse;
import com.carddemo.api.dto.AccountUpdateRequest;
import com.carddemo.api.dto.PageResponse;
import com.carddemo.core.domain.Account;
import com.carddemo.core.exception.ResourceNotFoundException;
import com.carddemo.core.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for Account operations.
 * Replaces business logic from COACTVWC (Account View) and COACTUPC (Account Update).
 *
 * Key COBOL logic replaced:
 * - VSAM READ on ACCTDATA file → JPA findById
 * - VSAM REWRITE on ACCTDATA file → JPA save
 * - VSAM STARTBR/READNEXT browse → JPA paginated queries
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountResponse getAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", String.valueOf(accountId)));
        return mapToResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(Long accountId, AccountUpdateRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", String.valueOf(accountId)));

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
        if (request.getAddressZip() != null) {
            account.setAddressZip(request.getAddressZip());
        }
        if (request.getGroupId() != null) {
            account.setGroupId(request.getGroupId());
        }

        Account saved = accountRepository.save(account);
        return mapToResponse(saved);
    }

    public PageResponse<AccountResponse> listAccounts(Pageable pageable) {
        Page<Account> page = accountRepository.findAll(pageable);
        return buildPageResponse(page);
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .accountId(account.getAcctId())
                .activeStatus(account.getActiveStatus())
                .currentBalance(account.getCurrentBalance())
                .creditLimit(account.getCreditLimit())
                .cashCreditLimit(account.getCashCreditLimit())
                .openDate(account.getOpenDate())
                .expirationDate(account.getExpirationDate())
                .reissueDate(account.getReissueDate())
                .currentCycleCredit(account.getCurrentCycleCredit())
                .currentCycleDebit(account.getCurrentCycleDebit())
                .addressZip(account.getAddressZip())
                .groupId(account.getGroupId())
                .build();
    }

    private PageResponse<AccountResponse> buildPageResponse(Page<Account> page) {
        return PageResponse.<AccountResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
