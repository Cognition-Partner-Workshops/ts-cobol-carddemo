package com.aws.carddemo.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aws.carddemo.card.Card;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;
import com.aws.carddemo.report.dto.ReportData;
import com.aws.carddemo.report.dto.ReportRequest;
import com.aws.carddemo.report.dto.ReportRequest.ReportType;
import com.aws.carddemo.report.dto.ReportStatusResponse;
import com.aws.carddemo.report.dto.ReportStatusResponse.ReportJobStatus;
import com.aws.carddemo.transaction.TransactionCategory;
import com.aws.carddemo.transaction.TransactionCategoryRepository;
import com.aws.carddemo.transaction.TransactionRecord;
import com.aws.carddemo.transaction.TransactionRecordRepository;
import com.aws.carddemo.transaction.TransactionType;
import com.aws.carddemo.transaction.TransactionTypeRepository;

@ExtendWith(MockitoExtension.class)
class ReportServiceTests {

    @Mock
    private TransactionRecordRepository transactionRecordRepository;

    @Mock
    private TransactionTypeRepository transactionTypeRepository;

    @Mock
    private TransactionCategoryRepository transactionCategoryRepository;

    private ReportService reportService;

    private Card card1;
    private Card card2;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                transactionRecordRepository,
                transactionTypeRepository,
                transactionCategoryRepository
        );

        card1 = new Card();
        card1.setCardNumber("4111111111111111");

        card2 = new Card();
        card2.setCardNumber("4222222222222222");
    }

    @Test
    void submitMonthlyReportReturnsJobId() {
        when(transactionRecordRepository.findAll()).thenReturn(List.of());
        when(transactionTypeRepository.findAll()).thenReturn(List.of());
        when(transactionCategoryRepository.findAll()).thenReturn(List.of());

        ReportRequest request = new ReportRequest(ReportType.MONTHLY, 6, 2025, null, null);
        String jobId = reportService.submitReport(request);

        assertThat(jobId).isNotNull().isNotEmpty();
    }

    @Test
    void submitMonthlyReportMissingMonthThrows() {
        ReportRequest request = new ReportRequest(ReportType.MONTHLY, null, 2025, null, null);

        assertThatThrownBy(() -> reportService.submitReport(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Month and year are required");
    }

    @Test
    void submitMonthlyReportInvalidMonthThrows() {
        ReportRequest request = new ReportRequest(ReportType.MONTHLY, 13, 2025, null, null);

        assertThatThrownBy(() -> reportService.submitReport(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Month must be between 1 and 12");
    }

    @Test
    void submitYearlyReportMissingYearThrows() {
        ReportRequest request = new ReportRequest(ReportType.YEARLY, null, null, null, null);

        assertThatThrownBy(() -> reportService.submitReport(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Year is required");
    }

    @Test
    void submitCustomReportMissingDatesThrows() {
        ReportRequest request = new ReportRequest(ReportType.CUSTOM, null, null, null, null);

        assertThatThrownBy(() -> reportService.submitReport(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Start date and end date are required");
    }

    @Test
    void submitCustomReportStartAfterEndThrows() {
        ReportRequest request = new ReportRequest(ReportType.CUSTOM, null, null,
                LocalDate.of(2025, 6, 30), LocalDate.of(2025, 6, 1));

        assertThatThrownBy(() -> reportService.submitReport(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Start date must be before or equal to end date");
    }

    @Test
    void getJobStatusNotFoundThrows() {
        assertThatThrownBy(() -> reportService.getJobStatus("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report job not found");
    }

    @Test
    void getReportDataNotCompletedThrows() {
        when(transactionRecordRepository.findAll()).thenReturn(List.of());
        when(transactionTypeRepository.findAll()).thenReturn(List.of());
        when(transactionCategoryRepository.findAll()).thenReturn(List.of());

        ReportRequest request = new ReportRequest(ReportType.YEARLY, null, 2020, null, null);
        String jobId = reportService.submitReport(request);

        ReportJob job = reportService.getJobStore().get(jobId);
        job.setStatus(ReportJobStatus.PROCESSING);

        assertThatThrownBy(() -> reportService.getReportData(jobId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not yet completed");
    }

    @Test
    void monthlyReportGroupsTransactionsByCard() {
        TransactionRecord txn1 = createTransaction(1L, card1, "SA", "5001", new BigDecimal("100.00"),
                LocalDateTime.of(2025, 6, 10, 12, 0));
        TransactionRecord txn2 = createTransaction(2L, card1, "SA", "5002", new BigDecimal("200.00"),
                LocalDateTime.of(2025, 6, 15, 14, 0));
        TransactionRecord txn3 = createTransaction(3L, card2, "SA", "5001", new BigDecimal("50.00"),
                LocalDateTime.of(2025, 6, 20, 10, 0));
        TransactionRecord outsideRange = createTransaction(4L, card1, "SA", "5001", new BigDecimal("999.00"),
                LocalDateTime.of(2025, 7, 1, 10, 0));

        when(transactionRecordRepository.findAll()).thenReturn(List.of(txn1, txn2, txn3, outsideRange));

        TransactionType type = new TransactionType();
        type.setTypeCd("SA");
        type.setTypeDesc("Sale");
        when(transactionTypeRepository.findAll()).thenReturn(List.of(type));

        TransactionCategory cat1 = new TransactionCategory();
        cat1.setCatCd("5001");
        cat1.setCatDesc("Groceries");
        TransactionCategory cat2 = new TransactionCategory();
        cat2.setCatCd("5002");
        cat2.setCatDesc("Restaurants");
        when(transactionCategoryRepository.findAll()).thenReturn(List.of(cat1, cat2));

        ReportRequest request = new ReportRequest(ReportType.MONTHLY, 6, 2025, null, null);
        String jobId = reportService.submitReport(request);

        ReportJob job = reportService.getJobStore().get(jobId);
        waitForJobCompletion(job);

        ReportData data = reportService.getReportData(jobId);
        assertThat(data.cardGroups()).hasSize(2);
        assertThat(data.grandTotal()).isEqualByComparingTo(new BigDecimal("350.00"));
        assertThat(data.reportType()).isEqualTo("MONTHLY");
        assertThat(data.periodStart()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(data.periodEnd()).isEqualTo(LocalDate.of(2025, 6, 30));
    }

    @Test
    void yearlyReportIncludesCorrectDateRange() {
        TransactionRecord txn = createTransaction(1L, card1, "SA", "5001", new BigDecimal("100.00"),
                LocalDateTime.of(2025, 3, 15, 12, 0));

        when(transactionRecordRepository.findAll()).thenReturn(List.of(txn));
        when(transactionTypeRepository.findAll()).thenReturn(List.of());
        when(transactionCategoryRepository.findAll()).thenReturn(List.of());

        ReportRequest request = new ReportRequest(ReportType.YEARLY, null, 2025, null, null);
        String jobId = reportService.submitReport(request);

        ReportJob job = reportService.getJobStore().get(jobId);
        waitForJobCompletion(job);

        ReportData data = reportService.getReportData(jobId);
        assertThat(data.periodStart()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(data.periodEnd()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(data.cardGroups()).hasSize(1);
        assertThat(data.grandTotal()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void customReportWithDateRange() {
        TransactionRecord txn1 = createTransaction(1L, card1, "SA", "5001", new BigDecimal("75.00"),
                LocalDateTime.of(2025, 3, 15, 12, 0));
        TransactionRecord txn2 = createTransaction(2L, card1, "SA", "5001", new BigDecimal("25.00"),
                LocalDateTime.of(2025, 3, 20, 12, 0));

        when(transactionRecordRepository.findAll()).thenReturn(List.of(txn1, txn2));
        when(transactionTypeRepository.findAll()).thenReturn(List.of());
        when(transactionCategoryRepository.findAll()).thenReturn(List.of());

        ReportRequest request = new ReportRequest(ReportType.CUSTOM, null, null,
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31));
        String jobId = reportService.submitReport(request);

        ReportJob job = reportService.getJobStore().get(jobId);
        waitForJobCompletion(job);

        ReportData data = reportService.getReportData(jobId);
        assertThat(data.cardGroups()).hasSize(1);
        assertThat(data.cardGroups().get(0).transactions()).hasSize(2);
        assertThat(data.grandTotal()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void reportCalculatesCategorySubtotals() {
        TransactionRecord txn1 = createTransaction(1L, card1, "SA", "5001", new BigDecimal("100.00"),
                LocalDateTime.of(2025, 6, 10, 12, 0));
        TransactionRecord txn2 = createTransaction(2L, card1, "SA", "5001", new BigDecimal("50.00"),
                LocalDateTime.of(2025, 6, 15, 12, 0));
        TransactionRecord txn3 = createTransaction(3L, card1, "SA", "5002", new BigDecimal("75.00"),
                LocalDateTime.of(2025, 6, 20, 12, 0));

        when(transactionRecordRepository.findAll()).thenReturn(List.of(txn1, txn2, txn3));

        TransactionCategory cat1 = new TransactionCategory();
        cat1.setCatCd("5001");
        cat1.setCatDesc("Groceries");
        TransactionCategory cat2 = new TransactionCategory();
        cat2.setCatCd("5002");
        cat2.setCatDesc("Restaurants");
        when(transactionCategoryRepository.findAll()).thenReturn(List.of(cat1, cat2));
        when(transactionTypeRepository.findAll()).thenReturn(List.of());

        ReportRequest request = new ReportRequest(ReportType.MONTHLY, 6, 2025, null, null);
        String jobId = reportService.submitReport(request);

        ReportJob job = reportService.getJobStore().get(jobId);
        waitForJobCompletion(job);

        ReportData data = reportService.getReportData(jobId);
        assertThat(data.cardGroups()).hasSize(1);
        assertThat(data.cardGroups().get(0).categoryTotals().get("Groceries"))
                .isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(data.cardGroups().get(0).categoryTotals().get("Restaurants"))
                .isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(data.cardGroups().get(0).cardTotal())
                .isEqualByComparingTo(new BigDecimal("225.00"));
    }

    @Test
    void reportTransactionsSortedByDate() {
        TransactionRecord txn1 = createTransaction(1L, card1, "SA", "5001", new BigDecimal("100.00"),
                LocalDateTime.of(2025, 6, 20, 12, 0));
        TransactionRecord txn2 = createTransaction(2L, card1, "SA", "5001", new BigDecimal("50.00"),
                LocalDateTime.of(2025, 6, 5, 12, 0));

        when(transactionRecordRepository.findAll()).thenReturn(List.of(txn1, txn2));
        when(transactionTypeRepository.findAll()).thenReturn(List.of());
        when(transactionCategoryRepository.findAll()).thenReturn(List.of());

        ReportRequest request = new ReportRequest(ReportType.MONTHLY, 6, 2025, null, null);
        String jobId = reportService.submitReport(request);

        ReportJob job = reportService.getJobStore().get(jobId);
        waitForJobCompletion(job);

        ReportData data = reportService.getReportData(jobId);
        assertThat(data.cardGroups().get(0).transactions().get(0).timestamp())
                .isBefore(data.cardGroups().get(0).transactions().get(1).timestamp());
    }

    @Test
    void emptyReportReturnsZeroGrandTotal() {
        when(transactionRecordRepository.findAll()).thenReturn(List.of());
        when(transactionTypeRepository.findAll()).thenReturn(List.of());
        when(transactionCategoryRepository.findAll()).thenReturn(List.of());

        ReportRequest request = new ReportRequest(ReportType.MONTHLY, 6, 2025, null, null);
        String jobId = reportService.submitReport(request);

        ReportJob job = reportService.getJobStore().get(jobId);
        waitForJobCompletion(job);

        ReportData data = reportService.getReportData(jobId);
        assertThat(data.cardGroups()).isEmpty();
        assertThat(data.grandTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private TransactionRecord createTransaction(Long id, Card card, String type, String category,
                                                  BigDecimal amount, LocalDateTime timestamp) {
        TransactionRecord txn = new TransactionRecord();
        txn.setId(id);
        txn.setCard(card);
        txn.setTransactionType(type);
        txn.setTransactionCategory(category);
        txn.setTransactionSource("ONLINE");
        txn.setAmount(amount);
        txn.setTimestamp(timestamp);
        txn.setDescription("Test transaction");
        txn.setMerchantName("Test Merchant");
        txn.setMerchantCity("Test City");
        return txn;
    }

    private void waitForJobCompletion(ReportJob job) {
        int maxWait = 50;
        while (job.getStatus() != ReportJobStatus.COMPLETED
                && job.getStatus() != ReportJobStatus.FAILED
                && maxWait-- > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
