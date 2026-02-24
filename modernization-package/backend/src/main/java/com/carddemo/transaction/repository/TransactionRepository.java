package com.carddemo.transaction.repository;

import com.carddemo.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Transaction entity.
 * Replaces VSAM access patterns for TRANSACT file:
 *   CT00 List: STARTBR + READNEXT/READPREV -> SELECT ... ORDER BY ... LIMIT/OFFSET
 *   CT01 View: READ by TRAN-ID -> SELECT ... WHERE transaction_id = ?
 *   CT02 Add:  WRITE TRANSACT -> INSERT INTO transaction VALUES (...)
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /**
     * Paginated list with optional filter by starting Transaction ID.
     * Replaces CT00 STARTBR at specified ID (BR-LT-04).
     */
    Page<Transaction> findByTransactionIdGreaterThanEqualOrderByTransactionIdAsc(
            String startTransactionId, Pageable pageable);

    /**
     * Paginated list ordered by Transaction ID ascending (BR-LT-01, BR-LT-07).
     */
    Page<Transaction> findAllByOrderByTransactionIdAsc(Pageable pageable);

    /**
     * Get the latest (highest ID) transaction for PF5 Copy Last feature.
     * Replaces legacy: STARTBR TRANSACT with HIGH-VALUES -> READPREV
     */
    Optional<Transaction> findFirstByOrderByTransactionIdDesc();

    /**
     * Generate next transaction ID using PostgreSQL sequence (BR-AT-13).
     * Thread-safe replacement for legacy browse-to-end + 1 pattern.
     */
    @Query(value = "SELECT LPAD(nextval('transaction_id_seq')::TEXT, 16, '0')", nativeQuery = true)
    String generateNextTransactionId();

    /**
     * Check if a transaction ID already exists (BR-AT-14).
     */
    boolean existsByTransactionId(@Param("transactionId") String transactionId);
}
