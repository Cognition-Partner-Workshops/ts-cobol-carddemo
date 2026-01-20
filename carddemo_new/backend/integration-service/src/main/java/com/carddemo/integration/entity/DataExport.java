package com.carddemo.integration.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "data_exports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataExport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "export_id", length = 30, unique = true)
    private String exportId;

    @Column(name = "export_type", length = 30)
    private String exportType;

    @Column(name = "target_system", length = 20)
    private String targetSystem;

    @Column(name = "entity_type", length = 30)
    private String entityType;

    @Column(name = "filter_criteria", length = 500)
    private String filterCriteria;

    @Column(name = "record_count")
    private Integer recordCount;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_format", length = 20)
    private String fileFormat;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "requested_by", length = 50)
    private String requestedBy;
}
