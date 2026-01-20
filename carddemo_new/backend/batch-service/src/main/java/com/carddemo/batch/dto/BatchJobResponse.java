package com.carddemo.batch.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchJobResponse {
    private Long jobId;
    private String jobName;
    private String jobType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer recordsProcessed;
    private Integer recordsSuccess;
    private Integer recordsFailed;
    private String errorMessage;
}
