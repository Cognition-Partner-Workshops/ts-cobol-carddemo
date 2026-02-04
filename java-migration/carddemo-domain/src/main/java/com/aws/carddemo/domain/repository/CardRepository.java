package com.aws.carddemo.domain.repository;

import com.aws.carddemo.domain.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findByAccountAccountId(Long accountId);

    Page<Card> findByAccountAccountId(Long accountId, Pageable pageable);

    List<Card> findByActiveStatus(String activeStatus);

    Page<Card> findByActiveStatus(String activeStatus, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.expirationDate <= :date AND c.activeStatus = 'Y'")
    List<Card> findExpiringCards(@Param("date") LocalDate date);

    @Query("SELECT c FROM Card c WHERE c.expirationDate < CURRENT_DATE AND c.activeStatus = 'Y'")
    List<Card> findExpiredActiveCards();

    @Query("SELECT c FROM Card c WHERE c.embossedName LIKE %:name%")
    Page<Card> findByEmbossedNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Card c WHERE c.activeStatus = 'Y'")
    long countActiveCards();

    @Query("SELECT COUNT(c) FROM Card c WHERE c.account.accountId = :accountId AND c.activeStatus = 'Y'")
    long countActiveCardsByAccount(@Param("accountId") Long accountId);

    boolean existsByCardNumber(String cardNumber);
}
