package com.aws.carddemo.service;

import com.aws.carddemo.dto.TransactionDto;
import com.aws.carddemo.entity.Account;
import com.aws.carddemo.entity.Card;
import com.aws.carddemo.entity.CardXref;
import com.aws.carddemo.entity.Transaction;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.TransactionValidationException;
import com.aws.carddemo.mapper.TransactionMapper;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.CardRepository;
import com.aws.carddemo.repository.CardXrefRepository;
import com.aws.carddemo.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    public TransactionService(TransactionRepository transactionRepository,
                               CardRepository cardRepository,
                               CardXrefRepository cardXrefRepository,
                               AccountRepository accountRepository,
                               TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
        this.transactionMapper = transactionMapper;
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransaction(String tranId) {
        Transaction transaction = transactionRepository.findById(tranId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "tranId", tranId));
        return transactionMapper.toDto(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(transactionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionsByCard(String cardNum, Pageable pageable) {
        return transactionRepository.findByCardCardNum(cardNum, pageable).map(transactionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionMapper.toDtoList(transactionRepository.findByDateRange(startDate, endDate));
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByCardAndDateRange(String cardNum, 
                                                                   LocalDateTime startDate, 
                                                                   LocalDateTime endDate) {
        return transactionMapper.toDtoList(
                transactionRepository.findByCardAndDateRange(cardNum, startDate, endDate));
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getLargeTransactions(BigDecimal amount) {
        return transactionMapper.toDtoList(transactionRepository.findLargeTransactions(amount));
    }

    @Transactional(readOnly = true)
    public BigDecimal sumTransactionsByCard(String cardNum) {
        BigDecimal sum = transactionRepository.sumTransactionsByCard(cardNum);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal sumTransactionsByAccount(Long acctId) {
        BigDecimal sum = transactionRepository.sumTransactionsByAccount(acctId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Transactional
    public TransactionDto createTransaction(TransactionDto dto) {
        Card card = cardRepository.findById(dto.getTranCardNum())
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNum", dto.getTranCardNum()));
        
        Transaction transaction = transactionMapper.toEntity(dto);
        transaction.setCard(card);
        transaction.setTranProcTs(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public TransactionDto postTransaction(TransactionDto dto) {
        validateTransaction(dto);
        
        CardXref xref = cardXrefRepository.findByCardNumWithDetails(dto.getTranCardNum())
                .orElseThrow(TransactionValidationException::invalidCardNumber);
        
        Account account = xref.getAccount();
        Card card = xref.getCard();
        
        Transaction transaction = transactionMapper.toEntity(dto);
        transaction.setCard(card);
        transaction.setTranProcTs(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);
        
        updateAccountBalance(account, dto.getTranAmt());
        
        return transactionMapper.toDto(transaction);
    }

    public void validateTransaction(TransactionDto dto) {
        CardXref xref = cardXrefRepository.findByCardNumWithDetails(dto.getTranCardNum())
                .orElseThrow(TransactionValidationException::invalidCardNumber);
        
        Account account = xref.getAccount();
        if (account == null) {
            throw TransactionValidationException.accountNotFound();
        }
        
        BigDecimal projectedBalance = account.getProjectedBalance(dto.getTranAmt());
        if (account.getAcctCreditLimit().compareTo(projectedBalance) < 0) {
            throw TransactionValidationException.overlimitTransaction();
        }
        
        LocalDate tranDate = dto.getTranOrigTs().toLocalDate();
        if (account.getAcctExpirationDate().isBefore(tranDate)) {
            throw TransactionValidationException.accountExpired();
        }
    }

    private void updateAccountBalance(Account account, BigDecimal amount) {
        account.setAcctCurrBal(account.getAcctCurrBal().add(amount));
        
        if (amount.compareTo(BigDecimal.ZERO) >= 0) {
            account.setAcctCurrCycCredit(account.getAcctCurrCycCredit().add(amount));
        } else {
            account.setAcctCurrCycDebit(account.getAcctCurrCycDebit().add(amount));
        }
        
        accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public long countTransactionsByCard(String cardNum) {
        return transactionRepository.countByCard(cardNum);
    }
}
