package com.carddemo.modern.interest;

import com.carddemo.modern.interest.Records.Account;
import com.carddemo.modern.interest.Records.Transaction;

/**
 * What one TCATBALF record makes CBACT04C write: a REWRITE of the previous account and/or one SYSTRAN record.
 * {@code abend}: the program abended on this record, after the writes above.
 */
record Step15Output(Account rewrite, Transaction transaction, int transactionSeq, boolean abend) {
}
