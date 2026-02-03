package com.aws.cardemo.persistence.repository;

import com.aws.cardemo.domain.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findByAccountId(String accountId);

    List<Card> findByCardActiveStatus(String status);

    @Query("SELECT c FROM Card c WHERE c.accountId = :accountId AND c.cardActiveStatus = 'Y'")
    List<Card> findActiveCardsByAccountId(@Param("accountId") String accountId);

    @Query("SELECT c FROM Card c WHERE c.cardEmbossedName LIKE %:name%")
    List<Card> findByEmbossedNameContaining(@Param("name") String name);
}
