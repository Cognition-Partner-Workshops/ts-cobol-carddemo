package com.carddemo.integration.service;

import com.carddemo.integration.dto.ExternalSystemDto;
import com.carddemo.integration.entity.ExternalSystem;
import com.carddemo.integration.repository.ExternalSystemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalSystemService {
    
    private final ExternalSystemRepository systemRepository;
    
    public List<ExternalSystem> getAllSystems() {
        return systemRepository.findAll();
    }
    
    public List<ExternalSystem> getActiveSystems() {
        return systemRepository.findByActiveTrue();
    }
    
    public Optional<ExternalSystem> getSystemByCode(String systemCode) {
        return systemRepository.findBySystemCode(systemCode);
    }
    
    public List<ExternalSystem> getSystemsByType(String systemType) {
        return systemRepository.findBySystemType(systemType);
    }
    
    @Transactional
    public ExternalSystem createSystem(ExternalSystemDto dto) {
        ExternalSystem system = new ExternalSystem();
        mapDtoToEntity(dto, system);
        system.setCreatedAt(LocalDateTime.now());
        system.setUpdatedAt(LocalDateTime.now());
        system.setHealthStatus("UNKNOWN");
        
        ExternalSystem saved = systemRepository.save(system);
        log.info("Created external system: {}", saved.getSystemCode());
        return saved;
    }
    
    @Transactional
    public Optional<ExternalSystem> updateSystem(String systemCode, ExternalSystemDto dto) {
        Optional<ExternalSystem> systemOpt = systemRepository.findBySystemCode(systemCode);
        if (systemOpt.isEmpty()) {
            return Optional.empty();
        }
        
        ExternalSystem system = systemOpt.get();
        mapDtoToEntity(dto, system);
        system.setUpdatedAt(LocalDateTime.now());
        
        ExternalSystem saved = systemRepository.save(system);
        log.info("Updated external system: {}", saved.getSystemCode());
        return Optional.of(saved);
    }
    
    @Transactional
    public boolean deleteSystem(String systemCode) {
        Optional<ExternalSystem> systemOpt = systemRepository.findBySystemCode(systemCode);
        if (systemOpt.isEmpty()) {
            return false;
        }
        
        systemRepository.delete(systemOpt.get());
        log.info("Deleted external system: {}", systemCode);
        return true;
    }
    
    @Transactional
    public Optional<ExternalSystem> performHealthCheck(String systemCode) {
        Optional<ExternalSystem> systemOpt = systemRepository.findBySystemCode(systemCode);
        if (systemOpt.isEmpty()) {
            return Optional.empty();
        }
        
        ExternalSystem system = systemOpt.get();
        
        // Simulate health check (in real implementation, would call the endpoint)
        try {
            // For demo purposes, mark as healthy
            system.setHealthStatus("HEALTHY");
            system.setLastHealthCheck(LocalDateTime.now());
            log.info("Health check passed for system: {}", systemCode);
        } catch (Exception e) {
            system.setHealthStatus("UNHEALTHY");
            system.setLastHealthCheck(LocalDateTime.now());
            log.error("Health check failed for system {}: {}", systemCode, e.getMessage());
        }
        
        return Optional.of(systemRepository.save(system));
    }
    
    @Transactional
    public Optional<ExternalSystem> toggleSystemStatus(String systemCode) {
        Optional<ExternalSystem> systemOpt = systemRepository.findBySystemCode(systemCode);
        if (systemOpt.isEmpty()) {
            return Optional.empty();
        }
        
        ExternalSystem system = systemOpt.get();
        system.setActive(!system.getActive());
        system.setUpdatedAt(LocalDateTime.now());
        
        ExternalSystem saved = systemRepository.save(system);
        log.info("Toggled external system {} to active={}", systemCode, saved.getActive());
        return Optional.of(saved);
    }
    
    private void mapDtoToEntity(ExternalSystemDto dto, ExternalSystem system) {
        system.setSystemCode(dto.getSystemCode());
        system.setSystemName(dto.getSystemName());
        system.setSystemType(dto.getSystemType());
        system.setDescription(dto.getDescription());
        system.setEndpointUrl(dto.getEndpointUrl());
        system.setAuthType(dto.getAuthType());
        system.setApiKey(dto.getApiKey());
        system.setUsername(dto.getUsername());
        system.setPassword(dto.getPassword());
        system.setTimeoutSeconds(dto.getTimeoutSeconds() != null ? dto.getTimeoutSeconds() : 30);
        system.setRetryCount(dto.getRetryCount() != null ? dto.getRetryCount() : 3);
        system.setActive(dto.getActive() != null ? dto.getActive() : true);
    }
}
