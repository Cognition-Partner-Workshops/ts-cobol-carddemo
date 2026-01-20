package com.carddemo.transactiontype.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCategoryDto {
    private String categoryCode;
    private String categoryDescription;
    private String parentCategoryCode;
    private String categoryType;
    private String merchantCategoryCode;
    private String reportingGroup;
    private Boolean active;
    private Integer displayOrder;
}
