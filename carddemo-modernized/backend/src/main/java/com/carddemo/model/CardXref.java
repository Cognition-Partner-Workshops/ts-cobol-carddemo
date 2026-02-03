package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cardXrefs")
public class CardXref {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String cardNumber;
    
    @Indexed
    private String customerId;
    
    @Indexed
    private String accountId;
}
