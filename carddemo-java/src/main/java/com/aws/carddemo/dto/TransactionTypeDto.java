package com.aws.carddemo.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionTypeDto {

    private Integer typeId;

    @NotBlank(message = "Type code is required")
    @Size(max = 2, message = "Type code must be at most 2 characters")
    private String typeCode;

    @NotBlank(message = "Type name is required")
    @Size(max = 50, message = "Type name must be at most 50 characters")
    private String typeName;

    @Size(max = 200, message = "Type description must be at most 200 characters")
    private String typeDesc;

    private Integer categoryId;
    private String categoryName;

    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
