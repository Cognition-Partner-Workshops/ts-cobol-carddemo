package com.carddemo.repository;

import com.carddemo.model.CardXref;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardXrefRepository extends MongoRepository<CardXref, String> {
    
    Optional<CardXref> findByCardNumber(String cardNumber);
    
    List<CardXref> findByCustomerId(String customerId);
    
    List<CardXref> findByAccountId(String accountId);
    
    boolean existsByCardNumber(String cardNumber);
}
