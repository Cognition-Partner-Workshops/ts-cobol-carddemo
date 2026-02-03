package com.carddemo.repository;

import com.carddemo.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    
    Optional<Transaction> findByTransactionId(String transactionId);
    
    List<Transaction> findByCardNumber(String cardNumber);
    
    Page<Transaction> findByCardNumber(String cardNumber, Pageable pageable);
    
    Page<Transaction> findAllByOrderByTransactionIdDesc(Pageable pageable);
    
    boolean existsByTransactionId(String transactionId);
    
    Optional<Transaction> findTopByOrderByTransactionIdDesc();
}
