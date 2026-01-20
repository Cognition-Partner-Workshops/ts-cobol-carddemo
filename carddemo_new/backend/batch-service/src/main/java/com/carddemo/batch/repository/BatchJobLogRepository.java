package com.carddemo.batch.repository;

import com.carddemo.batch.entity.BatchJobLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BatchJobLogRepository extends JpaRepository<BatchJobLog, Long> {
    List<BatchJobLog> findByJobName(String jobName);
    
    List<BatchJobLog> findByJobType(String jobType);
    
    List<BatchJobLog> findByStatus(String status);
    
    @Query("SELECT b FROM BatchJobLog b WHERE b.startTime >= :date ORDER BY b.startTime DESC")
    List<BatchJobLog> findJobsSince(@Param("date") LocalDateTime date);
    
    @Query("SELECT b FROM BatchJobLog b ORDER BY b.startTime DESC")
    List<BatchJobLog> findAllOrderByStartTimeDesc();
}
