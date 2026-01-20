package com.carddemo.authorization.repository;

import com.carddemo.authorization.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {
    List<Card> findByAccountId(String accountId);
    List<Card> findByCustomerId(String customerId);
    List<Card> findByCardStatus(String cardStatus);
}
