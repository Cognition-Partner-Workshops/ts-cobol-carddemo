package com.carddemo.batch;

import com.carddemo.entity.CardCrossReference;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.TransactionCategory;
import com.carddemo.entity.TransactionCategoryId;
import com.carddemo.entity.TransactionType;
import com.carddemo.repository.CardCrossReferenceRepository;
import com.carddemo.repository.TransactionCategoryRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.TransactionTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TransactionReportJob {

    private static final Logger log = LoggerFactory.getLogger(TransactionReportJob.class);

    private final TransactionRepository transactionRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;

    public TransactionReportJob(TransactionRepository transactionRepository,
                                CardCrossReferenceRepository cardCrossReferenceRepository,
                                TransactionTypeRepository transactionTypeRepository,
                                TransactionCategoryRepository transactionCategoryRepository) {
        this.transactionRepository = transactionRepository;
        this.cardCrossReferenceRepository = cardCrossReferenceRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionCategoryRepository = transactionCategoryRepository;
    }

    public Map<String, Object> execute(LocalDate startDate, LocalDate endDate) {
        log.info("START OF TRANSACTION REPORT JOB");

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Transaction> transactions = transactionRepository.findByDateRange(start, end);

        BigDecimal grandTotal = BigDecimal.ZERO;
        Map<Long, BigDecimal> accountTotals = new LinkedHashMap<>();
        List<Map<String, Object>> reportLines = new ArrayList<>();

        for (Transaction txn : transactions) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("tranId", txn.getTranId());

            Optional<CardCrossReference> xref = cardCrossReferenceRepository.findById(txn.getCardNum());
            Long acctId = xref.map(CardCrossReference::getAcctId).orElse(0L);
            line.put("accountId", acctId);

            line.put("typeCd", txn.getTypeCd());
            Optional<TransactionType> type = transactionTypeRepository.findById(txn.getTypeCd());
            line.put("typeDesc", type.map(t -> t.getTypeDesc().trim()).orElse(""));

            line.put("catCd", txn.getCatCd());
            Optional<TransactionCategory> cat = transactionCategoryRepository
                    .findById(new TransactionCategoryId(txn.getTypeCd(), txn.getCatCd()));
            line.put("catDesc", cat.map(c -> c.getCatTypeDesc().trim()).orElse(""));

            line.put("source", txn.getSource());
            line.put("amount", txn.getAmount());

            reportLines.add(line);

            grandTotal = grandTotal.add(txn.getAmount());
            accountTotals.merge(acctId, txn.getAmount(), BigDecimal::add);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportName", "Daily Transaction Report");
        report.put("startDate", startDate.toString());
        report.put("endDate", endDate.toString());
        report.put("transactionCount", transactions.size());
        report.put("grandTotal", grandTotal);
        report.put("accountTotals", accountTotals);
        report.put("transactions", reportLines);

        log.info("Transactions in report: {}", transactions.size());
        log.info("Grand total: {}", grandTotal);
        log.info("END OF TRANSACTION REPORT JOB");

        return report;
    }
}
