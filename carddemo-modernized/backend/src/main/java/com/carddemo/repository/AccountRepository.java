package com.carddemo.repository;

import com.carddemo.model.Account;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends MongoRepository<Account, String> {
    
    Optional<Account> findByAccountId(String accountId);
    
    boolean existsByAccountId(String accountId);
    
    void deleteByAccountId(String accountId);
}
