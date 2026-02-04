package com.aws.carddemo.service.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardDTO {
    private String cardNumber;
    private String maskedCardNumber;
    private Long accountId;
    private String embossedName;
    private LocalDate expirationDate;
    private String activeStatus;
    private boolean expired;
}
