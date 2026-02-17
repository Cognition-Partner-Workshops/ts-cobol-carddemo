package com.aws.carddemo.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.aws.carddemo.report.dto.CardTransactionGroup;
import com.aws.carddemo.report.dto.ReportData;
import com.aws.carddemo.report.dto.ReportRequest;
import com.aws.carddemo.report.dto.ReportStatusResponse.ReportJobStatus;
import com.aws.carddemo.report.dto.TransactionDetail;
import com.aws.carddemo.transaction.TransactionCategory;
import com.aws.carddemo.transaction.TransactionCategoryRepository;
import com.aws.carddemo.transaction.TransactionRecord;
import com.aws.carddemo.transaction.TransactionRecordRepository;
import com.aws.carddemo.transaction.TransactionType;
import com.aws.carddemo.transaction.TransactionTypeRepository;

@Component
public class ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);

    private final TransactionRecordRepository transactionRecordRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;

    public ReportGenerator(TransactionRecordRepository transactionRecordRepository,
                           TransactionTypeRepository transactionTypeRepository,
                           TransactionCategoryRepository transactionCategoryRepository) {
        this.transactionRecordRepository = transactionRecordRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionCategoryRepository = transactionCategoryRepository;
    }

    @Async("reportTaskExecutor")
    @Transactional(readOnly = true)
    public void generateReportAsync(ReportJob job, ReportRequest request) {
        job.setStatus(ReportJobStatus.PROCESSING);
        try {
            LocalDate startDate = resolveStartDate(request);
            LocalDate endDate = resolveEndDate(request);

            Map<String, String> typeDescriptions = loadTypeDescriptions();
            Map<String, String> categoryDescriptions = loadCategoryDescriptions();

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

            List<TransactionRecord> transactions =
                    transactionRecordRepository.findByTimestampBetween(startDateTime, endDateTime);

            Map<String, List<TransactionRecord>> grouped = transactions.stream()
                    .collect(Collectors.groupingBy(t -> t.getCard().getCardNumber()));

            List<CardTransactionGroup> cardGroups = new ArrayList<>();
            BigDecimal grandTotal = BigDecimal.ZERO;

            for (Map.Entry<String, List<TransactionRecord>> entry : grouped.entrySet()) {
                String cardNumber = entry.getKey();
                List<TransactionRecord> cardTransactions = entry.getValue().stream()
                        .sorted(Comparator.comparing(TransactionRecord::getTimestamp))
                        .toList();

                List<TransactionDetail> details = new ArrayList<>();
                Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
                BigDecimal cardTotal = BigDecimal.ZERO;

                for (TransactionRecord txn : cardTransactions) {
                    String typeDesc = typeDescriptions.getOrDefault(
                            txn.getTransactionType(), txn.getTransactionType());
                    String catDesc = categoryDescriptions.getOrDefault(
                            txn.getTransactionCategory(), txn.getTransactionCategory());

                    details.add(new TransactionDetail(
                            txn.getId(),
                            txn.getTimestamp(),
                            txn.getTransactionType(),
                            typeDesc,
                            txn.getTransactionCategory(),
                            catDesc,
                            txn.getDescription(),
                            txn.getAmount(),
                            txn.getMerchantName(),
                            txn.getMerchantCity()
                    ));

                    String categoryKey = catDesc != null ? catDesc : "Uncategorized";
                    categoryTotals.merge(categoryKey, txn.getAmount(), BigDecimal::add);
                    cardTotal = cardTotal.add(txn.getAmount());
                }

                grandTotal = grandTotal.add(cardTotal);
                cardGroups.add(new CardTransactionGroup(cardNumber, details, categoryTotals, cardTotal));
            }

            ReportData reportData = new ReportData(
                    request.reportType().name(),
                    startDate,
                    endDate,
                    cardGroups,
                    grandTotal
            );

            job.setReportData(reportData);
            job.setStatus(ReportJobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            log.info("Report job {} completed successfully", job.getJobId());
        } catch (Exception e) {
            log.error("Report job {} failed: {}", job.getJobId(), e.getMessage(), e);
            job.setStatus(ReportJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
        }
    }

    private LocalDate resolveStartDate(ReportRequest request) {
        return switch (request.reportType()) {
            case MONTHLY -> LocalDate.of(request.year(), request.month(), 1);
            case YEARLY -> LocalDate.of(request.year(), 1, 1);
            case CUSTOM -> request.startDate();
        };
    }

    private LocalDate resolveEndDate(ReportRequest request) {
        return switch (request.reportType()) {
            case MONTHLY -> LocalDate.of(request.year(), request.month(), 1).withDayOfMonth(
                    LocalDate.of(request.year(), request.month(), 1).lengthOfMonth());
            case YEARLY -> LocalDate.of(request.year(), 12, 31);
            case CUSTOM -> request.endDate();
        };
    }

    private Map<String, String> loadTypeDescriptions() {
        return transactionTypeRepository.findAll().stream()
                .collect(Collectors.toMap(TransactionType::getTypeCd, TransactionType::getTypeDesc));
    }

    private Map<String, String> loadCategoryDescriptions() {
        return transactionCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(TransactionCategory::getCatCd, TransactionCategory::getCatDesc));
    }
}
