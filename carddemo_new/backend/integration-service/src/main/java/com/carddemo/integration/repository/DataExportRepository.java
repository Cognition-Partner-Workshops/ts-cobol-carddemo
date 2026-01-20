package com.carddemo.integration.repository;

import com.carddemo.integration.entity.DataExport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DataExportRepository extends JpaRepository<DataExport, Long> {
    Optional<DataExport> findByExportId(String exportId);
    
    List<DataExport> findByExportType(String exportType);
    
    List<DataExport> findByTargetSystem(String targetSystem);
    
    List<DataExport> findByEntityType(String entityType);
    
    List<DataExport> findByStatus(String status);
    
    List<DataExport> findByRequestedBy(String requestedBy);
    
    @Query("SELECT e FROM DataExport e WHERE e.startedAt >= :since ORDER BY e.startedAt DESC")
    List<DataExport> findExportsSince(@Param("since") LocalDateTime since);
}
