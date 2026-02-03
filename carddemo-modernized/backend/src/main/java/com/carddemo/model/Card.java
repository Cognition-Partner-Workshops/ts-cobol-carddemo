package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cards")
public class Card {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String cardNumber;
    
    @Indexed
    private String accountId;
    
    private String cvvCode;
    private String embossedName;
    private LocalDate expirationDate;
    private String activeStatus;
}
