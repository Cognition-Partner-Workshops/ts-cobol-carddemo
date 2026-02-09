package com.carddemo.service;

import com.carddemo.dto.AccountViewDto;
import com.carddemo.dto.CardDto;
import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.CardAccountXref;
import com.carddemo.entity.Customer;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardAccountXrefRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CardAccountXrefRepository xrefRepository;
    private final CustomerRepository customerRepository;

    public AccountService(AccountRepository accountRepository,
                          CardRepository cardRepository,
                          CardAccountXrefRepository xrefRepository,
                          CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.xrefRepository = xrefRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public AccountViewDto getAccountView(Long acctId) {
        Account account = accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Did not find this account in account master file"));

        AccountViewDto dto = new AccountViewDto();
        dto.setAcctId(account.getAcctId());
        dto.setActiveStatus(account.getActiveStatus());
        dto.setCurrBal(account.getCurrBal());
        dto.setCreditLimit(account.getCreditLimit());
        dto.setCashCreditLimit(account.getCashCreditLimit());
        dto.setOpenDate(account.getOpenDate());
        dto.setExpirationDate(account.getExpirationDate());
        dto.setGroupId(account.getGroupId());
        dto.setCurrCycCredit(account.getCurrCycCredit());
        dto.setCurrCycDebit(account.getCurrCycDebit());

        List<CardAccountXref> xrefs = xrefRepository.findByAcctId(acctId);
        if (!xrefs.isEmpty()) {
            CardAccountXref xref = xrefs.get(0);
            dto.setCustId(xref.getCustId());
            customerRepository.findById(xref.getCustId()).ifPresent(cust -> {
                dto.setCustFirstName(cust.getFirstName());
                dto.setCustLastName(cust.getLastName());
            });
        }

        List<Card> cards = cardRepository.findByAcctId(acctId);
        List<CardDto> cardDtos = new ArrayList<>();
        for (Card card : cards) {
            CardDto cardDto = new CardDto();
            cardDto.setCardNum(card.getCardNum());
            cardDto.setAcctId(card.getAcctId());
            cardDto.setCvvCd(card.getCvvCd());
            cardDto.setEmbossedName(card.getEmbossedName());
            cardDto.setExpirationDate(card.getExpirationDate());
            cardDto.setActiveStatus(card.getActiveStatus());
            cardDtos.add(cardDto);
        }
        dto.setCards(cardDtos);

        return dto;
    }

    public Account updateAccount(Long acctId, Account updatedAccount) {
        Account existing = accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + acctId));

        if (updatedAccount.getActiveStatus() != null) {
            existing.setActiveStatus(updatedAccount.getActiveStatus());
        }
        if (updatedAccount.getCreditLimit() != null) {
            existing.setCreditLimit(updatedAccount.getCreditLimit());
        }
        if (updatedAccount.getCashCreditLimit() != null) {
            existing.setCashCreditLimit(updatedAccount.getCashCreditLimit());
        }
        if (updatedAccount.getExpirationDate() != null) {
            existing.setExpirationDate(updatedAccount.getExpirationDate());
        }
        if (updatedAccount.getReissueDate() != null) {
            existing.setReissueDate(updatedAccount.getReissueDate());
        }
        if (updatedAccount.getGroupId() != null) {
            existing.setGroupId(updatedAccount.getGroupId());
        }

        return accountRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Page<Account> listAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long acctId) {
        return accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + acctId));
    }
}
