package com.carddemo.repository;

import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.entity.TransactionCategoryBalance.TransactionCategoryBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * JPA repository for TransactionCategoryBalance entity operations.
 * Provides CRUD operations and custom queries for transaction category balance management.
 *
 * <p>Replaces mainframe VSAM file operations for TCATBAL file.
 *
 * @see TransactionCategoryBalance
 */
@Repository
public interface TransactionCategoryBalanceRepository 
        extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalanceId> {

    /**
     * Find all category balances for a specific account.
     *
     * @param accountId the account identifier
     * @return list of category balances for the account
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.id.accountId = :accountId")
    List<TransactionCategoryBalance> findByAccountId(@Param("accountId") Long accountId);

    /**
     * Find all category balances for a specific type code.
     *
     * @param typeCode the transaction type code
     * @return list of category balances for the type
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.id.typeCode = :typeCode")
    List<TransactionCategoryBalance> findByTypeCode(@Param("typeCode") String typeCode);

    /**
     * Find all category balances for a specific category code.
     *
     * @param categoryCode the category code
     * @return list of category balances for the category
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.id.categoryCode = :categoryCode")
    List<TransactionCategoryBalance> findByCategoryCode(@Param("categoryCode") Integer categoryCode);

    /**
     * Find category balances for an account and type code.
     *
     * @param accountId the account identifier
     * @param typeCode the transaction type code
     * @return list of category balances matching the criteria
     */
    @Query("SELECT tcb FROM TransactionCategoryBalance tcb " +
           "WHERE tcb.id.accountId = :accountId AND tcb.id.typeCode = :typeCode")
    List<TransactionCategoryBalance> findByAccountIdAndTypeCode(
            @Param("accountId") Long accountId, @Param("typeCode") String typeCode);

    /**
     * Calculate total balance for an account across all categories.
     *
     * @param accountId the account identifier
     * @return total balance for the account
     */
    @Query("SELECT COALESCE(SUM(tcb.balance), 0) FROM TransactionCategoryBalance tcb " +
           "WHERE tcb.id.accountId = :accountId")
    BigDecimal sumBalanceByAccountId(@Param("accountId") Long accountId);

    /**
     * Find category balances with balance greater than specified amount.
     *
     * @param balance the minimum balance threshold
     * @return list of category balances above the threshold
     */
    List<TransactionCategoryBalance> findByBalanceGreaterThan(BigDecimal balance);
}
