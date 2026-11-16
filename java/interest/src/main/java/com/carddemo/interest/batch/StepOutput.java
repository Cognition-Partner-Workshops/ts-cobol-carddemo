package com.carddemo.interest.batch;

import com.carddemo.interest.record.AccountRecord;
import com.carddemo.interest.record.TransactionRecord;

/**
 * What CBACT04C writes while handling one TCATBALF record, in program order: the REWRITE of the
 * previous account (1050, on an account break), the interest transaction (1300-B), and whether
 * the program abended (9999) after those writes.
 */
public record StepOutput(AccountRecord rewrittenAccount, long writeSeq, TransactionRecord transaction,
                         boolean abended) {
}
