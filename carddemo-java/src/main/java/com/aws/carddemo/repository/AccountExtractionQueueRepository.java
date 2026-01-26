package com.aws.carddemo.repository;

import com.aws.carddemo.entity.AccountExtractionQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountExtractionQueueRepository extends JpaRepository<AccountExtractionQueue, Long> {

    Optional<AccountExtractionQueue> findByRequestId(String requestId);

    List<AccountExtractionQueue> findByRequestStatus(String status);

    List<AccountExtractionQueue> findByAccountAcctId(Long acctId);

    @Query("SELECT q FROM AccountExtractionQueue q WHERE q.requestStatus = 'PENDING' ORDER BY q.createdAt ASC")
    List<AccountExtractionQueue> findPendingRequests();

    @Query("SELECT COUNT(q) FROM AccountExtractionQueue q WHERE q.requestStatus = :status")
    long countByStatus(@Param("status") String status);
}
