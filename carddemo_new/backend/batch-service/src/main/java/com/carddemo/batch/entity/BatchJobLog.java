package com.carddemo.batch.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_job_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", length = 50)
    private String jobName;

    @Column(name = "job_type", length = 30)
    private String jobType;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "records_success")
    private Integer recordsSuccess;

    @Column(name = "records_failed")
    private Integer recordsFailed;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "parameters", length = 500)
    private String parameters;
}
