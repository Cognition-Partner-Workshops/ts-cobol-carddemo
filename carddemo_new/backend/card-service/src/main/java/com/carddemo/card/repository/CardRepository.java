package com.carddemo.card.repository;

import com.carddemo.card.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {
    List<Card> findByAccountId(String accountId);
    List<Card> findByCustomerId(String customerId);
    Page<Card> findByAccountId(String accountId, Pageable pageable);
    Page<Card> findByActiveStatus(String activeStatus, Pageable pageable);
    boolean existsByCardNumber(String cardNumber);
}
