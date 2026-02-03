package com.carddemo.repository;

import com.carddemo.model.Card;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends MongoRepository<Card, String> {
    
    Optional<Card> findByCardNumber(String cardNumber);
    
    List<Card> findByAccountId(String accountId);
    
    boolean existsByCardNumber(String cardNumber);
    
    void deleteByCardNumber(String cardNumber);
}
