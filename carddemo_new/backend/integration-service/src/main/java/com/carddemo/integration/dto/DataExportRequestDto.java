package com.carddemo.integration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataExportRequestDto {
    private String exportType;
    private String targetSystem;
    private String entityType;
    private String filterCriteria;
    private String fileFormat;
    private String requestedBy;
}
