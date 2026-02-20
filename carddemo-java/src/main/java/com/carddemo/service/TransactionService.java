package com.carddemo.service;

import com.carddemo.dto.TransactionReportEntry;
import com.carddemo.dto.TransactionRequest;
import com.carddemo.entity.CardCrossReference;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.TransactionCategory;
import com.carddemo.entity.TransactionCategoryId;
import com.carddemo.entity.TransactionType;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.CardCrossReferenceRepository;
import com.carddemo.repository.TransactionCategoryRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.TransactionTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CardCrossReferenceRepository cardCrossReferenceRepository,
                              TransactionTypeRepository transactionTypeRepository,
                              TransactionCategoryRepository transactionCategoryRepository) {
        this.transactionRepository = transactionRepository;
        this.cardCrossReferenceRepository = cardCrossReferenceRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionCategoryRepository = transactionCategoryRepository;
    }

    public Page<Transaction> listTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    public Page<Transaction> listTransactionsByCard(String cardNum, Pageable pageable) {
        return transactionRepository.findByCardNum(cardNum, pageable);
    }

    public Page<Transaction> listTransactionsByAccount(Long acctId, Pageable pageable) {
        return transactionRepository.findByAccountId(acctId, pageable);
    }

    public Transaction getTransaction(String tranId) {
        return transactionRepository.findById(tranId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + tranId));
    }

    @Transactional
    public Transaction addTransaction(TransactionRequest request) {
        CardCrossReference xref = cardCrossReferenceRepository.findById(request.getCardNum())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Card not found in cross reference: " + request.getCardNum()));

        String tranId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Transaction transaction = new Transaction();
        transaction.setTranId(tranId);
        transaction.setTypeCd(request.getTypeCd());
        transaction.setCatCd(request.getCatCd());
        transaction.setSource(request.getSource() != null ? request.getSource() : "ONLINE");
        transaction.setDescription(request.getDescription() != null ? request.getDescription() : "");
        transaction.setAmount(request.getAmount());
        transaction.setMerchantId(request.getMerchantId());
        transaction.setMerchantName(request.getMerchantName());
        transaction.setMerchantCity(request.getMerchantCity());
        transaction.setMerchantZip(request.getMerchantZip());
        transaction.setCardNum(request.getCardNum());
        transaction.setOrigTs(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    public List<TransactionReportEntry> generateTransactionReport(String startDate, String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime start = LocalDate.parse(startDate, formatter).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate, formatter).atTime(23, 59, 59);

        List<Transaction> transactions = transactionRepository.findByDateRange(start, end);
        List<TransactionReportEntry> reportEntries = new ArrayList<>();

        for (Transaction txn : transactions) {
            TransactionReportEntry entry = new TransactionReportEntry();
            entry.setTranId(txn.getTranId());
            entry.setTypeCd(txn.getTypeCd());
            entry.setCatCd(txn.getCatCd());
            entry.setSource(txn.getSource());
            entry.setAmount(txn.getAmount());
            entry.setOrigTs(txn.getOrigTs());

            CardCrossReference xref = cardCrossReferenceRepository.findById(txn.getCardNum()).orElse(null);
            if (xref != null) {
                entry.setAccountId(xref.getAcctId());
            }

            transactionTypeRepository.findById(txn.getTypeCd())
                    .ifPresent(tt -> entry.setTypeDesc(tt.getTypeDesc()));

            transactionCategoryRepository.findById(
                    new TransactionCategoryId(txn.getTypeCd(), txn.getCatCd()))
                    .ifPresent(tc -> entry.setCatDesc(tc.getCatTypeDesc()));

            reportEntries.add(entry);
        }

        return reportEntries;
    }
}
