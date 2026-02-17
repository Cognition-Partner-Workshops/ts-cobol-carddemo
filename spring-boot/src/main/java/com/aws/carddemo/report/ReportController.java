package com.aws.carddemo.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aws.carddemo.report.dto.CardTransactionGroup;
import com.aws.carddemo.report.dto.ReportData;
import com.aws.carddemo.report.dto.ReportRequest;
import com.aws.carddemo.report.dto.ReportStatusResponse;
import com.aws.carddemo.report.dto.TransactionDetail;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> submitReport(@Valid @RequestBody ReportRequest request) {
        String jobId = reportService.submitReport(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("jobId", jobId));
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<ReportStatusResponse> getReportStatus(@PathVariable String jobId) {
        return ResponseEntity.ok(reportService.getJobStatus(jobId));
    }

    @GetMapping("/{jobId}/download")
    public ResponseEntity<?> downloadReport(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "json") String format) {
        ReportData reportData = reportService.getReportData(jobId);

        if ("csv".equalsIgnoreCase(format)) {
            String csv = convertToCsv(reportData);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + jobId + ".csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv);
        }

        return ResponseEntity.ok(reportData);
    }

    private String convertToCsv(ReportData reportData) {
        StringBuilder csv = new StringBuilder();
        csv.append("Card Number,Transaction ID,Timestamp,Type,Type Description,Category,Category Description,Description,Amount,Merchant,City\n");

        for (CardTransactionGroup group : reportData.cardGroups()) {
            for (TransactionDetail txn : group.transactions()) {
                csv.append(escapeCsv(group.cardNumber())).append(",");
                csv.append(txn.transactionId()).append(",");
                csv.append(txn.timestamp()).append(",");
                csv.append(escapeCsv(txn.transactionType())).append(",");
                csv.append(escapeCsv(txn.typeDescription())).append(",");
                csv.append(escapeCsv(txn.transactionCategory())).append(",");
                csv.append(escapeCsv(txn.categoryDescription())).append(",");
                csv.append(escapeCsv(txn.description())).append(",");
                csv.append(txn.amount()).append(",");
                csv.append(escapeCsv(txn.merchantName())).append(",");
                csv.append(escapeCsv(txn.merchantCity())).append("\n");
            }
            csv.append(",,,,,,Card Total:,").append(group.cardTotal()).append(",,,\n");
        }
        csv.append(",,,,,,Grand Total:,").append(reportData.grandTotal()).append(",,,\n");
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
