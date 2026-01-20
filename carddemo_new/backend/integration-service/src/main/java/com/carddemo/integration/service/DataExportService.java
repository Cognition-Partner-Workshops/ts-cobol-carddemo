package com.carddemo.integration.service;

import com.carddemo.integration.dto.DataExportRequestDto;
import com.carddemo.integration.entity.DataExport;
import com.carddemo.integration.repository.DataExportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataExportService {
    
    private final DataExportRepository exportRepository;
    
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    
    public List<DataExport> getAllExports() {
        return exportRepository.findAll();
    }
    
    public Optional<DataExport> getExportById(String exportId) {
        return exportRepository.findByExportId(exportId);
    }
    
    public List<DataExport> getExportsByType(String exportType) {
        return exportRepository.findByExportType(exportType);
    }
    
    public List<DataExport> getExportsByStatus(String status) {
        return exportRepository.findByStatus(status);
    }
    
    public List<DataExport> getExportsByUser(String requestedBy) {
        return exportRepository.findByRequestedBy(requestedBy);
    }
    
    public List<DataExport> getRecentExports(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return exportRepository.findExportsSince(since);
    }
    
    @Transactional
    public DataExport createExport(DataExportRequestDto dto) {
        DataExport export = new DataExport();
        export.setExportId(generateExportId());
        export.setExportType(dto.getExportType());
        export.setTargetSystem(dto.getTargetSystem());
        export.setEntityType(dto.getEntityType());
        export.setFilterCriteria(dto.getFilterCriteria());
        export.setFileFormat(dto.getFileFormat() != null ? dto.getFileFormat() : "JSON");
        export.setRequestedBy(dto.getRequestedBy());
        export.setStatus(STATUS_PENDING);
        export.setStartedAt(LocalDateTime.now());
        
        DataExport saved = exportRepository.save(export);
        log.info("Created data export: {} for entity {}", saved.getExportId(), saved.getEntityType());
        
        // Trigger async processing
        processExportAsync(saved.getExportId());
        
        return saved;
    }
    
    @Async
    public void processExportAsync(String exportId) {
        try {
            Thread.sleep(100); // Small delay to allow transaction to commit
            processExport(exportId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Export processing interrupted: {}", exportId);
        }
    }
    
    @Transactional
    public Optional<DataExport> processExport(String exportId) {
        Optional<DataExport> exportOpt = exportRepository.findByExportId(exportId);
        if (exportOpt.isEmpty()) {
            return Optional.empty();
        }
        
        DataExport export = exportOpt.get();
        
        if (!STATUS_PENDING.equals(export.getStatus())) {
            log.warn("Export {} is not in processable state: {}", exportId, export.getStatus());
            return Optional.of(export);
        }
        
        export.setStatus(STATUS_PROCESSING);
        exportRepository.save(export);
        
        try {
            // Simulate export processing (in real implementation, would query data and generate file)
            log.info("Processing export: {} entity={}", exportId, export.getEntityType());
            
            // Simulate record count
            int recordCount = (int) (Math.random() * 1000) + 100;
            String filePath = "/exports/" + export.getEntityType().toLowerCase() + "_" + exportId + "." + export.getFileFormat().toLowerCase();
            
            export.setRecordCount(recordCount);
            export.setFilePath(filePath);
            export.setStatus(STATUS_COMPLETED);
            export.setCompletedAt(LocalDateTime.now());
            
            log.info("Export {} completed: {} records exported to {}", exportId, recordCount, filePath);
            
        } catch (Exception e) {
            export.setStatus(STATUS_FAILED);
            export.setErrorMessage(e.getMessage());
            export.setCompletedAt(LocalDateTime.now());
            log.error("Export {} failed: {}", exportId, e.getMessage());
        }
        
        return Optional.of(exportRepository.save(export));
    }
    
    @Transactional
    public boolean cancelExport(String exportId) {
        Optional<DataExport> exportOpt = exportRepository.findByExportId(exportId);
        if (exportOpt.isEmpty()) {
            return false;
        }
        
        DataExport export = exportOpt.get();
        if (STATUS_COMPLETED.equals(export.getStatus()) || STATUS_FAILED.equals(export.getStatus())) {
            log.warn("Cannot cancel export {} in state: {}", exportId, export.getStatus());
            return false;
        }
        
        export.setStatus("CANCELLED");
        export.setCompletedAt(LocalDateTime.now());
        exportRepository.save(export);
        
        log.info("Export {} cancelled", exportId);
        return true;
    }
    
    private String generateExportId() {
        return "EXP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
