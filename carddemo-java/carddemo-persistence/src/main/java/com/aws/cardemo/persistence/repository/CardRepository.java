package com.aws.cardemo.persistence.repository;

import com.aws.cardemo.domain.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Card entity persistence operations.
 * 
 * This repository provides data access methods for credit cards in the CardDemo application.
 * It extends JpaRepository to inherit standard CRUD operations and adds custom query methods
 * for card-specific business requirements.
 * 
 * The repository supports:
 * - Standard CRUD operations (inherited from JpaRepository)
 * - Card filtering by account and status
 * - Active card lookups for transaction validation
 * - Card search by embossed name
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    /**
     * Finds all cards associated with a specific account.
     * 
     * @param accountId The account identifier to filter cards by
     * @return List of cards linked to the specified account
     */
    List<Card> findByAccountId(String accountId);

    /**
     * Finds all cards with a specific active status.
     * 
     * @param status The card active status (Y=Active, N=Inactive)
     * @return List of cards matching the specified status
     */
    List<Card> findByCardActiveStatus(String status);

    /**
     * Finds only active cards for a specific account.
     * 
     * This query returns cards that are currently active (status = 'Y') for the
     * specified account. Useful for determining which cards can be used for transactions.
     * 
     * @param accountId The account identifier to filter active cards by
     * @return List of active cards for the specified account
     */
    @Query("SELECT c FROM Card c WHERE c.accountId = :accountId AND c.cardActiveStatus = 'Y'")
    List<Card> findActiveCardsByAccountId(@Param("accountId") String accountId);

    /**
     * Searches for cards by the embossed name on the card.
     * 
     * This query performs a partial match search on the embossed name field.
     * 
     * @param name The search term to match against embossed names
     * @return List of cards whose embossed name contains the search term
     */
    @Query("SELECT c FROM Card c WHERE c.cardEmbossedName LIKE %:name%")
    List<Card> findByEmbossedNameContaining(@Param("name") String name);
}
