package com.carddemo.card.repository;

import com.carddemo.common.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findByAccountId(Long accountId);

    List<Card> findByActiveStatus(String activeStatus);

    Page<Card> findByActiveStatus(String activeStatus, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.expirationDate < :date")
    List<Card> findExpiredCards(@Param("date") LocalDate date);

    @Query("SELECT c FROM Card c WHERE c.expirationDate BETWEEN :startDate AND :endDate")
    List<Card> findCardsExpiringBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT c FROM Card c WHERE c.cardNumber LIKE %:lastFourDigits")
    List<Card> findByLastFourDigits(@Param("lastFourDigits") String lastFourDigits);

    Optional<Card> findByCardNumberAndActiveStatus(String cardNumber, String activeStatus);

    @Query("SELECT COUNT(c) FROM Card c WHERE c.activeStatus = 'Y'")
    Long countActiveCards();

    @Query("SELECT COUNT(c) FROM Card c WHERE c.accountId = :accountId AND c.activeStatus = 'Y'")
    Long countActiveCardsByAccountId(@Param("accountId") Long accountId);
}
