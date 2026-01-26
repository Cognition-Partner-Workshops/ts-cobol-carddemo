package com.aws.carddemo.repository;

import com.aws.carddemo.entity.Card;
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

    List<Card> findByAccountAcctId(Long acctId);

    List<Card> findByCardActiveStatus(String status);

    Page<Card> findByCardActiveStatus(String status, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.cardExpirationDate < :date AND c.cardActiveStatus = 'Y'")
    List<Card> findExpiredCards(@Param("date") LocalDate date);

    @Query("SELECT c FROM Card c WHERE c.cardExpirationDate BETWEEN :startDate AND :endDate")
    List<Card> findCardsExpiringBetween(@Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT c FROM Card c JOIN FETCH c.account WHERE c.cardNum = :cardNum")
    Optional<Card> findByCardNumWithAccount(@Param("cardNum") String cardNum);

    @Query("SELECT c FROM Card c JOIN FETCH c.transactions WHERE c.cardNum = :cardNum")
    Optional<Card> findByCardNumWithTransactions(@Param("cardNum") String cardNum);

    @Query("SELECT COUNT(c) FROM Card c WHERE c.cardActiveStatus = :status")
    long countByStatus(@Param("status") String status);

    boolean existsByCardNum(String cardNum);
}
