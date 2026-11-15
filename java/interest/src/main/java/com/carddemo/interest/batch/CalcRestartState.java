package com.carddemo.interest.batch;

import java.util.OptionalLong;

import org.springframework.batch.item.ExecutionContext;

/**
 * What calcInterestStep saves with every commit (architecture.md section 6, REL-11 as proposed):
 * the run's row in interest_run, the last account of the chunk, the transaction counter (the six
 * digits of TRAN-ID, INT-Q02), seq_no, the number of accounts updated and the length of the run log.
 */
record CalcRestartState(long interestRunId, OptionalLong lastAccountId, int transactionCounter, int lastSeqNo,
        long accountsUpdated, long runLogLength) {

    private static final String PREFIX = "calcInterestStep.";
    private static final String INTEREST_RUN_ID = PREFIX + "interestRunId";
    private static final String LAST_ACCOUNT_ID = PREFIX + "lastAccountId";
    private static final String TRANSACTION_COUNTER = PREFIX + "transactionCounter";
    private static final String LAST_SEQ_NO = PREFIX + "lastSeqNo";
    private static final String ACCOUNTS_UPDATED = PREFIX + "accountsUpdated";
    private static final String RUN_LOG_LENGTH = PREFIX + "runLogLength";

    /** The state of a step that has committed nothing yet. */
    static CalcRestartState beforeTheFirstAccount(long interestRunId, long runLogLength) {
        return new CalcRestartState(interestRunId, OptionalLong.empty(), 0, 0, 0, runLogLength);
    }

    static boolean savedIn(ExecutionContext context) {
        return context.containsKey(INTEREST_RUN_ID);
    }

    static CalcRestartState from(ExecutionContext context) {
        return new CalcRestartState(
                context.getLong(INTEREST_RUN_ID),
                context.containsKey(LAST_ACCOUNT_ID) ? OptionalLong.of(context.getLong(LAST_ACCOUNT_ID)) : OptionalLong.empty(),
                context.getInt(TRANSACTION_COUNTER),
                context.getInt(LAST_SEQ_NO),
                context.getLong(ACCOUNTS_UPDATED),
                context.getLong(RUN_LOG_LENGTH));
    }

    void saveIn(ExecutionContext context) {
        context.putLong(INTEREST_RUN_ID, interestRunId);
        lastAccountId.ifPresent(accountId -> context.putLong(LAST_ACCOUNT_ID, accountId));
        context.putInt(TRANSACTION_COUNTER, transactionCounter);
        context.putInt(LAST_SEQ_NO, lastSeqNo);
        context.putLong(ACCOUNTS_UPDATED, accountsUpdated);
        context.putLong(RUN_LOG_LENGTH, runLogLength);
    }
}
