package com.carddemo.modern.interest;

import com.carddemo.modern.interest.Records.Account;
import com.carddemo.modern.interest.Records.Transaction;

/** What one TCATBALF record makes CBACT04C write: a REWRITE of the previous account and/or one SYSTRAN record. */
record Step15Output(Account rewrite, Transaction transaction, int transactionSeq) {
}
