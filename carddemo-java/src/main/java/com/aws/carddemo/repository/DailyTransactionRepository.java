package com.aws.carddemo.repository;

import com.aws.carddemo.entity.DailyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyTransactionRepository extends JpaRepository<DailyTransaction, Long> {

    List<DailyTransaction> findByProcessed(Boolean processed);

    Page<DailyTransaction> findByProcessed(Boolean processed, Pageable pageable);

    List<DailyTransaction> findByTranCardNum(String cardNum);

    @Query("SELECT d FROM DailyTransaction d WHERE d.processed = false ORDER BY d.createdAt ASC")
    List<DailyTransaction> findUnprocessedTransactions();

    @Query("SELECT d FROM DailyTransaction d WHERE d.processed = false ORDER BY d.createdAt ASC")
    Page<DailyTransaction> findUnprocessedTransactions(Pageable pageable);

    @Modifying
    @Query("UPDATE DailyTransaction d SET d.processed = true WHERE d.id = :id")
    void markAsProcessed(@Param("id") Long id);

    @Modifying
    @Query("UPDATE DailyTransaction d SET d.processed = true WHERE d.id IN :ids")
    void markAsProcessed(@Param("ids") List<Long> ids);

    @Query("SELECT COUNT(d) FROM DailyTransaction d WHERE d.processed = false")
    long countUnprocessed();

    @Query("SELECT COUNT(d) FROM DailyTransaction d WHERE d.processed = true")
    long countProcessed();
}
