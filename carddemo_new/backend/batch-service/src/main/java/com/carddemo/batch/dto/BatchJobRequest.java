package com.carddemo.batch.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobRequest {
    private String jobType;
    private LocalDate processDate;
    private String accountId;
    private String parameters;
}
