package com.aws.carddemo.repository;

import com.aws.carddemo.entity.BatchJobLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchJobLogRepository extends JpaRepository<BatchJobLog, Long> {

    List<BatchJobLog> findByJobName(String jobName);

    List<BatchJobLog> findByJobStatus(String jobStatus);

    @Query("SELECT b FROM BatchJobLog b WHERE b.startTime BETWEEN :startDate AND :endDate")
    List<BatchJobLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT b FROM BatchJobLog b WHERE b.jobName = :jobName ORDER BY b.startTime DESC")
    List<BatchJobLog> findRecentJobsByName(@Param("jobName") String jobName);

    @Query("SELECT b FROM BatchJobLog b WHERE b.jobName = :jobName ORDER BY b.startTime DESC LIMIT 1")
    Optional<BatchJobLog> findLatestByJobName(@Param("jobName") String jobName);

    @Query("SELECT b FROM BatchJobLog b WHERE b.jobStatus = 'RUNNING'")
    List<BatchJobLog> findRunningJobs();
}
