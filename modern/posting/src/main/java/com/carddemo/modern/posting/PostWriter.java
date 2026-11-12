package com.carddemo.modern.posting;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;

/** 2500-WRITE-REJECT-REC or 2700-UPDATE-TCATBAL, 2800-UPDATE-ACCOUNT-REC, 2900-WRITE-TRANSACTION-FILE. */
final class PostWriter implements ItemWriter<Cbtrn02cProcessor.Posted> {
    private final PostingRepository repo;
    private final Cbtrn02cProcessor processor;
    private final TransactionTemplate operation;
    PostWriter(PostingRepository repo, Cbtrn02cProcessor processor, PlatformTransactionManager tx) {
        this.repo = repo; this.processor = processor;
        operation = new TransactionTemplate(tx);
        operation.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    }

    @Override
    public void write(Chunk<? extends Cbtrn02cProcessor.Posted> chunk) {
        for (var item : chunk) {
            String message = "ERROR WRITING TO REJECTS FILE";
            try {
                if (item.reject() != null) {
                    operation.executeWithoutResult(s -> repo.jdbc().update("INSERT INTO posting.reject VALUES (?,?)", item.seq(), item.reject()));
                } else {
                    message = item.createCategory() ? "ERROR WRITING TRANSACTION BALANCE FILE" : "ERROR REWRITING TRANSACTION BALANCE FILE";
                    operation.executeWithoutResult(s -> repo.postCategory(item.category(),item.createCategory()));
                    try {
                        operation.executeWithoutResult(s -> repo.rewriteAccount(item.account()));
                    } catch (DataAccessException ignored) {
                        // 2800 has INVALID KEY but no status check or abend; transaction WRITE still follows.
                    }
                    message = "ERROR WRITING TO TRANSACTION FILE";
                    operation.executeWithoutResult(s -> repo.transaction(item.transaction()));
                }
            } catch (DataAccessException e) {
                // A failed SQL operation rolls back only its savepoint. CEE3ABD keeps preceding writes.
                processor.abend(message,e instanceof DuplicateKeyException ? 22
                        : e instanceof org.springframework.dao.EmptyResultDataAccessException ? 23 : 30);
                Abend.stopStep();
                break;
            }
        }
    }
}
