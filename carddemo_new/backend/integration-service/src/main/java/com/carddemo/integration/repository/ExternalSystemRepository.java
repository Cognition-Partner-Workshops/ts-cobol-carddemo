package com.carddemo.integration.repository;

import com.carddemo.integration.entity.ExternalSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalSystemRepository extends JpaRepository<ExternalSystem, Long> {
    Optional<ExternalSystem> findBySystemCode(String systemCode);
    
    List<ExternalSystem> findBySystemType(String systemType);
    
    List<ExternalSystem> findByActiveTrue();
    
    List<ExternalSystem> findByHealthStatus(String healthStatus);
}
