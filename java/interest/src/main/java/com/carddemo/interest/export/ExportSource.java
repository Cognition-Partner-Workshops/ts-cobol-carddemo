package com.carddemo.interest.export;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * What the export reads of a finished run (architecture.md section 3: interest_run,
 * interest_transaction and account are read by interest-export) and the one thing it records:
 * that the run was exported. The adapter is {@code jdbc.export.JdbcExportSource}.
 */
public interface ExportSource {

    /** The status of interest_run that the calculation leaves and the only one that is exported. */
    String CALCULATED = "CALCULATED";

    /** The run and the figures of strategy item RC5 recorded after the calculation; absent figures are null. */
    record RunToExport(long runId, String status, Long writtenTransactionCount, BigDecimal writtenAmountSum) {
    }

    /** One row of interest_transaction: the fields of layout CVTRA05Y; texts as stored, filler raw (E4). */
    record TransactionRow(int sequenceNumber, String id, String typeCode, long categoryCode, String source,
            String description, BigDecimal amount, long merchantId, String merchantName, String merchantCity,
            String merchantZip, String cardNumber, String originTimestamp, String processedTimestamp, byte[] filler) {
    }

    /** One row of account: the fields of layout CVACT01Y; texts as stored, filler raw (E4). */
    record AccountRow(long accountId, String activeStatus, BigDecimal currentBalance, BigDecimal creditLimit,
            BigDecimal cashCreditLimit, String openDate, String expirationDate, String reissueDate,
            BigDecimal cycleCredit, BigDecimal cycleDebit, String zip, String groupId, byte[] filler) {
    }

    Optional<RunToExport> run(long runId);

    /** The transactions of the run in the order they were written (seq_no): the order of SYSTRAN. */
    List<TransactionRow> transactions(long runId);

    /** Every account in key order (acct_id): the order of the KSDS. */
    List<AccountRow> accounts();

    /** Sets the run to EXPORTED if it still is CALCULATED; false when it is not. */
    boolean markExported(long runId);
}
