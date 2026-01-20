package com.carddemo.integration.controller;

import com.carddemo.integration.dto.*;
import com.carddemo.integration.entity.*;
import com.carddemo.integration.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
@Slf4j
public class IntegrationController {
    
    private final ExternalSystemService systemService;
    private final MessageService messageService;
    private final DataExportService exportService;
    
    // EPIC-011 Feature 1: External System Management
    @GetMapping("/systems")
    public ResponseEntity<List<ExternalSystem>> getAllSystems() {
        List<ExternalSystem> systems = systemService.getAllSystems();
        return ResponseEntity.ok(systems);
    }
    
    @GetMapping("/systems/active")
    public ResponseEntity<List<ExternalSystem>> getActiveSystems() {
        List<ExternalSystem> systems = systemService.getActiveSystems();
        return ResponseEntity.ok(systems);
    }
    
    @GetMapping("/systems/{systemCode}")
    public ResponseEntity<ExternalSystem> getSystem(@PathVariable String systemCode) {
        return systemService.getSystemByCode(systemCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/systems/type/{systemType}")
    public ResponseEntity<List<ExternalSystem>> getSystemsByType(@PathVariable String systemType) {
        List<ExternalSystem> systems = systemService.getSystemsByType(systemType);
        return ResponseEntity.ok(systems);
    }
    
    @PostMapping("/systems")
    public ResponseEntity<ExternalSystem> createSystem(@RequestBody ExternalSystemDto dto) {
        ExternalSystem system = systemService.createSystem(dto);
        return ResponseEntity.ok(system);
    }
    
    @PutMapping("/systems/{systemCode}")
    public ResponseEntity<ExternalSystem> updateSystem(
            @PathVariable String systemCode,
            @RequestBody ExternalSystemDto dto) {
        return systemService.updateSystem(systemCode, dto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/systems/{systemCode}")
    public ResponseEntity<Void> deleteSystem(@PathVariable String systemCode) {
        if (systemService.deleteSystem(systemCode)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/systems/{systemCode}/health-check")
    public ResponseEntity<ExternalSystem> performHealthCheck(@PathVariable String systemCode) {
        return systemService.performHealthCheck(systemCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/systems/{systemCode}/toggle")
    public ResponseEntity<ExternalSystem> toggleSystemStatus(@PathVariable String systemCode) {
        return systemService.toggleSystemStatus(systemCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // EPIC-011 Feature 2: Message Queue Integration
    @GetMapping("/messages")
    public ResponseEntity<List<IntegrationMessage>> getAllMessages() {
        List<IntegrationMessage> messages = messageService.getAllMessages();
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/messages/{messageId}")
    public ResponseEntity<IntegrationMessage> getMessage(@PathVariable String messageId) {
        return messageService.getMessageById(messageId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/messages/correlation/{correlationId}")
    public ResponseEntity<List<IntegrationMessage>> getMessagesByCorrelation(@PathVariable String correlationId) {
        List<IntegrationMessage> messages = messageService.getMessagesByCorrelation(correlationId);
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/messages/status/{status}")
    public ResponseEntity<List<IntegrationMessage>> getMessagesByStatus(@PathVariable String status) {
        List<IntegrationMessage> messages = messageService.getMessagesByStatus(status);
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/messages/pending")
    public ResponseEntity<List<IntegrationMessage>> getPendingMessages() {
        List<IntegrationMessage> messages = messageService.getPendingMessages();
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/messages/recent")
    public ResponseEntity<List<IntegrationMessage>> getRecentMessages(
            @RequestParam(defaultValue = "24") int hours) {
        List<IntegrationMessage> messages = messageService.getRecentMessages(hours);
        return ResponseEntity.ok(messages);
    }
    
    @PostMapping("/messages/send")
    public ResponseEntity<IntegrationMessage> sendMessage(@RequestBody IntegrationMessageDto dto) {
        IntegrationMessage message = messageService.sendMessage(dto);
        return ResponseEntity.ok(message);
    }
    
    @PostMapping("/messages/receive")
    public ResponseEntity<IntegrationMessage> receiveMessage(@RequestBody IntegrationMessageDto dto) {
        IntegrationMessage message = messageService.receiveMessage(dto);
        return ResponseEntity.ok(message);
    }
    
    @PostMapping("/messages/{messageId}/retry")
    public ResponseEntity<IntegrationMessage> retryMessage(@PathVariable String messageId) {
        return messageService.retryMessage(messageId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // EPIC-011 Feature 2: Data Export
    @GetMapping("/exports")
    public ResponseEntity<List<DataExport>> getAllExports() {
        List<DataExport> exports = exportService.getAllExports();
        return ResponseEntity.ok(exports);
    }
    
    @GetMapping("/exports/{exportId}")
    public ResponseEntity<DataExport> getExport(@PathVariable String exportId) {
        return exportService.getExportById(exportId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/exports/type/{exportType}")
    public ResponseEntity<List<DataExport>> getExportsByType(@PathVariable String exportType) {
        List<DataExport> exports = exportService.getExportsByType(exportType);
        return ResponseEntity.ok(exports);
    }
    
    @GetMapping("/exports/status/{status}")
    public ResponseEntity<List<DataExport>> getExportsByStatus(@PathVariable String status) {
        List<DataExport> exports = exportService.getExportsByStatus(status);
        return ResponseEntity.ok(exports);
    }
    
    @GetMapping("/exports/user/{requestedBy}")
    public ResponseEntity<List<DataExport>> getExportsByUser(@PathVariable String requestedBy) {
        List<DataExport> exports = exportService.getExportsByUser(requestedBy);
        return ResponseEntity.ok(exports);
    }
    
    @GetMapping("/exports/recent")
    public ResponseEntity<List<DataExport>> getRecentExports(
            @RequestParam(defaultValue = "24") int hours) {
        List<DataExport> exports = exportService.getRecentExports(hours);
        return ResponseEntity.ok(exports);
    }
    
    @PostMapping("/exports")
    public ResponseEntity<DataExport> createExport(@RequestBody DataExportRequestDto dto) {
        DataExport export = exportService.createExport(dto);
        return ResponseEntity.ok(export);
    }
    
    @PostMapping("/exports/{exportId}/cancel")
    public ResponseEntity<Void> cancelExport(@PathVariable String exportId) {
        if (exportService.cancelExport(exportId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
